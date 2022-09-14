package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_SDRS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_SDRS
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle

class AdminControllerIntTest : IntegrationTestBase() {
  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql"
  )
  fun `Get feature toggles`() {
    val result = getFeatureToggles()

    assertThat(result)
      .usingRecursiveComparison()
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          FeatureToggle(FULL_SYNC_NOMIS, true),
          FeatureToggle(DELTA_SYNC_NOMIS, true),
          FeatureToggle(FULL_SYNC_SDRS, false),
          FeatureToggle(DELTA_SYNC_SDRS, true),
        )
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql"
  )
  fun `Update feature toggles`() {

    val fullSyncNomis = FeatureToggle(feature = FULL_SYNC_NOMIS, enabled = false)
    webTestClient.put().uri("/admin/toggle-feature")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_OFFENCES_ADMIN")))
      .bodyValue(listOf(fullSyncNomis))
      .exchange()
      .expectStatus().isOk

    val result = getFeatureToggles()

    assertThat(result)
      .usingRecursiveComparison()
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          FeatureToggle(FULL_SYNC_NOMIS, false),
          FeatureToggle(DELTA_SYNC_NOMIS, true),
          FeatureToggle(FULL_SYNC_SDRS, false),
          FeatureToggle(DELTA_SYNC_SDRS, true),
        )
      )
  }

  private fun getFeatureToggles(): MutableList<FeatureToggle>? =
    webTestClient.get().uri("/admin/feature-toggles")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(FeatureToggle::class.java)
      .returnResult().responseBody
}
