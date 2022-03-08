package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class Offence(
  @JsonProperty("OffenceRevisionId")
  val offenceRevisionId: Int,
  val code: String,
  @JsonProperty("OffenceStartDate")
  val offenceStartDate: LocalDate,
  @JsonProperty("OffenceEndDate")
  val offenceEndDate: LocalDate?
)