package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ImportCsvResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository

@Service
class ScheduleOffenceUpdateService(
  private val offenceScheduleMappingRepository: OffenceScheduleMappingRepository,
) {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun importSchedulePartOffences(scheduleMappings: List<OffenceScheduleMapping>, schedulePart: SchedulePart): ImportCsvResult = try {
    offenceScheduleMappingRepository.saveAll(scheduleMappings)
    ImportCsvResult(
      success = true,
      message = "Imported ${scheduleMappings.size} offences to Schedule ${schedulePart.schedule.act} part ${schedulePart.partNumber}",
      errors = emptyList(),
    )
  } catch (e: Exception) {
    ImportCsvResult(
      success = false,
      message = "Failed to import schedule offences: ${e.localizedMessage}",
      errors = listOf("Database error"),
    )
  }
}
