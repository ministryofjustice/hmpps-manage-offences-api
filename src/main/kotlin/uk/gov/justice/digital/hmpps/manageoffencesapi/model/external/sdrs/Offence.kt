package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.LocalDate
import java.time.LocalDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class Offence(
  val offenceRevisionId: Int,
  @JsonProperty("code")
  val code: String,
  val description: String? = null,
  val cjsTitle: String? = null,
  val offenceStartDate: LocalDate? = null,
  val offenceEndDate: LocalDate? = null,
  val changedDate: LocalDateTime? = null
)
