package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceReactivatedInNomis
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_SDRS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_SDRS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.LINK_SCHEDULES_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.PUBLISH_EVENTS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.SYNC_HOME_OFFICE_CODES
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.T3_OFFENCE_EXCLUSIONS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.UNLINK_SCHEDULES_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceReactivatedInNomisRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository

class AdminControllerIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var offenceRepository: OffenceRepository

  @Autowired
  lateinit var nomisScheduleMappingRepository: NomisScheduleMappingRepository

  @Autowired
  lateinit var offenceScheduleMappingRepository: OffenceScheduleMappingRepository

  @Autowired
  lateinit var offenceReactivatedInNomisRepository: OffenceReactivatedInNomisRepository

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
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
          FeatureToggle(SYNC_HOME_OFFICE_CODES, true),
          FeatureToggle(PUBLISH_EVENTS, true),
          FeatureToggle(UNLINK_SCHEDULES_NOMIS, true),
          FeatureToggle(LINK_SCHEDULES_NOMIS, true),
          FeatureToggle(T3_OFFENCE_EXCLUSIONS, true),
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
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
          FeatureToggle(SYNC_HOME_OFFICE_CODES, true),
          FeatureToggle(PUBLISH_EVENTS, true),
          FeatureToggle(UNLINK_SCHEDULES_NOMIS, true),
          FeatureToggle(LINK_SCHEDULES_NOMIS, true),
          FeatureToggle(T3_OFFENCE_EXCLUSIONS, true),
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-active-and-inactive-offence.sql",
  )
  fun `Reactivate offence in NOMIS that is inactive`() {
    prisonApiMockServer.stubFindByOffenceCodeStartsWith("M5119999")
    prisonApiMockServer.stubActivateOffence()
    val offence = offenceRepository.findOneByCode("M5119999").get()

    webTestClient.post().uri("/admin/nomis/offences/reactivate")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_OFFENCE_ACTIVATOR")))
      .bodyValue(listOf(offence.id))
      .exchange()
      .expectStatus().isOk

    val reactivatedOffence = offenceReactivatedInNomisRepository.findById(offence.code).get()
    assertThat(reactivatedOffence)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*Date")
      .isEqualTo(
        OffenceReactivatedInNomis(
          offenceCode = offence.code,
          reactivatedByUsername = "test-client",
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-active-and-inactive-offence.sql",
  )
  fun `Attempt to reactivate offence in NOMIS that isn't end dated - returns 400 Validation Exception`() {
    prisonApiMockServer.stubFindByOffenceCodeStartsWith("M4119999")
    val offence = offenceRepository.findOneByCode("M4119999").get()

    webTestClient.post().uri("/admin/nomis/offences/reactivate")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_OFFENCE_ACTIVATOR")))
      .bodyValue(listOf(offence.id))
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-inactive-offence-and-reactivated.sql",
  )
  fun `Deactivate offence in NOMIS that is end dated - but is active in NOMIS`() {
    prisonApiMockServer.stubFindByOffenceCode("M5119999")
    prisonApiMockServer.stubDeactivateOffence()
    val offence = offenceRepository.findOneByCode("M5119999").get()

    webTestClient.post().uri("/admin/nomis/offences/deactivate")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_OFFENCE_ACTIVATOR")))
      .bodyValue(listOf(offence.id))
      .exchange()
      .expectStatus().isOk

    assertThat(offenceReactivatedInNomisRepository.findById(offence.code).isPresent).isFalse
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-schedule-and-offence-data.sql",
  )
  fun `Add encouragement offence and ensure schedule is updated`() {
    prisonApiMockServer.stubFindByOffenceCode("XX99001")

    // Mocks the correct schedule linking request for newly created XX99001E, if the request isn't correct the test will
    // fail.
    prisonApiMockServer.stubLinkOffence(linkOffenceRequest)
    prisonApiMockServer.stubCreateOffence()

    // Retrieve the offence to grab the generated ID of what will be the parent
    val parentOffence = offenceRepository.findOneByCode("XX99001").get()

    webTestClient.post().uri("/admin/nomis/offences/encouragement/${parentOffence.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_OFFENCE_ACTIVATOR")))
      .bodyValue(listOf(parentOffence.id))
      .exchange()
      .expectStatus().isOk

    val encouragementOffence = offenceRepository.findOneByCode("XX99001E").get()
    assertThat(encouragementOffence.parentOffenceId).isEqualTo(parentOffence.id)

    val childScheduleMappings = offenceScheduleMappingRepository.findByOffenceId(encouragementOffence.id)
    val parentScheduleMappings = offenceScheduleMappingRepository.findByOffenceId(parentOffence.id)

    assertThat(childScheduleMappings.map { it.schedulePart })
      .usingRecursiveComparison()
      .ignoringCollectionOrder()
      .isEqualTo(parentScheduleMappings.map { it.schedulePart })
  }

  private fun getFeatureToggles(): MutableList<FeatureToggle>? =
    webTestClient.get().uri("/admin/feature-toggles")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(FeatureToggle::class.java)
      .returnResult().responseBody

  companion object {
    private val linkOffenceRequest = """ [ {                         
            "offenceCode" : "XX99001E",
            "schedule" : "SCHEDULE_13"
          }]
    """.trimIndent()
  }
}
