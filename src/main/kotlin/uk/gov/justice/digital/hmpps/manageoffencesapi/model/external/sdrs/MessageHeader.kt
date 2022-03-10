package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.ZonedDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class MessageHeader(
  val from: String,
  val messageID: MessageID,
  val messageType: String,
  val timeStamp: ZonedDateTime,
  val to: String
)