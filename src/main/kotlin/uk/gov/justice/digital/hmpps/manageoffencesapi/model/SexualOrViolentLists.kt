package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Contains the list of all the offences that are sexual (Schedule 3 or 15 Part 2) or violent (Schedule 15 Part 1)")
data class SexualOrViolentLists(
  @Schema(description = "Offence code starts with SX03 or SX56 or is in Schedule 15, Part 2")
  val sexualCodesAndS15P2: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence is in Schedule 3 or Schedule 15, Part 2")
  val sexualS3AndS15P2: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence is in Schedule 15, Part 1")
  val violent: Set<OffenceToScheduleMapping> = emptySet(),
)
