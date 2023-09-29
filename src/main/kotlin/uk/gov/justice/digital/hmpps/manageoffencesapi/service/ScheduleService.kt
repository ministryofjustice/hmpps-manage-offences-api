package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceToScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.OffenceToScheduleMappingDto
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule as ModelSchedule

@Service
class ScheduleService(
  private val scheduleRepository: ScheduleRepository,
  private val schedulePartRepository: SchedulePartRepository,
  private val offenceScheduleMappingRepository: OffenceScheduleMappingRepository,
  private val offenceRepository: OffenceRepository,
  private val prisonApiUserClient: PrisonApiUserClient,
  private val nomisScheduleMappingRepository: NomisScheduleMappingRepository,
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

    nomisScheduleMappingRepository.findOneBySchedulePartId(linkOffence.schedulePartId)?.let { nomisScheduleMapping ->
      prisonApiUserClient.linkToSchedule(
        offences.map {
          OffenceToScheduleMappingDto(
            schedule = nomisScheduleMapping.nomisScheduleName,
            offenceCode = it.code,
          )
        },
      )
    }
  }

  /*
    If the offence has any children (inchoate offences) they are unlinked automatically
   */
  @Transactional
  fun unlinkOffences(schedulePartIdAndOffenceIds: List<SchedulePartIdAndOffenceId>) {
    val allOffenceIds = schedulePartIdAndOffenceIds.map { it.offenceId }
    val parentOffences = offenceRepository.findAllById(allOffenceIds).filter { it.parentCode == null }
    val parentOffenceIds = parentOffences.map { it.id }

    println("###################### ${schedulePartIdAndOffenceIds.size}")
    println("###################### ${schedulePartIdAndOffenceIds.size}")
    println("###################### ${schedulePartIdAndOffenceIds.size}")
    println("###################### ${schedulePartIdAndOffenceIds.size}")
    println("###################### ${schedulePartIdAndOffenceIds.size}")
    schedulePartIdAndOffenceIds.forEach {
      if (!parentOffenceIds.contains(it.offenceId)) return@forEach // ignore any children that have been directly passed in
      println("Deleteing ${it.schedulePartId} ${it.offenceId}")
      deleteOffenceScheduleMapping(it.schedulePartId, it.offenceId)

      val childOffences = offenceRepository.findByParentOffenceId(it.offenceId)
      val childOffenceIds = childOffences.map { child -> child.id }
      childOffenceIds.forEach { childOffenceId -> deleteOffenceScheduleMapping(it.schedulePartId, childOffenceId) }

      nomisScheduleMappingRepository.findOneBySchedulePartId(it.schedulePartId)?.let {
        prisonApiUserClient.unlinkFromSchedule(
          parentOffences.plus(childOffences).map { offenceToUnlink ->
            OffenceToScheduleMappingDto(
              schedule = it.nomisScheduleName,
              offenceCode = offenceToUnlink.code,
            )
          },
        )
      }
    }
  }

  private fun deleteOffenceScheduleMapping(schedulePartId: Long, offenceId: Long) =
    offenceScheduleMappingRepository.deleteBySchedulePartIdAndOffenceId(schedulePartId, offenceId)

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
