package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Schedule details")
data class ScheduleDetails(
  val id: Long? = null,
  val act: String,
  val code: String,
  val url: String? = null,
  val schedulePartNumbers: List<Int>? = null,
)
