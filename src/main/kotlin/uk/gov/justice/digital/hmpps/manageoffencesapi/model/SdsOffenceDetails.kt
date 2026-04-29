package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Shows which (if any) PCSC Marker the offence relates to as well as any SDS early release exclusions for the offence")
data class SdsOffenceDetails(
  val offenceCode: String,
  val pcscMarkers: PcscMarkers,
  val earlyReleaseExclusions: List<OffenceSdsExclusionIndicator>,
)
