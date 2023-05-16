package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.FeatureToggleRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceReactivatedInNomisRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import java.time.LocalDate

@Service
class AdminService(
  private val featureToggleRepository: FeatureToggleRepository,
  private val offenceRepository: OffenceRepository,
  private val offenceReactivatedInNomisRepository: OffenceReactivatedInNomisRepository,
  private val prisonApiClient: PrisonApiClient,
  private val prisonApiUserClient: PrisonApiUserClient,
) {
  @Transactional
  fun toggleFeature(featureToggles: List<FeatureToggle>) {
    featureToggles.forEach {
      featureToggleRepository.findById(it.feature)
        .ifPresent { featureToggle -> featureToggleRepository.save(featureToggle.copy(enabled = it.enabled)) }
    }
  }

  fun getCurrentAuthentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw IllegalStateException("User is not authenticated")

  @Transactional
  fun reactivateNomisOffence(offenceIds: List<Long>) {
    offenceIds.forEach { offenceId ->
      val offence = offenceRepository.findById(offenceId)
        .orElseThrow { EntityNotFoundException("Offence with ID $offenceId not found") }
      val nomisOffence = prisonApiClient
        .findByOffenceCodeStartsWith(offence.code, 0)
        .content.first { it.code == offence.code }

      if (nomisOffence.isActive) {
        throw ValidationException("Th offence flag is already set to active")
      }
      prisonApiUserClient.changeOffenceActiveFlag(transform(offence, true))
      offenceReactivatedInNomisRepository.save(transform(offenceId, getCurrentAuthentication().principal))
    }
  }

  @Transactional
  fun deactivateNomisOffence(offenceIds: List<Long>) {
    offenceIds.forEach { offenceId ->
      val offence = offenceRepository.findById(offenceId)
        .orElseThrow { EntityNotFoundException("Offence with ID $offenceId not found") }
      if (offence.endDate == null || offence.endDate.isAfter(LocalDate.now().minusDays(1))) {
        throw ValidationException("This offence's end date has not yet expired - therefore cannot deactivate in NOMIS")
      }
      val nomisOffence = prisonApiClient
        .findByOffenceCodeStartsWith(offence.code, 0)
        .content.first { it.code == offence.code }

      if (!nomisOffence.isActive) {
        throw ValidationException("Th offence $offenceId is already inactive in NOMIS")
      }
      prisonApiUserClient.changeOffenceActiveFlag(transform(offence, false))

      if (offenceReactivatedInNomisRepository.existsById(offenceId)) {
        offenceReactivatedInNomisRepository.deleteById(offenceId)
      }
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
