package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class FeatureToggle(
  @Id
  @Enumerated(EnumType.STRING)
  val feature: Feature,
  val enabled: Boolean,
)
