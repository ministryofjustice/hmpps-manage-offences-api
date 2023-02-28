package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Schedule part ID and Offence ID - used for unlinking offences from schedules")
data class SchedulePartIdAndOffenceId(
  val schedulePartId: Long,
  val offenceId: Long,
)
