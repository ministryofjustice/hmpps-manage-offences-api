package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceSchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceSchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule as ModelSchedule

@Service
class ScheduleService(
  private val scheduleRepository: ScheduleRepository,
  private val schedulePartRepository: SchedulePartRepository,
  private val offenceSchedulePartRepository: OffenceSchedulePartRepository,
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
            scheduleEntity
          )
        }
      )
    }
  }

  /*
    If an offence in @offenceIds has any children (inchoate offences) they are linked automatically
    Any offences in @offenceIds that are themselves children will be ignored
   */
  @Transactional
  fun linkOffences(schedulePartId: Long, offenceIds: List<Long>) {
    val schedulePart = schedulePartRepository.findById(schedulePartId)
      .orElseThrow { EntityNotFoundException("No schedulePart exists for $schedulePartId") }

    val childOffenceIds = offenceRepository.findByParentOffenceIdIn(offenceIds).map { it.id }

    val offences = offenceRepository
      .findAllById(offenceIds.plus(childOffenceIds).distinct())
      .filter { it.parentCode == null || childOffenceIds.contains(it.id) }

    offenceSchedulePartRepository.saveAll(
      offences.map { offence ->
        OffenceSchedulePart(
          schedulePart = schedulePart,
          offence = offence
        )
      }
    )
  }

  /*
    If an offence in @offenceSchedulePartIds has any children (inchoate offences) they are unlinked automatically
    Any offences in @offenceIds that are themselves children will be ignored
   */
  @Transactional
  fun unlinkOffences(offenceSchedulePartIds: List<SchedulePartIdAndOffenceId>) {

    val allOffenceIds = offenceSchedulePartIds.map { it.offenceId }
    val parentOffenceIds = offenceRepository.findAllById(allOffenceIds).filter { it.parentCode == null }.map { it.id }

    offenceSchedulePartIds.forEach {
      if (!parentOffenceIds.contains(it.offenceId)) return@forEach // ignore any children that have been directly passed in

      val childOffenceIds = offenceRepository.findByParentOffenceId(it.offenceId).map { child -> child.id }
      offenceSchedulePartRepository.deleteBySchedulePartIdAndOffenceId(
        it.schedulePartId,
        it.offenceId
      )
      childOffenceIds.forEach { childOffenceId ->
        offenceSchedulePartRepository.deleteBySchedulePartIdAndOffenceId(
          it.schedulePartId,
          childOffenceId
        )
      }
    }
  }

  @Transactional(readOnly = true)
  fun findScheduleById(scheduleId: Long): ModelSchedule {
    val schedule = scheduleRepository.findById(scheduleId)
      .orElseThrow { EntityNotFoundException("No schedule exists for $scheduleId") }

    val entityScheduleParts = schedulePartRepository.findByScheduleId(scheduleId)
    val offenceScheduleParts = offenceSchedulePartRepository.findBySchedulePartScheduleId(scheduleId)
    val offencesByParts = offenceScheduleParts.groupBy { it.schedulePart.id }

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

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
