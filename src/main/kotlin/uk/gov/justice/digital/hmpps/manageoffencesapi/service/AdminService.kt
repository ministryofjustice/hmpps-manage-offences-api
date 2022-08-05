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
  fun toggleFeature(featureToggles: List<FeatureToggle>) {
    featureToggles.forEach {
      featureToggleRepository.findById(it.feature)
        .ifPresent { featureToggle -> featureToggleRepository.save(featureToggle.copy(enabled = it.enabled)) }
    }
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
