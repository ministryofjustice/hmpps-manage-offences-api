package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tools.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import tools.jackson.databind.annotation.JsonNaming
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType
import java.time.ZonedDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class MessageHeader(
  val from: String,
  val messageID: MessageID,
  @Enumerated(EnumType.STRING)
  val messageType: MessageType,
  val timeStamp: ZonedDateTime,
  val to: String,
)
