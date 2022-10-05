package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.NomisScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceSchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceToScheduleHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.DELETE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.INSERT
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceSchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceToScheduleHistoryRepository
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
  private val offenceToScheduleHistoryRepository: OffenceToScheduleHistoryRepository,
  private val nomisScheduleMappingRepository: NomisScheduleMappingRepository,
  private val adminService: AdminService,
  private val prisonApiClient: PrisonApiClient,
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
  fun linkOffences(schedulePartId: Long, offenceIds: Set<Long>) {
    val schedulePart = schedulePartRepository.findById(schedulePartId)
      .orElseThrow { EntityNotFoundException("No schedulePart exists for $schedulePartId") }

    val childOffences = offenceRepository.findByParentOffenceIdIn(offenceIds)
    val childOffenceIds = childOffences.map { it.id }.toSet()

    val parentOffences = offenceRepository.findAllById(offenceIds.minus(childOffenceIds))
      .filter { it.parentCode == null }

    val offences = parentOffences.plus(childOffences)

    val offenceScheduleParts = offenceSchedulePartRepository.saveAll(
      offences.map { offence ->
        OffenceSchedulePart(
          schedulePart = schedulePart,
          offence = offence
        )
      }
    )
    offenceToScheduleHistoryRepository.saveAll(offenceScheduleParts.map { transform(it, INSERT) })
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
      deleteSchedulePart(it.schedulePartId, it.offenceId)

      val childOffenceIds = offenceRepository.findByParentOffenceId(it.offenceId).map { child -> child.id }
      childOffenceIds.forEach { childOffenceId -> deleteSchedulePart(it.schedulePartId, childOffenceId) }
    }
  }

  private fun deleteSchedulePart(schedulePartId: Long, offenceId: Long) {
    saveToHistory(schedulePartId, offenceId, DELETE)
    offenceSchedulePartRepository.deleteBySchedulePartIdAndOffenceId(schedulePartId, offenceId)
  }

  private fun saveToHistory(schedulePartId: Long, offenceId: Long, changeType: ChangeType) {
    val osp = offenceSchedulePartRepository.findOneBySchedulePartIdAndOffenceId(schedulePartId, offenceId)
      .orElseThrow { EntityNotFoundException("No offenceSchedulePart exists for $schedulePartId and $offenceId") }
    offenceToScheduleHistoryRepository.save(transform(osp, changeType))
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

  fun deltaSyncScheduleMappingsToNomis() {
    if (!adminService.isFeatureEnabled(DELTA_SYNC_NOMIS)) return
    log.info("Starting job to synchronise offence-to-schedule mappings with NOMIS")
    val mappingsToPush = offenceToScheduleHistoryRepository.findByPushedToNomisOrderByCreatedDateDesc(false)
    if (mappingsToPush.isEmpty()) return

    val nomisScheduleMappings = nomisScheduleMappingRepository.findAll()
    val mappingsByScheduleAndOffence = mappingsToPush.groupBy { Pair(it.schedulePartId, it.offenceCode) }
    val mappingDtosToInsert = convertToNomisMapping(mappingsByScheduleAndOffence.values, nomisScheduleMappings, INSERT)
    val mappingDtosToDelete = convertToNomisMapping(mappingsByScheduleAndOffence.values, nomisScheduleMappings, DELETE)

    if (mappingDtosToInsert.isNotEmpty()) prisonApiClient.linkToSchedule(mappingDtosToInsert)
    if (mappingDtosToDelete.isNotEmpty()) prisonApiClient.unlinkFromSchedule(mappingDtosToDelete)

    offenceToScheduleHistoryRepository.saveAll(
      mappingsToPush.map { it.copy(pushedToNomis = true) }
    )
  }

  private fun convertToNomisMapping(
    mappingsByScheduleAndOffence: Collection<List<OffenceToScheduleHistory>>,
    nomisScheduleMappings: List<NomisScheduleMapping>,
    changeType: ChangeType
  ) = mappingsByScheduleAndOffence
    .filter { it.first().changeType == changeType }
    .map { mapping ->
      transform(mapping.first(), nomisScheduleMappings)
    }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
