package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.LocalDate

@JsonNaming(UpperCamelCaseStrategy::class)
data class Offence(
  val offenceRevisionId: Int,
  @JsonProperty("code")
  val code: String,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate?
)
