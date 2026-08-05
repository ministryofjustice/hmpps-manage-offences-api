package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Contains the list of all the offences that are excluded from progression model")
data class ProgressionModelExclusionLists(
  @param:Schema(description = "Offence under the SA2026 Excluded Offences for Progression Model schedule")
  val sentencingAct2026ProgressionModelExclusions: Set<OffenceToScheduleMapping>,
)
