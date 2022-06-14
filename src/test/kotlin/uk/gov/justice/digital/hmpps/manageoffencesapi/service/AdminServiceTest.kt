package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.FeatureToggleRepository
import java.util.Optional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.FeatureToggle as FeatureToggleEntity

class AdminServiceTest {
  private val featureToggleRepository = mock<FeatureToggleRepository>()

  private val adminService = AdminService(featureToggleRepository)

  @Test
  fun `If the feature doesnt exist then no save is performed`() {
    whenever(featureToggleRepository.findById(FULL_SYNC_NOMIS)).thenReturn(Optional.empty())

    adminService.toggleFeature(FeatureToggle(FULL_SYNC_NOMIS, true))

    verify(featureToggleRepository, times(1)).findById(FULL_SYNC_NOMIS)
    verifyNoMoreInteractions(featureToggleRepository)
  }

  @Test
  fun `Toggling a feature gets saved`() {
    whenever(featureToggleRepository.findById(FULL_SYNC_NOMIS)).thenReturn(Optional.of(FeatureToggleEntity(FULL_SYNC_NOMIS, false)))

    adminService.toggleFeature(FeatureToggle(FULL_SYNC_NOMIS, true))

    verify(featureToggleRepository, times(1)).save(FeatureToggleEntity(FULL_SYNC_NOMIS, true))
  }

  @Test
  fun `Get all toggles`() {
    whenever(featureToggleRepository.findAll()).thenReturn(listOf(FeatureToggleEntity(FULL_SYNC_NOMIS, false)))

    val res = adminService.getAllToggles()

    assertThat(res).isEqualTo(listOf(FeatureToggle(FULL_SYNC_NOMIS, false)))
  }
}
