package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.util.UUID

@JsonNaming(UpperCamelCaseStrategy::class)
data class MessageID(
  val relatesTo: String,
  @JsonProperty("UUID")
  val uuid: UUID,
)
