package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class PcscMarkers(
  @Schema(description = "Schedule 15 Part 1 + Schedule 15 Part 2 that attract life (exclude all offences that start on or after 28 June 2022)")
  val inListA: Boolean,
  @Schema(description = "SDS between 4 and 7 years : Schedule 15 Part 2 that attract life + serious violent offences")
  val inListB: Boolean,
  @Schema(description = "Sec250 >7 years = List C: Schedule 15 Part 2 that attract life + serious violent offences (same as List B)")
  val inListC: Boolean,
  @Schema(description = "Schedule 15 Part 1 + Schedule 15 Part 2 that attract life")
  val inListD: Boolean,
)

@Schema(description = "Shows which (if any) PCSC Marker the offence relates to")
data class OffencePcscMarkers(
  val offenceCode: String,
  val pcscMarkers: PcscMarkers,
)
