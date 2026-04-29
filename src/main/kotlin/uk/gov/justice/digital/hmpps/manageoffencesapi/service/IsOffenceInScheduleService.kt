package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.T3_OFFENCE_EXCLUSIONS
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffencePcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusion
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusion.Companion.getSdsExclusionIndicator
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ScheduleInfo
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SdsOffenceDetails
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.FeatureToggleRepository
import java.time.LocalDate

@Service
class IsOffenceInScheduleService(
  private val featureToggleRepository: FeatureToggleRepository,
  private val cachedScheduleService: CachedScheduleService,
) {

  fun findPcscMarkers(offenceCodes: List<String>): List<OffencePcscMarkers> {
    log.info("Determining PCSC schedules for passed in offences")
    return getOffencePcscMarkers(offenceCodes)
  }

  fun categoriseSdsExclusionsOffences(offenceCodes: List<String>): List<OffenceSdsExclusion> {
    log.info("Determining Sexual or Violent status for passed in offences")
    return getSdsExclusionIndicators(offenceCodes)
  }

  private fun getSdsExclusionIndicators(offenceCodes: List<String>): List<OffenceSdsExclusion> {
    val scheduleInfo = cachedScheduleService.getCachedScheduleInformation()

    return offenceCodes.map { offenceCode ->
      getSds40Exclusions(offenceCode, scheduleInfo)
    }
  }

  fun getSdsOffenceDetails(offenceCodes: List<String>): List<SdsOffenceDetails> {
    log.info("Determining SDS markers for passed in offences")
    val scheduleInfo = cachedScheduleService.getCachedScheduleInformation()
    return offenceCodes.map { offenceCode ->
      val pcscMarkers = getPcscMarkers(offenceCode, scheduleInfo).pcscMarkers
      val sds40Exclusion = getSds40Exclusions(offenceCode, scheduleInfo).schedulePart.let { sds40ExclusionMarker ->
        if (sds40ExclusionMarker != OffenceSdsExclusionIndicator.NONE) {
          sds40ExclusionMarker
        } else {
          null
        }
      }
      val progressionModelExclusion = getProgressionModelExclusion(offenceCode, scheduleInfo)
      SdsOffenceDetails(
        offenceCode = offenceCode,
        pcscMarkers = pcscMarkers,
        earlyReleaseExclusions = listOfNotNull(sds40Exclusion, progressionModelExclusion),
      )
    }
  }

  private fun getSds40Exclusions(
    offenceCode: String,
    scheduleInfo: CachedScheduleInformation,
  ): OffenceSdsExclusion {
    val isSexual = scheduleInfo.part2Mappings.contains(offenceCode) ||
      hasSexualCodePrefix(offenceCode) ||
      scheduleInfo.sexualOffencesFromLegislation.contains(offenceCode) ||
      scheduleInfo.sexScheduleMappings.contains(offenceCode)

    val isSexualTranche3 = scheduleInfo.trancheThreeSexScheduleMappings.contains(offenceCode)
    val isDomesticViolenceTranche3 = scheduleInfo.trancheThreeViolenceMappings.contains(offenceCode)
    val isMurderTranche3 = scheduleInfo.tranceThreeMurderMappings.contains(offenceCode)

    val isDomesticViolence = scheduleInfo.domesticViolenceMappings.contains(offenceCode)
    val isNationalSecurity = scheduleInfo.securityOffencesFromLegislation.contains(offenceCode)
    val isViolent = scheduleInfo.part1Mappings.contains(offenceCode)
    val isTerrorism = scheduleInfo.terrorismMapping.contains(offenceCode)

    val indicator = getSdsExclusionIndicator(
      isSexual,
      isDomesticViolence,
      isNationalSecurity,
      isTerrorism,
      isViolent,
      isDomesticViolenceTranche3,
      isSexualTranche3,
      isMurderTranche3,
    )

    return OffenceSdsExclusion(
      offenceCode = offenceCode,
      schedulePart = indicator,
    )
  }

  private fun getProgressionModelExclusion(offenceCode: String, scheduleInfo: CachedScheduleInformation): OffenceSdsExclusionIndicator? = if (scheduleInfo.schedule13Part3Mappings.contains(offenceCode)) {
    OffenceSdsExclusionIndicator.SCHEDULE_13_PART_3
  } else {
    null
  }

  private fun hasSexualCodePrefix(offenceCode: String): Boolean = SEXUAL_CODES_FOR_EXCLUSION_LIST.any { offenceCode.startsWith(it) }

  private fun getOffencePcscMarkers(offenceCodes: List<String>): List<OffencePcscMarkers> {
    val scheduleInfo = cachedScheduleService.getCachedScheduleInformation()

    return offenceCodes.map {
      getPcscMarkers(it, scheduleInfo)
    }
  }

  private fun getPcscMarkers(
    offenceCode: String,
    scheduleInfo: CachedScheduleInformation,
  ): OffencePcscMarkers = OffencePcscMarkers(
    offenceCode = offenceCode,
    PcscMarkers(
      inListA = inListA(scheduleInfo.part1LifeMappings, scheduleInfo.part2LifeMappings, offenceCode),
      inListB = inListB(scheduleInfo.seriousViolentOffenceMappings, scheduleInfo.part2LifeMappings, offenceCode),
      inListC = inListC(scheduleInfo.seriousViolentOffenceMappings, scheduleInfo.part2LifeMappings, offenceCode),
      inListD = inListD(scheduleInfo.part1LifeMappings, scheduleInfo.part2LifeMappings, offenceCode),
    ),
  )

  // List A: Schedule 15 Part 1 + Schedule 15 Part 2 that attract life (exclude all offences that start on or after 28 June 2022)
// NOMIS SCHEDULE_15_ATTRACTS_LIFE - SDS >7 years between 01 April 2020 and 28 June 2022
  private fun inListA(
    part1LifeMappings: Set<OffenceAndStartDate>,
    part2LifeMappings: Set<OffenceAndStartDate>,
    offenceCode: String,
  ): Boolean = part1LifeMappings.any { p -> offenceCode == p.code && p.startDate < SDS_LIST_A_CUT_OFF_DATE } ||
    part2LifeMappings.any { p ->
      offenceCode == p.code && p.startDate < SDS_LIST_A_CUT_OFF_DATE
    }

  // List B: Schedule 15 Part 2 that attract life + serious violent offences (same as List C)
// NOMIS - PCSC_SDS - SDS between 4 and 7 years
  private fun inListB(
    seriousViolentOffenceMappings: Set<OffenceAndStartDate>,
    part2LifeMappings: Set<OffenceAndStartDate>,
    offenceCode: String,
  ) = seriousViolentOffenceMappings.any { p -> offenceCode == p.code } || part2LifeMappings.any { p -> offenceCode == p.code }

  // List C: Schedule 15 Part 2 that attract life + serious violent offences (same as List B)
// NOMIS - PCSC_SEC_250 - Sec250 >7 years
  private fun inListC(
    seriousViolentOffenceMappings: Set<OffenceAndStartDate>,
    part2LifeMappings: Set<OffenceAndStartDate>,
    offenceCode: String,
  ) = seriousViolentOffenceMappings.any { p -> offenceCode == p.code } || part2LifeMappings.any { p -> offenceCode == p.code }

  // List D: Schedule 15 Part 1 + Schedule 15 Part 2 that attract life
// NOMIS - PCSC_SDS_PLUS
  private fun inListD(
    part1LifeMappings: Set<OffenceAndStartDate>,
    part2LifeMappings: Set<OffenceAndStartDate>,
    offenceCode: String,
  ) = part1LifeMappings.any { p -> offenceCode == p.code } || part2LifeMappings.any { p -> offenceCode == p.code }

  private fun trancheThreeEnabled() = featureToggleRepository.findById(T3_OFFENCE_EXCLUSIONS).map { it.enabled }.orElse(false)

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    val SDS_LIST_A_CUT_OFF_DATE: LocalDate = LocalDate.of(2022, 6, 28)

    val SEXUAL_CODES_FOR_EXCLUSION_LIST = listOf("SX03", "SX56")

    val SCHEDULE_15 = ScheduleInfo(
      act = "Criminal Justice Act 2003",
      code = "15",
    )
  }
}
