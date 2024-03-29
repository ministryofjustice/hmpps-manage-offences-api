package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class GetOffenceRequest(
  val allOffences: String,
  val alphaChar: Char? = null,
  @JsonProperty("CJSCode")
  val cjsCode: String? = null,
  val changedDate: LocalDateTime? = null,
)
