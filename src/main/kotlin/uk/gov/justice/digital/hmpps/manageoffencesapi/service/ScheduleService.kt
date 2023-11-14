package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisScheduleName
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisSyncType
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffencePcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceToScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.OffenceToScheduleMappingDto
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceToSyncWithNomisRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule as ModelSchedule

@Service
class ScheduleService(
  private val scheduleRepository: ScheduleRepository,
  private val schedulePartRepository: SchedulePartRepository,
  private val offenceScheduleMappingRepository: OffenceScheduleMappingRepository,
  private val offenceRepository: OffenceRepository,
  private val prisonApiUserClient: PrisonApiUserClient,
  private val nomisScheduleMappingRepository: NomisScheduleMappingRepository,
  private val offenceToSyncWithNomisRepository: OffenceToSyncWithNomisRepository,
  private val adminService: AdminService,
) {

  //  Only used for migration purposes when the data is changed outside of the UI
  @Scheduled(cron = "0 0 */1 * * *")
  @SchedulerLock(name = "unlinkScheduleMappingsToNomis")
  @Transactional
  fun unlinkScheduleMappingsToNomis() {
    if (!adminService.isFeatureEnabled(Feature.UNLINK_SCHEDULES_NOMIS)) {
      log.info("Unlink schedules with NOMIS - disabled")
      return
    }
    log.info("Unlink schedules with NOMIS used for migration records - starting")

    val schedulesToUnlink = offenceToSyncWithNomisRepository.findByNomisSyncType(NomisSyncType.UNLINK_SCHEDULE_TO_OFFENCE_MAPPING)

    prisonApiUserClient.unlinkFromSchedule(
      schedulesToUnlink.map { s ->
        OffenceToScheduleMappingDto(
          schedule = s.nomisScheduleName?.name!!,
          offenceCode = s.offenceCode,
        )
      },
    )
    log.info("Unlink schedules with NOMIS finished")
  }

  //  Only used for migration purposes when the data is changed outside of the UI
  @Scheduled(cron = "0 0 */1 * * *")
  @SchedulerLock(name = "linkScheduleMappingsToNomis")
  @Transactional
  fun linkScheduleMappingsToNomis() {
    if (!adminService.isFeatureEnabled(Feature.LINK_SCHEDULES_NOMIS)) {
      log.info("Link schedules with NOMIS - disabled")
      return
    }
    log.info("Link schedules with NOMIS used for migration records - starting")

    val (pcscSchedulesToLink, schedulesToLink) = offenceToSyncWithNomisRepository.findByNomisSyncType(NomisSyncType.LINK_SCHEDULE_TO_OFFENCE_MAPPING).partition { it.nomisScheduleName == NomisScheduleName.POTENTIAL_LINK_PCSC }

    prisonApiUserClient.linkToSchedule(
      schedulesToLink.map { s ->
        OffenceToScheduleMappingDto(
          schedule = s.nomisScheduleName?.name!!,
          offenceCode = s.offenceCode,
        )
      },
    )

    if (pcscSchedulesToLink.isNotEmpty()) {
      val pcscMappings = determinePcscMappingsForNomis(pcscSchedulesToLink.map { o -> o.offenceCode })
      prisonApiUserClient.linkToSchedule(pcscMappings)
    }
    log.info("Link schedules with NOMIS finished")
  }

  @Transactional
  fun createSchedule(schedule: ModelSchedule) {
    scheduleRepository.findOneByActAndCode(schedule.act, schedule.code)
      ?: throw EntityExistsException(schedule.id.toString())

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

    if (schedulePart.schedule.code == "15" && (schedulePart.partNumber == 1 || schedulePart.partNumber == 2)) {
      val pcscMappings = determinePcscMappingsForNomis(offences.map { it.code })
      prisonApiUserClient.linkToSchedule(pcscMappings)
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

    schedulePartIdAndOffenceIds.forEach {
      if (!parentOffenceIds.contains(it.offenceId)) return@forEach // ignore any children that have been directly passed in
      val parentOffence = parentOffences.first { po -> po.id == it.offenceId }
      deleteOffenceScheduleMapping(it.schedulePartId, it.offenceId)

      val childOffences = offenceRepository.findByParentOffenceId(it.offenceId)
      val childOffenceIds = childOffences.map { child -> child.id }
      childOffenceIds.forEach { childOffenceId -> deleteOffenceScheduleMapping(it.schedulePartId, childOffenceId) }

      val offences = childOffences.plus(parentOffence)

      nomisScheduleMappingRepository.findOneBySchedulePartId(it.schedulePartId)?.let {
        val req = offences.map { offenceToUnlink ->
          OffenceToScheduleMappingDto(
            schedule = it.nomisScheduleName,
            offenceCode = offenceToUnlink.code,
          )
        }
        prisonApiUserClient.unlinkFromSchedule(
          offences.map { offenceToUnlink ->
            OffenceToScheduleMappingDto(
              schedule = it.nomisScheduleName,
              offenceCode = offenceToUnlink.code,
            )
          },
        )
      }

      val schedulePart = schedulePartRepository.findById(it.schedulePartId)
        .orElseThrow { EntityNotFoundException("No schedulePart exists for ${it.schedulePartId}") }

      if (schedulePart.schedule.code == "15" && (schedulePart.partNumber == 1 || schedulePart.partNumber == 2)) {
        val pcscMappings = determinePcscMappingsForNomis(offences.map { o -> o.code })
        prisonApiUserClient.unlinkFromSchedule(pcscMappings)
      }
    }
  }

  private fun determinePcscMappingsForNomis(offences: List<String>): List<OffenceToScheduleMappingDto> {
    val pcscSchedules = getOffencePcscMarkers(offences)
    val pcscMappings = mutableListOf<OffenceToScheduleMappingDto>()
    pcscSchedules.forEach { pcscSchedule ->
      if (pcscSchedule.pcscMarkers.inListA) {
        pcscMappings.add(
          OffenceToScheduleMappingDto(
            schedule = NomisScheduleName.SCHEDULE_15_ATTRACTS_LIFE.name,
            offenceCode = pcscSchedule.offenceCode,
          ),
        )
      }

      if (pcscSchedule.pcscMarkers.inListB) {
        pcscMappings.add(
          OffenceToScheduleMappingDto(
            schedule = NomisScheduleName.PCSC_SDS.name,
            offenceCode = pcscSchedule.offenceCode,
          ),
        )
      }

      if (pcscSchedule.pcscMarkers.inListC) {
        pcscMappings.add(
          OffenceToScheduleMappingDto(
            schedule = NomisScheduleName.PCSC_SEC_250.name,
            offenceCode = pcscSchedule.offenceCode,
          ),
        )
      }

      if (pcscSchedule.pcscMarkers.inListD) {
        pcscMappings.add(
          OffenceToScheduleMappingDto(
            schedule = NomisScheduleName.PCSC_SDS_PLUS.name,
            offenceCode = pcscSchedule.offenceCode,
          ),
        )
      }
    }
    return pcscMappings
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

  @Transactional(readOnly = true)
  fun findPcscSchedules(offenceCodes: List<String>): List<OffencePcscMarkers> {
    log.info("Determining PCSC schedules for passed in offences")
    return getOffencePcscMarkers(offenceCodes)
  }

  private fun getOffencePcscMarkers(offenceCodes: List<String>): List<OffencePcscMarkers> {
    val (part1LifeMappings, part2LifeMappings, seriousViolentOffenceMappings) = getSchedule15PcscMappings()

    return offenceCodes.map {
      OffencePcscMarkers(
        offenceCode = it,
        PcscMarkers(
          inListA = inListA(part1LifeMappings, part2LifeMappings, it),
          inListB = inListB(seriousViolentOffenceMappings, part2LifeMappings, it),
          inListC = inListC(seriousViolentOffenceMappings, part2LifeMappings, it),
          inListD = inListD(part1LifeMappings, part2LifeMappings, it),
        ),
      )
    }
  }

  private fun getSchedule15PcscMappings(): Triple<List<OffenceScheduleMapping>, List<OffenceScheduleMapping>, List<OffenceScheduleMapping>> {
    val schedule15 = scheduleRepository.findOneByActAndCode("Criminal Justice Act 2003", "15")
      ?: throw EntityNotFoundException("Schedule 15 not found")
    val parts = schedulePartRepository.findByScheduleId(schedule15.id)
    val part1Mappings = offenceScheduleMappingRepository.findBySchedulePartId(parts.first { p -> p.partNumber == 1 }.id)
    val part1LifeMappings = part1Mappings.filter { it.offence.maxPeriodIsLife == true }
    val part2Mappings = offenceScheduleMappingRepository.findBySchedulePartId(parts.first { p -> p.partNumber == 2 }.id)
    val part2LifeMappings = part2Mappings.filter { it.offence.maxPeriodIsLife == true }
    val seriousViolentOffenceMappings =
      part1Mappings.filter { it.paragraphNumber == "1" || it.paragraphNumber == "4" || it.paragraphNumber == "6" || it.paragraphNumber == "64" || it.paragraphNumber == "65" }
    return Triple(part1LifeMappings, part2LifeMappings, seriousViolentOffenceMappings)
  }

  // List A: Schedule 15 Part 1 + Schedule 15 Part 2 that attract life (exclude all offences that start after 28 June 2022)
  // NOMIS SCHEDULE_15_ATTRACTS_LIFE - SDS >7 years between 01 April 2020 and 28 June 2022
  private fun inListA(
    part1LifeMappings: List<OffenceScheduleMapping>,
    part2LifeMappings: List<OffenceScheduleMapping>,
    offenceCode: String,
  ): Boolean =
    part1LifeMappings.any { p -> offenceCode == p.offence.code && p.offence.startDate < SDS_LIST_A_CUT_OFF_DATE } ||
      part2LifeMappings.any { p ->
        offenceCode == p.offence.code && p.offence.startDate < SDS_LIST_A_CUT_OFF_DATE
      }

  // List B: Schedule 15 Part 2 that attract life + serious violent offences (same as List C)
  // NOMIS - PCSC_SDS - SDS between 4 and 7 years
  private fun inListB(
    seriousViolentOffenceMappings: List<OffenceScheduleMapping>,
    part2LifeMappings: List<OffenceScheduleMapping>,
    offenceCode: String,
  ) =
    seriousViolentOffenceMappings.any { p -> offenceCode == p.offence.code } || part2LifeMappings.any { p -> offenceCode == p.offence.code }

  // List C: Schedule 15 Part 2 that attract life + serious violent offences (same as List B)
  // NOMIS - PCSC_SEC_250 - Sec250 >7 years
  private fun inListC(
    seriousViolentOffenceMappings: List<OffenceScheduleMapping>,
    part2LifeMappings: List<OffenceScheduleMapping>,
    offenceCode: String,
  ) =
    seriousViolentOffenceMappings.any { p -> offenceCode == p.offence.code } || part2LifeMappings.any { p -> offenceCode == p.offence.code }

  // List D: Schedule 15 Part 1 + Schedule 15 Part 2 that attract life
  // NOMIS - PCSC_SDS_PLUS
  private fun inListD(
    part1LifeMappings: List<OffenceScheduleMapping>,
    part2LifeMappings: List<OffenceScheduleMapping>,
    offenceCode: String,
  ) =
    part1LifeMappings.any { p -> offenceCode == p.offence.code } || part2LifeMappings.any { p -> offenceCode == p.offence.code }

  fun findOffenceById(offenceId: Long): OffenceToScheduleMapping {
    val offence = offenceRepository.findById(offenceId)
      .orElseThrow { EntityNotFoundException("Offence not found with ID $offenceId") }
    val children = offenceRepository.findByParentOffenceId(offenceId)
    return transform(offence, children)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    val SDS_LIST_A_CUT_OFF_DATE = LocalDate.of(2022, 6, 28)
  }
}
