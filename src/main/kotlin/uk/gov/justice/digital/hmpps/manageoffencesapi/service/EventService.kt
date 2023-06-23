package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// TODO temporarily commented out whilst diagnosing aws integration issue
@Service
class EventService(
//   private val hmppsQueueService: HmppsQueueService,
//   private val mapper: ObjectMapper,
) {
//
//   private val domainTopic by lazy {
//     hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw IllegalStateException("hmppseventtopic topic not found")
//   }
//
//   fun publishOffenceChangedEvent(offenceCode: String) {
//     val event =
//       OffenceUpdatedDomainEvent(additionalInformation = (OffenceAdditionalInformation(offenceCode = offenceCode)))
//     domainTopic.snsClient.publish(
//       PublishRequest.builder()
//         .topicArn(domainTopic.arn)
//         .message(mapper.writeValueAsString(event))
//         .messageAttributes(mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(event.eventType).build()))
//         .build(),
//     )
//     log.info("Published 'offence changed event' for: $offenceCode")
//   }

  fun publishOffenceChangedEvent(offenceCode: String) {
    log.info("TEMP logging for publishing event (does nothing): $offenceCode")
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}

data class OffenceAdditionalInformation(
  val offenceCode: String,
)

abstract class OffenceDomainEvent {
  abstract val additionalInformation: OffenceAdditionalInformation
  val occurredAt: String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Europe/London")).format(Instant.now())
  abstract val eventType: String
  abstract val version: Int
  abstract var description: String
}

class OffenceUpdatedDomainEvent(
  override val eventType: String = "manage-offences.offence.changed",
  override var description: String = "An offence has been updated/created",
  override val additionalInformation: OffenceAdditionalInformation,
  override val version: Int = 1,
) : OffenceDomainEvent()
