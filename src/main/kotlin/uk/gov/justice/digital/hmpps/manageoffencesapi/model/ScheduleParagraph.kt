package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Schedule paragraph details and associated offences")
data class ScheduleParagraph(
  val id: Long? = null,
  val paragraphNumber: Int,
  val paragraphTitle: String,
  val offences: List<Offence>? = null,
)
