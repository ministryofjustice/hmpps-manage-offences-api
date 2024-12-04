package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Contains the list of all the offences that are sexual, domestic abuse, national security, terrorism or violent")
data class SdsExclusionLists(
  @Schema(description = "Offence falls under the Sexual category")
  val sexual: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Tranche Three Offence falls under the Sexual category")
  val sexualTrancheThree: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence falls under the Domestic Abuse category")
  val domesticAbuse: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Tranche Three Offence falls under the Domestic Abuse category")
  val domesticAbuseTrancheThree: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence falls under the National Security category")
  val nationalSecurity: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence falls under the Violent category")
  val violent: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Offence falls under the Terrorism category")
  val terrorism: Set<OffenceToScheduleMapping> = emptySet(),
  @Schema(description = "Tranche Three Offence falls under the Murder category")
  val murderTrancheThree: Set<OffenceToScheduleMapping> = emptySet(),
)
