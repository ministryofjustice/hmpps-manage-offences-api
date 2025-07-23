package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.CustodialIndicator
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ImportCsvResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import java.io.BufferedReader
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OffenceImportService(
  private val offenceRepository: OffenceRepository,
  private val offenceScheduleMappingRepository: OffenceScheduleMappingRepository,
  private val schedulePartRepository: SchedulePartRepository,
) {

  fun validateSchedulePartExists(schedulePartId: Long): Boolean = schedulePartRepository.findById(schedulePartId) != null

  fun validateCsv(reader: BufferedReader): ImportCsvResult? {
    val headerLine = reader.readLine()
      ?: return ImportCsvResult(
        success = false,
        message = "Invalid CSV",
        errors = listOf("CSV file is missing header row."),
      )

    val actualHeaders = headerLine.split(",").map { it.trim() }
    val missingHeaders = csvHeaders.filter { it !in actualHeaders }

    if (missingHeaders.isNotEmpty()) {
      return ImportCsvResult(
        success = false,
        message = "Invalid CSV",
        errors = listOf("CSV file is missing the following headers: ${missingHeaders.joinToString(", ")}"),
      )
    }

    val errors = mutableListOf<String>()
    val newOffenceCodes = mutableListOf<String>()

    reader.lineSequence()
      .withIndex()
      .take(5001)
      .forEach { (index, line) ->
        val lineNumber = index + 2

        if (lineNumber > 5001) {
          return ImportCsvResult(
            success = false,
            message = "Invalid CSV",
            errors = listOf("Maximum of 5000 offences can be imported at one time."),
          )
        }

        if (line.isBlank()) return@forEach

        val values = line.split(",")
        if (values.size != csvHeaders.size) {
          errors.add("Line $lineNumber: Incorrect number of columns (expected ${csvHeaders.size}, found ${values.size})")
          return@forEach
        }

        val lineErrors = values.mapIndexedNotNull { columnIndex, value ->
          val header = csvHeaders[columnIndex]
          validateValue(lineNumber, header, value.trim())
        }

        if (lineErrors.isNotEmpty()) {
          errors += lineErrors
        }

        if (newOffenceCodes.contains(values[0])) {
          errors.add("Line $lineNumber offence code ${values[0]} has been specified more than once.")
        } else {
          newOffenceCodes.add(values[0])
        }
      }

    val existingOffences =
      offenceRepository.returnCodesThatExist(newOffenceCodes).map { "offence code $it already exists in the database" }

    if (existingOffences.isNotEmpty()) {
      errors += existingOffences
    }

    return if (errors.isNotEmpty()) {
      ImportCsvResult(success = false, message = "Invalid offence data", errors)
    } else {
      null
    }
  }

  @Transactional
  fun persist(reader: BufferedReader, schedulePartId: Long? = null): ImportCsvResult {
    val offencesToSave = parseCsv(reader)

    if (offencesToSave.isEmpty()) {
      return ImportCsvResult(success = false, message = "No offences found in the CSV", errors = listOf("Empty CSV"))
    }

    return try {
      offenceRepository.saveAll(offencesToSave)

      schedulePartId?.let { id ->
        val schedulePart = schedulePartRepository.findById(id)
          .orElseThrow { IllegalArgumentException("Schedule Part with ID $id not found") }

        val mappings = offencesToSave.map { offence ->
          OffenceScheduleMapping(schedulePart = schedulePart, offence = offence)
        }

        offenceScheduleMappingRepository.saveAll(mappings)
      }

      ImportCsvResult(
        success = true,
        message = "Imported ${offencesToSave.size} offences",
        errors = emptyList(),
      )
    } catch (e: Exception) {
      ImportCsvResult(
        success = false,
        message = "Failed to import offences: ${e.localizedMessage}",
        errors = listOf("Database error"),
      )
    }
  }

  fun parseCsv(reader: BufferedReader): List<Offence> {
    reader.readLine() // ignore the headers

    return reader.lineSequence()
      .withIndex()
      .mapNotNull { (index, line) ->
        val values = line.split(",").map { it.trim() }

        if (values.size < csvHeaders.size) {
          println("Skipping line ${index + 2}: Not enough columns")
          return@mapNotNull null
        }

        val code = values[0]
        val srdsString = "OFFENCES_${code.firstOrNull() ?: return@mapNotNull null}"

        val custodialIndicator =
          if (listOf("YES", "NO", "EITHER").contains(values.last())) {
            CustodialIndicator.valueOf(values.last())
          } else {
            null
          }

        try {
          Offence(
            code = values[0],
            description = values[1],
            cjsTitle = values[1],
            offenceType = values[2].takeUnless { it.isBlank() },
            revisionId = values[3].toInt(),
            startDate = LocalDate.parse(values[4]),
            endDate = values[5].takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
            changedDate = LocalDateTime.now(),
            legislation = values[6].takeUnless { it.isBlank() },
            maxPeriodIsLife = values[7].toBooleanStrictOrNull() ?: false,
            maxPeriodOfIndictmentYears = values[8].toIntOrNull(),
            maxPeriodOfIndictmentMonths = values[9].toIntOrNull(),
            maxPeriodOfIndictmentWeeks = values[10].toIntOrNull(),
            maxPeriodOfIndictmentDays = values[11].toIntOrNull(),
            custodialIndicator = custodialIndicator,
            sdrsCache = SdrsCache.valueOf(srdsString),
          )
        } catch (e: Exception) {
          println("Skipping line ${index + 2}: ${e.message}")
          null
        }
      }
      .toList()
  }

  fun validateValue(lineNumber: Int, header: String, value: String): String? {
    if (csvMandatoryFields.contains(header) && value.isBlank()) {
      return "Line $lineNumber [$header] must be provided"
    }

    if (value.isBlank()) return null

    return when (header) {
      "revisionId",
      "maxPeriodOfIndictmentYears",
      "maxPeriodOfIndictmentMonths",
      "maxPeriodOfIndictmentWeeks",
      "maxPeriodOfIndictmentDays",
      -> if (runCatching { value.toInt() }.isSuccess) null else "Line $lineNumber [$header] Expected integer"

      "startDate", "endDate" ->
        if (runCatching { LocalDate.parse(value) }.isSuccess) null else "Line $lineNumber [$header] Expected date (yyyy-MM-dd)"

      "changedDate", "loadDate" ->
        if (runCatching { LocalDateTime.parse(value) }.isSuccess) null else "Line $lineNumber [$header] Expected datetime (yyyy-MM-ddTHH:mm:ss)"

      "isChild", "maxPeriodIsLife" ->
        if (value.lowercase() in listOf(
            "true",
            "false",
          )
        ) {
          null
        } else {
          "Line $lineNumber [$header] Expected boolean (true/false)"
        }

      "custodialIndicator" ->
        if (value in listOf(
            "YES",
            "NO",
            "EITHER",
          )
        ) {
          null
        } else {
          "Line $lineNumber [$header] Expected one of: YES, NO, EITHER"
        }

      else -> null
    }
  }

  companion object {
    val csvHeaders = listOf(
      "code",
      "description",
      "offenceType",
      "revisionId",
      "startDate",
      "endDate",
      "legislation",
      "maxPeriodIsLife",
      "maxPeriodOfIndictmentYears",
      "maxPeriodOfIndictmentMonths",
      "maxPeriodOfIndictmentWeeks",
      "maxPeriodOfIndictmentDays",
      "custodialIndicator",
    )
    val csvMandatoryFields = listOf("code", "description", "revisionId")
  }
}
