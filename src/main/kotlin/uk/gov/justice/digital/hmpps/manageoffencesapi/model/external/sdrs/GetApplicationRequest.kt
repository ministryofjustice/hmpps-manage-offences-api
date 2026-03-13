package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import tools.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class GetApplicationRequest(
  val allOffences: String,
  @JsonProperty("CJSCode")
  val cjsCode: String? = null,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val changedDate: LocalDateTime? = null,
)
