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
import java.util.concurrent.TimeUnit.HOURS

@Configuration
@EnableCaching
@EnableScheduling
class CacheConfiguration {

  @Bean
  fun cacheManager(): CacheManager {
    return ConcurrentMapCacheManager(
      PCSC_LISTS,
      PCSC_MARKERS,
      SDS_EARLY_RELEASE_EXCLUSION_LISTS,
      SDS_EARLY_RELEASE_EXCLUSIONS,
      HO_CODE,
    )
  }

  @CacheEvict(allEntries = true, cacheNames = [PCSC_LISTS, PCSC_MARKERS, SDS_EARLY_RELEASE_EXCLUSION_LISTS, SDS_EARLY_RELEASE_EXCLUSIONS])
  @Scheduled(fixedDelay = 2, timeUnit = HOURS)
  fun cacheEvictEveryTwoHours() {
    log.info("Evicting caches: {}, {}, {}, {}", PCSC_LISTS, PCSC_MARKERS, SDS_EARLY_RELEASE_EXCLUSION_LISTS, SDS_EARLY_RELEASE_EXCLUSIONS)
  }

  @CacheEvict(allEntries = true, cacheNames = [HO_CODE])
  @Scheduled(fixedDelay = 24, timeUnit = HOURS)
  fun cacheEvictDaily() {
    log.info("Evicting cache: {}", HO_CODE)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(CacheConfiguration::class.java)
    const val PCSC_LISTS: String = "pcscLists"
    const val PCSC_MARKERS: String = "pcscMarkers"
    const val SDS_EARLY_RELEASE_EXCLUSION_LISTS: String = "sdsEarlyReleaseExclusionLists"
    const val SDS_EARLY_RELEASE_EXCLUSIONS: String = "sdsEarlyReleaseExclusions"
    const val HO_CODE: String = "hoCode"
  }
}
