package uk.gov.justice.digital.hmpps.manageoffencesapi.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource

@Configuration
@Profile("!test") // prevent scheduler running during integration tests
@EnableScheduling
@EnableSchedulerLock(
  defaultLockAtLeastFor = "PT10M",
  defaultLockAtMostFor = "PT10M",
)
class SchedulerConfiguration {
  @Bean
  fun lockProvider(dataSource: DataSource): LockProvider = JdbcTemplateLockProvider(dataSource)
}
