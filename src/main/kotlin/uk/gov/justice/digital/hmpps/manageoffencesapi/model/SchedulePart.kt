package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Schedule part details and associated offences")
data class SchedulePart(
  val id: Long,
  val partNumber: Int,
  val scheduleParagraphs: List<ScheduleParagraph>? = null,
)
