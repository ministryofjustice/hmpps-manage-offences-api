package uk.gov.justice.digital.hmpps.manageoffencesapi.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource

@Configuration
@Profile("!test") // prevent scheduler running during integration tests
@EnableScheduling
@EnableSchedulerLock(
  defaultLockAtLeastFor = "1m",
  defaultLockAtMostFor = "10m",
)
class SchedulerConfiguration {
  @Bean
  fun lockProvider(dataSource: DataSource): LockProvider = JdbcTemplateLockProvider(
    JdbcTemplateLockProvider.Configuration.builder()
      .withJdbcTemplate(JdbcTemplate(dataSource))
      .usingDbTime()
      .build(),
  )
}
