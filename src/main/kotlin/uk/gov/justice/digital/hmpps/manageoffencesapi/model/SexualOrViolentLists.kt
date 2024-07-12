package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Contains the list of all the offences that are sexual, domestic abuse, national security or violent")
data class SexualOrViolentLists(
  @Schema(description = "Offence falls under the Sexual category")
  val sexual: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence falls under the Domestic Abuse category")
  val domesticAbuse: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence falls under the National Security category")
  val nationalSecurity: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence falls under the Violent category")
  val violent: Set<OffenceToScheduleMapping> = emptySet(),
)
