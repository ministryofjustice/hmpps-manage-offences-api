package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class GetMojOffenceRequest(
  val allOffences: String,
  @JsonProperty("CJSCode")
  val cjsCode: String? = null,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val changedDate: LocalDateTime? = null,
)
