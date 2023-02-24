package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Schedule paragraph ID and Offence ID - used for unlinking offences from schedules")
data class ScheduleParagraphIdAndOffenceId(
  val scheduleParagraphId: Long,
  val offenceId: Long,
)
