package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.SYNC_SDRS
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle

class AdminControllerIntTest : IntegrationTestBase() {
  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql"
  )
  fun `Get offences by offence code`() {
    val result = webTestClient.get().uri("/admin/feature-toggles")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(FeatureToggle::class.java)
      .returnResult().responseBody

    assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(
        listOf(
          FeatureToggle(FULL_SYNC_NOMIS, true),
          FeatureToggle(DELTA_SYNC_NOMIS, true),
          FeatureToggle(SYNC_SDRS, true),
        )
      )
  }
}
