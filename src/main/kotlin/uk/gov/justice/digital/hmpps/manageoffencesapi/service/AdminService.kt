package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.EventToRaise
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.EventType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.EventToRaiseRepository
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
  private val eventToRaiseRepository: EventToRaiseRepository,
) {

  private val encouragementOffenceEligibilityStartDate = LocalDate.parse("2008-02-15")

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
        throw ValidationException("The offence flag is already set to active")
      }
      prisonApiUserClient.changeOffenceActiveFlag(transform(offence, true))
      offenceReactivatedInNomisRepository.save(transform(offence, getCurrentAuthentication().principal))
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
        throw ValidationException("The offence $offenceId is already inactive in NOMIS")
      }
      prisonApiUserClient.changeOffenceActiveFlag(transform(offence, false))

      if (offenceReactivatedInNomisRepository.existsById(offence.code)) {
        offenceReactivatedInNomisRepository.deleteById(offence.code)
      }
    }
  }

  @Transactional
  fun createEncouragementOffence(parentOffenceId: Long): Offence {
    val offence = offenceRepository.findById(parentOffenceId)
      .orElseThrow { EntityNotFoundException("Offence not found with ID $parentOffenceId") }

    // Parent offence should have no parentCode, or parentCode must be the same as the current Offence (indicating the parent)
    if (offence.parentCode !== null && offence.parentCode !== offence.code) {
      throw ValidationException("Offence must be a valid parent")
    }

    if (offence.endDate !== null && offence.endDate < encouragementOffenceEligibilityStartDate) {
      throw ValidationException("Offence must have an end date post $encouragementOffenceEligibilityStartDate")
    }

    val children = offenceRepository.findByParentOffenceId(parentOffenceId)
    val encouragementCode = offence.code + 'E'

    if (children.any { it.code == encouragementCode }) {
      throw ValidationException("Encouragement offence already exists for offence code $encouragementCode")
    }

    val prisonRecord = prisonApiClient.findByOffenceCodeStartsWith(offence.code, 0)

    if (prisonRecord.content.isEmpty()) {
      throw ValidationException("No prison record was found for offence code ${offence.code}")
    }

    val encouragementOffence = offence.copy(
      id = -1,
      code = encouragementCode,
      description = "Encouragement to ${offence.description}",
      cjsTitle = "Encouragement to ${offence.cjsTitle}",
    )

    val newOffence = offenceRepository.save(encouragementOffence)

    prisonApiClient.createOffences(
      listOf(
        prisonRecord.content.first().copy(
          code = encouragementOffence.code,
          description = encouragementOffence.description!!,
        ),
      ),
    )

    eventToRaiseRepository.save(
      EventToRaise(
        offenceCode = encouragementOffence.code,
        eventType = EventType.OFFENCE_CHANGED,
      ),
    )

    return transform(newOffence, childOffenceIds = listOf())
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
