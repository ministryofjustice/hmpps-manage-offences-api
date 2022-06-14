package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.FeatureToggleRepository

@Service
class AdminService(
  private val featureToggleRepository: FeatureToggleRepository,
) {
  @Transactional
  fun toggleFeature(featureToggle: FeatureToggle) {
    featureToggleRepository.findById(featureToggle.feature)
      .ifPresent { featureToggleRepository.save(it.copy(enabled = featureToggle.enabled)) }
  }

  @Transactional(readOnly = true)
  fun getAllToggles(): List<FeatureToggle> =
    featureToggleRepository.findAll().map { transform(it) }

  fun isFeatureEnabled(feature: Feature): Boolean =
    featureToggleRepository.findById(feature).map { it.enabled }.orElse(false)

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
