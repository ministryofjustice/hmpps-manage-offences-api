package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Schedule details when associated to an offence")
data class LinkedScheduleDetails(
  val id: Long,
  val act: String,
  val code: String,
  val url: String? = null,
  val partNumber: Int,
  val paragraphNumber: String? = null,
  val paragraphTitle: String? = null,
  val lineReference: String? = null,
  val legislationText: String? = null,
)
