package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Schedule part details and associated offences")
data class SchedulePart(
  val id: Long? = null,
  val partNumber: Int,
  val offences: List<Offence>? = null,
)
