package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Offence details")
data class Offence(
  @Schema(description = "Unique ID of the offence")
  val id: Long,
  @Schema(description = "The offence code")
  val code: String? = null,
  @Schema(description = "The offence description")
  val description: String? = null,
)
