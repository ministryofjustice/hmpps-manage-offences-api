package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Schedule details")
data class Schedule(
  val id: Long? = null,
  val act: String,
  val code: String,
  val url: String? = null,
  val scheduleParts: List<SchedulePart>? = null,
)
