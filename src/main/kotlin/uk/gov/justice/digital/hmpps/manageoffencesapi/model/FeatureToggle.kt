package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature

@Schema(description = "Feature toggle details")
data class FeatureToggle(
  @Schema(description = "Feature to be toggled: FULL_SYNC_NOMIS, DELTA_SYNC_NOMIS, FULL_SYNC_SDRS or DELTA_SYNC_SDRS")
  val feature: Feature,
  @Schema(description = "true or false - depending on whether the feature should be enabled")
  val enabled: Boolean,
)
