package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.LINK_SCHEDULES_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.UNLINK_SCHEDULES_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisScheduleName
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisSyncType
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffencePcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusion
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusion.Companion.getSdsExclusionIndicator
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceToScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PcscLists
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ScheduleInfo
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SdsExclusionLists
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
  private val prisonApiClient: PrisonApiClient,
  private val nomisScheduleMappingRepository: NomisScheduleMappingRepository,
  private val offenceToSyncWithNomisRepository: OffenceToSyncWithNomisRepository,
  private val adminService: AdminService,
) {

  //  Only used for migration purposes when the data is changed outside the UI
  @Scheduled(cron = "0 */10 * * * *")
  @SchedulerLock(name = "unlinkScheduleMappingsToNomis")
  @Transactional
  fun unlinkScheduleMappingsToNomis() {
    if (!adminService.isFeatureEnabled(UNLINK_SCHEDULES_NOMIS)) {
      log.info("Unlink schedules with NOMIS - disabled")
      return
    }
    log.info("Unlink schedules with NOMIS used for migration records - starting")

    val schedulesToUnlink =
      offenceToSyncWithNomisRepository.findByNomisSyncType(NomisSyncType.UNLINK_SCHEDULE_FROM_OFFENCE)

    prisonApiClient.unlinkFromSchedule(
      schedulesToUnlink.map { s ->
        OffenceToScheduleMappingDto(
          schedule = s.nomisScheduleName?.name!!,
          offenceCode = s.offenceCode,
        )
      },
    )
    offenceToSyncWithNomisRepository.deleteAllById(schedulesToUnlink.map { it.id })
    log.info("Unlink schedules with NOMIS finished")
  }

  //  Only used for migration purposes when the data is changed outside the UI
  @Scheduled(cron = "0 5-55/10 * * * *")
  @SchedulerLock(name = "linkScheduleMappingsToNomis")
  @Transactional
  fun linkScheduleMappingsToNomis() {
    if (!adminService.isFeatureEnabled(LINK_SCHEDULES_NOMIS)) {
      log.info("Link schedules with NOMIS - disabled")
      return
    }
    log.info("Link schedules with NOMIS used for migration records - starting")

    val (pcscSchedulesToLink, schedulesToLink) = offenceToSyncWithNomisRepository.findByNomisSyncType(NomisSyncType.LINK_SCHEDULE_TO_OFFENCE)
      .partition { it.nomisScheduleName == NomisScheduleName.POTENTIAL_LINK_PCSC }

    prisonApiClient.linkToSchedule(
      schedulesToLink.map { s ->
        OffenceToScheduleMappingDto(
          schedule = s.nomisScheduleName?.name!!,
          offenceCode = s.offenceCode,
        )
      },
    )

    if (pcscSchedulesToLink.isNotEmpty()) {
      val pcscMappings = determinePcscMappingsForNomis(pcscSchedulesToLink.map { o -> o.offenceCode })
      prisonApiClient.linkToSchedule(pcscMappings)
    }
    offenceToSyncWithNomisRepository.deleteAllById(pcscSchedulesToLink.plus(schedulesToLink).map { it.id })
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

      val childOffences = offenceRepository.findByParentOffenceId(it.offenceId)
      val offences = childOffences.plus(parentOffence)

      val schedulePart = schedulePartRepository.findById(it.schedulePartId)
        .orElseThrow { EntityNotFoundException("No schedulePart exists for ${it.schedulePartId}") }

      var pcscMappings = emptyList<OffenceToScheduleMappingDto>()
      if (schedulePart.schedule.code == "15" && (schedulePart.partNumber == 1 || schedulePart.partNumber == 2)) {
        pcscMappings = determinePcscMappingsForNomis(offences.map { o -> o.code })
      }
      deleteOffenceScheduleMapping(it.schedulePartId, it.offenceId)

      val childOffenceIds = childOffences.map { child -> child.id }
      childOffenceIds.forEach { childOffenceId -> deleteOffenceScheduleMapping(it.schedulePartId, childOffenceId) }

      nomisScheduleMappingRepository.findOneBySchedulePartId(it.schedulePartId)?.let {
        prisonApiUserClient.unlinkFromSchedule(
          offences.map { offenceToUnlink ->
            OffenceToScheduleMappingDto(
              schedule = it.nomisScheduleName,
              offenceCode = offenceToUnlink.code,
            )
          },
        )
      }

      if (pcscMappings.isNotEmpty()) {
        prisonApiUserClient.unlinkFromSchedule(pcscMappings)
      }
    }
  }

  private fun determinePcscMappingsForNomis(offenceCodes: List<String>): List<OffenceToScheduleMappingDto> {
    val pcscSchedules = getOffencePcscMarkers(offenceCodes)
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
  fun findPcscMarkers(offenceCodes: List<String>): List<OffencePcscMarkers> {
    log.info("Determining PCSC schedules for passed in offences")
    return getOffencePcscMarkers(offenceCodes)
  }

  @Transactional(readOnly = true)
  fun categoriseSdsExclusionsOffences(offenceCodes: List<String>): List<OffenceSdsExclusion> {
    log.info("Determining Sexual or Violent status for passed in offences")
    return getSdsExclusionIndicators(offenceCodes)
  }

  private fun getSdsExclusionIndicators(offenceCodes: List<String>): List<OffenceSdsExclusion> {
    val (part1Mappings, part2Mappings) = getSchedule15Mappings()
    val domesticViolenceMappings = getDomesticViolenceScheduleMappings()
    val securityOffencesFromLegislation = getSecurityOffencesLegislation()
    val sexualOffencesFromLegislation = getSexOffencesLegislation()
    val terrorismMapping = getTerrorismScheduleMappings()
    val sexScheduleMappings = getSexScheduleMappings()

    return offenceCodes.map { offenceCode ->
      val isSexual = part2Mappings.any { it.offence.code == offenceCode } ||
        hasSexualCodePrefix(offenceCode) ||
        sexualOffencesFromLegislation.any { it.code == offenceCode } ||
        sexScheduleMappings.any { it.offence.code == offenceCode }
      val isDomesticViolence = domesticViolenceMappings.any { it.offence.code == offenceCode }
      val isNationalSecurity = securityOffencesFromLegislation.any { it.code == offenceCode }
      val isViolent = part1Mappings.any { it.offence.code == offenceCode }
      val isTerrorism = terrorismMapping.any { it.offence.code == offenceCode }

      val indicator = getSdsExclusionIndicator(isSexual, isDomesticViolence, isNationalSecurity, isTerrorism, isViolent)

      OffenceSdsExclusion(
        offenceCode = offenceCode,
        schedulePart = indicator,
      )
    }
  }

  private fun getSexScheduleMappings() =
    offenceScheduleMappingRepository.findBySchedulePartScheduleActAndSchedulePartScheduleCode(
      SEXUAL_EXLUDED_OFFENCES_SCHEDULE.act,
      SEXUAL_EXLUDED_OFFENCES_SCHEDULE.code,
    )

  fun getSdsExclusionLists(): SdsExclusionLists {
    val (part1Mappings, part2Mappings) = getSchedule15Mappings()
    val domesticViolenceMappings = getDomesticViolenceScheduleMappings()
    val securityOffencesFromLegislation = getSecurityOffencesLegislation()
    val sexualOffencesFromLegislation = getSexOffencesLegislation()
    val sexOffencesByPrefix = offenceRepository.findByCodeStartsWithAnyIgnoreCase(
      SEXUAL_CODES_FOR_EXCLUSION_LIST[0],
      SEXUAL_CODES_FOR_EXCLUSION_LIST[1],
    )
    val terrorismMappings = getTerrorismScheduleMappings()
    val sexScheduleMappings = getSexScheduleMappings()

    val allSexualOffence = part2Mappings.asSequence().map { transform(it) }
      .plus(sexualOffencesFromLegislation.map { transform(it, emptyList<Offence>()) })
      .plus(sexOffencesByPrefix.map { transform(it, emptyList<Offence>()) })
      .plus(sexScheduleMappings.map { transform(it) })
      .distinctBy { it.code }
      .sortedBy { it.code }
      .toSet()

    return SdsExclusionLists(
      sexual = allSexualOffence,
      domesticAbuse = domesticViolenceMappings.map { transform(it) }.sortedBy { it.code }.toSet(),
      nationalSecurity = securityOffencesFromLegislation.map { transform(it, emptyList<Offence>()) }
        .sortedBy { it.code }.toSet(),
      violent = (part1Mappings.map { transform(it) }).sortedBy { it.code }.toSet(),
      terrorism = terrorismMappings.map { transform(it) }.sortedBy { it.code }.toSet(),
    )
  }

  private fun hasSexualCodePrefix(offenceCode: String): Boolean =
    SEXUAL_CODES_FOR_EXCLUSION_LIST.any { offenceCode.startsWith(it) }

  private fun getSecurityOffencesLegislation(): List<Offence> =
    offenceRepository.findByLegislationLikeIgnoreCase(
      NATIONAL_SECURITY_LEGISLATION[0],
      NATIONAL_SECURITY_LEGISLATION[1],
      NATIONAL_SECURITY_LEGISLATION[2],
      NATIONAL_SECURITY_LEGISLATION[3],
    )

  private fun getSexOffencesLegislation(): List<Offence> =
    offenceRepository.findByLegislationLikeIgnoreCase(SEXUAL_OFFENCES_LEGISLATION)

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
    val (part1Mappings, part2Mappings) = getSchedule15Mappings()
    val part1LifeMappings = part1Mappings.filter { it.offence.maxPeriodIsLife == true }
    val part2LifeMappings = part2Mappings.filter { it.offence.maxPeriodIsLife == true }
    val seriousViolentOffenceMappings =
      part1Mappings.filter { it.paragraphNumber == "1" || it.paragraphNumber == "4" || it.paragraphNumber == "6" || it.paragraphNumber == "64" || it.paragraphNumber == "65" }
    return Triple(part1LifeMappings, part2LifeMappings, seriousViolentOffenceMappings)
  }

  private fun getSchedule15Mappings(): Pair<List<OffenceScheduleMapping>, List<OffenceScheduleMapping>> {
    val schedule15 = scheduleRepository.findOneByActAndCode("Criminal Justice Act 2003", "15")
      ?: throw EntityNotFoundException("Schedule 15 not found")
    val parts = schedulePartRepository.findByScheduleId(schedule15.id)
    val part1Mappings =
      offenceScheduleMappingRepository.findBySchedulePartId(parts.first { p -> p.partNumber == 1 }.id)
    val part2Mappings =
      offenceScheduleMappingRepository.findBySchedulePartId(parts.first { p -> p.partNumber == 2 }.id)
    return Pair(part1Mappings, part2Mappings)
  }

  private fun getDomesticViolenceScheduleMappings(): List<OffenceScheduleMapping> {
    val domesticViolenceSchedule =
      scheduleRepository.findOneByActAndCode("Domestic Violence Excluded Offences", "DVEO")
        ?: throw EntityNotFoundException("Domestic Violence Schedule Not Found")
    return offenceScheduleMappingRepository.findBySchedulePartScheduleId(domesticViolenceSchedule.id)
  }

  private fun getTerrorismScheduleMappings(): List<OffenceScheduleMapping> {
    val terrorismSchedule =
      scheduleRepository.findOneByActAndCode("Terrorism Excluded Offences", "TEO")
        ?: throw EntityNotFoundException("Terrorism Schedule Not Found")
    return offenceScheduleMappingRepository.findBySchedulePartScheduleId(terrorismSchedule.id)
  }

  // List A: Schedule 15 Part 1 + Schedule 15 Part 2 that attract life (exclude all offences that start on or after 28 June 2022)
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

  fun getPcscLists(): PcscLists {
    val (part1LifeMappings, part2LifeMappings, seriousViolentOffenceMappings) = getSchedule15PcscMappings()
    val listA = mutableSetOf<OffenceToScheduleMapping>()
    val listB = mutableSetOf<OffenceToScheduleMapping>()
    val listC = mutableSetOf<OffenceToScheduleMapping>()
    val listD = mutableSetOf<OffenceToScheduleMapping>()

    part1LifeMappings.plus(part2LifeMappings).plus(seriousViolentOffenceMappings).forEach {
      if (inListA(part1LifeMappings, part2LifeMappings, it.offence.code)) listA.add(transform(it))
      if (inListB(seriousViolentOffenceMappings, part2LifeMappings, it.offence.code)) listB.add(transform(it))
      if (inListC(seriousViolentOffenceMappings, part2LifeMappings, it.offence.code)) listC.add(transform(it))
      if (inListD(part1LifeMappings, part2LifeMappings, it.offence.code)) listD.add(transform(it))
    }

    log.info("Returning PCSC lists sizes = list A: ${listA.size}, list B: ${listB.size}, list C: ${listC.size}, list D: ${listD.size}")

    return PcscLists(
      listA = listA.sortedBy { it.code }.toSet(),
      listB = listB.sortedBy { it.code }.toSet(),
      listC = listC.sortedBy { it.code }.toSet(),
      listD = listD.sortedBy { it.code }.toSet(),
    )
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    val SDS_LIST_A_CUT_OFF_DATE: LocalDate = LocalDate.of(2022, 6, 28)
    val NATIONAL_SECURITY_LEGISLATION = listOf(
      "National Security Act 2023",
      "Official Secrets Act 1989",
      "Official Secrets Act 1920",
      "Official Secrets Act 1911",
    )
    const val SEXUAL_OFFENCES_LEGISLATION = "Sexual Offences Act 2003"
    val SEXUAL_CODES_FOR_EXCLUSION_LIST = listOf("SX03", "SX56")
    val SEXUAL_EXLUDED_OFFENCES_SCHEDULE = ScheduleInfo(
      act = "Sexual Excluded Offences",
      code = "SEO",
    )
  }
}
