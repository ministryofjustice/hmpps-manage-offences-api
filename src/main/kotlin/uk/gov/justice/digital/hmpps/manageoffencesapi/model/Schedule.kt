package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ScheduleStatus

@Schema(description = "Schedule details")
data class Schedule(
  val id: Long,
  val act: String,
  val code: String,
  val url: String?,
  val scheduleParts: List<SchedulePart>?,
  @Schema(
    description = "Publication status. Schedules are always created as DRAFT; the value supplied on create is ignored. " +
      "DRAFT schedules are only returned to callers holding ROLE_MANAGE_OFFENCES_ADMIN.",
  )
  val status: ScheduleStatus = ScheduleStatus.LIVE,
)

data class ScheduleInfo(
  val act: String,
  val code: String,
)

@Schema(description = "Request to change the publication status of a schedule")
data class ScheduleStatusRequest(
  val status: ScheduleStatus,
)
