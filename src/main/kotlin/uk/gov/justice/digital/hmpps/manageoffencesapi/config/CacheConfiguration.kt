package uk.gov.justice.digital.hmpps.manageoffencesapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS

@Configuration
@EnableCaching
@EnableScheduling
class CacheConfiguration {

  @Bean
  fun cacheManager(): CacheManager = ConcurrentMapCacheManager(
    PCSC_LISTS,
    PCSC_MARKERS,
    SDS_EARLY_RELEASE_EXCLUSION_LISTS,
    SDS_EARLY_RELEASE_EXCLUSIONS,
    TORERA_OFFENCE_CODES,
    OFFENCE_CODE_TO_HOME_OFFICE_CODE,
  )

  @CacheEvict(
    allEntries = true,
    cacheNames = [PCSC_LISTS, PCSC_MARKERS, SDS_EARLY_RELEASE_EXCLUSION_LISTS, SDS_EARLY_RELEASE_EXCLUSIONS, TORERA_OFFENCE_CODES],
  )
  @Scheduled(fixedDelay = 2, timeUnit = HOURS)
  fun cacheEvict() {
    log.info(
      "Evicting caches: {}, {}, {}, {}, {}",
      PCSC_LISTS,
      PCSC_MARKERS,
      SDS_EARLY_RELEASE_EXCLUSION_LISTS,
      SDS_EARLY_RELEASE_EXCLUSIONS,
      TORERA_OFFENCE_CODES,
    )
  }

  @CacheEvict(
    allEntries = true,
    cacheNames = [OFFENCE_CODE_TO_HOME_OFFICE_CODE],
  )
  @Scheduled(fixedDelay = 14, timeUnit = DAYS)
  fun cacheEvictEveryFourteenDays() {
    log.info("Evicting cache every 14 days: {}", OFFENCE_CODE_TO_HOME_OFFICE_CODE)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(CacheConfiguration::class.java)
    const val PCSC_LISTS: String = "pcscLists"
    const val PCSC_MARKERS: String = "pcscMarkers"
    const val SDS_EARLY_RELEASE_EXCLUSION_LISTS: String = "sdsEarlyReleaseExclusionLists"
    const val SDS_EARLY_RELEASE_EXCLUSIONS: String = "sdsEarlyReleaseExclusions"
    const val TORERA_OFFENCE_CODES: String = "toreraOffenceCodes"
    const val OFFENCE_CODE_TO_HOME_OFFICE_CODE: String = "offenceCodesToHomeOfficeCode"
  }
}
