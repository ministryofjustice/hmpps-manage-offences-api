package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceToScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule as ModelSchedule

@Service
class ScheduleService(
  private val scheduleRepository: ScheduleRepository,
  private val schedulePartRepository: SchedulePartRepository,
  private val offenceScheduleMappingRepository: OffenceScheduleMappingRepository,
  private val offenceRepository: OffenceRepository,
) {
  @Transactional
  fun createSchedule(schedule: ModelSchedule) {
    scheduleRepository.findOneByActAndCode(schedule.act, schedule.code)
      .ifPresent { throw EntityExistsException(it.id.toString()) }

    val scheduleEntity = scheduleRepository.save(transform(schedule))
    if (schedule.scheduleParts != null) {
      schedulePartRepository.saveAll(
        schedule.scheduleParts.map { schedulePart ->
          transform(
            schedulePart,
            scheduleEntity,
          )
        },
      )
    }
  }

  /*
    If the associated offence  has any children (inchoate offences) they are linked automatically
   */
  @Transactional
  fun linkOffences(linkOffence: LinkOffence) {
    val schedulePart = schedulePartRepository.findById(linkOffence.schedulePartId)
      .orElseThrow { EntityNotFoundException("No schedulePart exists for $linkOffence.schedulePartId") }

    val parentOffence = offenceRepository.findById(linkOffence.offenceId)
      .orElseThrow { EntityNotFoundException("No schedulePart exists for ${linkOffence.offenceId}") }

    val childOffences = offenceRepository.findByParentOffenceId(linkOffence.offenceId)
    val offences = childOffences.plus(parentOffence)

    offenceScheduleMappingRepository.saveAll(
      offences.map { offence ->
        transform(schedulePart, offence, linkOffence)
      },
    )
  }

  /*
    If the offence has any children (inchoate offences) they are unlinked automatically
   */
  @Transactional
  fun unlinkOffences(schedulePartIdAndOffenceIds: List<SchedulePartIdAndOffenceId>) {
    val allOffenceIds = schedulePartIdAndOffenceIds.map { it.offenceId }
    val parentOffenceIds = offenceRepository.findAllById(allOffenceIds).filter { it.parentCode == null }.map { it.id }

    schedulePartIdAndOffenceIds.forEach {
      if (!parentOffenceIds.contains(it.offenceId)) return@forEach // ignore any children that have been directly passed in
      deleteOffenceScheduleMapping(it.schedulePartId, it.offenceId)

      val childOffenceIds = offenceRepository.findByParentOffenceId(it.offenceId).map { child -> child.id }
      childOffenceIds.forEach { childOffenceId -> deleteOffenceScheduleMapping(it.schedulePartId, childOffenceId) }
    }
  }

  private fun deleteOffenceScheduleMapping(scheduleParagraphId: Long, offenceId: Long) =
    offenceScheduleMappingRepository.deleteBySchedulePartIdAndOffenceId(scheduleParagraphId, offenceId)

  @Transactional(readOnly = true)
  fun findScheduleById(scheduleId: Long): ModelSchedule {
    val schedule = scheduleRepository.findById(scheduleId)
      .orElseThrow { EntityNotFoundException("No schedule exists for $scheduleId") }

    val entityScheduleParts = schedulePartRepository.findByScheduleId(scheduleId)
    val offenceScheduleMappings = offenceScheduleMappingRepository.findBySchedulePartScheduleId(scheduleId)
    val offencesByParts = offenceScheduleMappings.groupBy { it.schedulePart.id }

    val scheduleParts = entityScheduleParts.map {
      transform(it, offencesByParts)
    }

    return transform(schedule, scheduleParts)
  }

  @Transactional(readOnly = true)
  fun findAllSchedules(): List<ModelSchedule> {
    val schedules = scheduleRepository.findAll()

    return schedules.map {
      transform(it)
    }
  }

  fun findOffenceById(offenceId: Long): OffenceToScheduleMapping {
    val offence = offenceRepository.findById(offenceId)
      .orElseThrow { EntityNotFoundException("Offence not found with ID $offenceId") }
    val children = offenceRepository.findByParentOffenceId(offenceId)
    return transform(offence, children)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
