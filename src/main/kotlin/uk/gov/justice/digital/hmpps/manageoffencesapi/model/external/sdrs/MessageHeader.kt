package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType
import java.time.ZonedDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class MessageHeader(
  val from: String,
  val messageID: MessageID,
  @Enumerated(EnumType.STRING)
  val messageType: MessageType,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  val timeStamp: ZonedDateTime,
  val to: String,
)
