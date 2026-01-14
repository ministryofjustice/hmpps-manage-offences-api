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
      return ImportCsvResult(success = false, message = "No valid offences found within CSV", errors = listOf("Invalid CSV"))
    }

    val newOffencesCodes = newMappings.map { it.code }

    val currentScheduleOffences = offenceRepository.findOffenceCodesBySchedulePart(schedulePart)
    val newParentOffences = offenceRepository.findRootOffencesByCodeIn(newOffencesCodes)
    val newChildOffences = offenceRepository.findChildOffences(newParentOffences.map { it.id })

    val newScheduleOffences = offenceRepository
      .findRootOffencesByCodeIn(newMappings.map { it.code })
      .filter { it.code !in currentScheduleOffences }

    val parentMappings = newMappings.mapNotNull {
      val currentOffence = newScheduleOffences.find { offence -> offence.code == it.code } ?: return@mapNotNull null
      OffenceScheduleMapping(
        schedulePart = schedulePart,
        offence = currentOffence,
        lineReference = it.lineReference,
        legislationText = it.legislationText,
        paragraphNumber = it.paragraphNumber,
        paragraphTitle = it.paragraphTitle,
      )
    }

    val childMappings = parentMappings.flatMap { parent ->
      newChildOffences
        .filter { it.parentOffenceId == parent.offence.id }
        .map { child ->
          OffenceScheduleMapping(
            schedulePart = schedulePart,
            offence = child,
            lineReference = parent.lineReference,
            legislationText = parent.legislationText,
            paragraphNumber = parent.paragraphNumber,
            paragraphTitle = parent.paragraphTitle,
          )
        }
    }

    if (parentMappings.isEmpty()) {
      return ImportCsvResult(
        success = false,
        message = "No valid offences",
        errors = listOf("No offences codes found or are already included within the schedule"),
      )
    }

    val parentChildMappings = parentMappings.plus(childMappings)

    return scheduleOffenceUpdateService.importSchedulePartOffences(parentChildMappings, schedulePart)
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

        val code = values[0].trim()
        val lineReference = values[1].trim().take(256).ifEmpty { null }
        val legislationText = values[2].trim().take(1024).ifEmpty { null }
        val paragraphNumber = values[3].trim().take(20).ifEmpty { null }
        val paragraphTitle = values[4].trim().take(256).ifEmpty { null }

        if (code.isEmpty()) {
          log.info("Skipping empty line {} with no code", index + 2)
          return@mapNotNull null
        } else if (code.length > 7) {
          log.info("Skipping empty line {} with code greater than 7 characters", index + 2)
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
