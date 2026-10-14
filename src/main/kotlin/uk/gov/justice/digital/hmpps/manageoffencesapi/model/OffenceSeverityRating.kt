package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Severity rating for an offence code")
data class OffenceSeverityRating(
  @Schema(description = "The offence code")
  val offenceCode: String,
  @Schema(description = "The NOMIS severity ranking for the offence")
  val severityRating: String,
)
