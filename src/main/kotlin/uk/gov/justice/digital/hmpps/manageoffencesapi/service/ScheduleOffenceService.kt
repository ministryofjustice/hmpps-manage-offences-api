package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ImportCsvResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import java.io.BufferedReader

@Service
class ScheduleOffenceService(
  private val offenceRepository: OffenceRepository,
  private val schedulePartRepository: SchedulePartRepository,
  private val scheduleOffenceUpdateService: ScheduleOffenceUpdateService,
) {

  data class CsvLine(
    val code: String,
    val lineReference: String?,
    val legislationText: String?,
    val paragraphNumber: String?,
    val paragraphTitle: String?,
  )

  fun getSchedulePart(schedulePartId: Long): SchedulePart? = schedulePartRepository.findByIdOrNull(schedulePartId)

  fun import(reader: BufferedReader, schedulePart: SchedulePart): ImportCsvResult {
    val newMappings = parseCsv(reader).distinctBy { it.code }

    if (newMappings.isEmpty()) {
      return ImportCsvResult(success = false, message = "No offences found in the CSV", errors = listOf("Empty CSV"))
    }

    val offences = offenceRepository.findByCodeIn(newMappings.map { it.code.trim() })
    val mappings = newMappings.mapNotNull {
      val currentOffence = offences.find { offence -> offence.code == it.code }
      if (currentOffence == null) {
        log.warn("No offence found for code {}", it.code)
        return@mapNotNull null
      }

      OffenceScheduleMapping(
        schedulePart = schedulePart,
        offence = currentOffence,
        lineReference = it.lineReference,
        legislationText = it.legislationText,
        paragraphNumber = it.paragraphNumber,
        paragraphTitle = it.paragraphTitle,
      )
    }

    if (mappings.isEmpty()) {
      return ImportCsvResult(
        success = false,
        message = "No offences valid offences",
        errors = listOf("No offences codes found within the CSV match any actual offences"),
      )
    }

    return scheduleOffenceUpdateService.importSchedulePartOffences(mappings, schedulePart)
  }

  fun parseCsv(reader: BufferedReader): List<CsvLine> {
    reader.readLine() // ignore the headers

    return reader.lineSequence()
      .withIndex()
      .mapNotNull { (index, line) ->
        val values = line.split(",").map { it.trim() }

        if (values.size < csvHeaders.size) {
          log.info("Skipping line {} with incorrect number of {} columns", index + 2, values.size)
          return@mapNotNull null
        }

        val code = values[0]
        val lineReference = values[1].ifEmpty { null }
        val legislationText = values[2].ifEmpty { null }
        val paragraphNumber = values[3].ifEmpty { null }
        val paragraphTitle = values[4].ifEmpty { null }

        if (code.isEmpty()) {
          log.info("Skipping empty line {} with no code", index + 2)
          return@mapNotNull null
        }

        CsvLine(code, lineReference, legislationText, paragraphNumber, paragraphTitle)
      }.toList()
  }

  companion object {
    val csvHeaders = listOf(
      "code",
      "lineReference",
      "legislationText",
      "paragraphNumber",
      "paragraphTitle",
    )
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
