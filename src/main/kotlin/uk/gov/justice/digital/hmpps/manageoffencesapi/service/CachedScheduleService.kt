package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration.Companion.SCHEDULE_DATA
import java.time.LocalDate

@Service
class CachedScheduleService(
  private val scheduleService: ScheduleService,
) {

  @Cacheable(SCHEDULE_DATA, key = "'master'")
  @Transactional(readOnly = true)
  fun getCachedScheduleInformation(): CachedScheduleInformation {
    val (part1Mappings, part2Mappings) = scheduleService.getSchedule15Mappings()
    val (domesticViolenceMappings, trancheThreeViolenceMappings) = scheduleService.getDomesticViolenceScheduleMappings()
    val securityOffencesFromLegislation = scheduleService.getSecurityOffencesLegislation()
    val sexualOffencesFromLegislation = scheduleService.getSexOffencesLegislation()
    val terrorismMapping = scheduleService.getTerrorismScheduleMappings()
    val (sexScheduleMappings, trancheThreeSexScheduleMappings) = scheduleService.getSexScheduleMappings()
    val tranceThreeMurderMappings = scheduleService.getTrancheThreeMurderScheduleMappings()

    val (part1LifeMappings, part2LifeMappings, seriousViolentOffenceMappings) = scheduleService.getSchedule15PcscMappings()

    return CachedScheduleInformation(
      part1Mappings.map { it.offence.code }.toSet(),
      part2Mappings.map { it.offence.code }.toSet(),
      domesticViolenceMappings.map { it.offence.code }.toSet(),
      trancheThreeViolenceMappings.map { it.offence.code }.toSet(),
      securityOffencesFromLegislation.map { it.code }.toSet(),
      sexualOffencesFromLegislation.map { it.code }.toSet(),
      terrorismMapping.map { it.offence.code }.toSet(),
      sexScheduleMappings.map { it.offence.code }.toSet(),
      trancheThreeSexScheduleMappings.map { it.offence.code }.toSet(),
      tranceThreeMurderMappings.map { it.offence.code }.toSet(),
      part1LifeMappings.map { OffenceAndStartDate(it.offence.code, it.offence.startDate) }.toSet(),
      part2LifeMappings.map { OffenceAndStartDate(it.offence.code, it.offence.startDate) }.toSet(),
      seriousViolentOffenceMappings.map { OffenceAndStartDate(it.offence.code, it.offence.startDate) }.toSet(),
    )
  }
}

data class CachedScheduleInformation(
  val part1Mappings: Set<String>,
  val part2Mappings: Set<String>,
  val domesticViolenceMappings: Set<String>,
  val trancheThreeViolenceMappings: Set<String>,
  val securityOffencesFromLegislation: Set<String>,
  val sexualOffencesFromLegislation: Set<String>,
  val terrorismMapping: Set<String>,
  val sexScheduleMappings: Set<String>,
  val trancheThreeSexScheduleMappings: Set<String>,
  val tranceThreeMurderMappings: Set<String>,
  val part1LifeMappings: Set<OffenceAndStartDate>,
  val part2LifeMappings: Set<OffenceAndStartDate>,
  val seriousViolentOffenceMappings: Set<OffenceAndStartDate>,
)

data class OffenceAndStartDate(
  val code: String,
  val startDate: LocalDate,
)
