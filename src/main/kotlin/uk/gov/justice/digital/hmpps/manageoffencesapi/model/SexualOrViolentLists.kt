package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Contains the list of all the offences that are sexual (Schedule 3 or 15 Part 2) or violent (Schedule 15 Part 1)")
data class SexualOrViolentLists(
  @Schema(description = "Offence is in Schedule 3 or Schedule 15, Part 2")
  val sexual: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence is in Schedule 15, Part 1")
  val violent: Set<OffenceToScheduleMapping> = emptySet(),
)
