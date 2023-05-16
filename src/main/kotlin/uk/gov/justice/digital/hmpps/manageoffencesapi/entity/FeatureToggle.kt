package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature

@Entity
@Table
data class FeatureToggle(
  @Id
  @Enumerated(EnumType.STRING)
  val feature: Feature,
  val enabled: Boolean,
)
