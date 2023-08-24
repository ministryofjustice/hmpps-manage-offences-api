package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.EventToRaiseRepository
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class EventService(
  private val hmppsQueueService: HmppsQueueService,
  private val adminService: AdminService,
  private val mapper: ObjectMapper,
  private val eventToRaiseRepository: EventToRaiseRepository,
) {

  @Scheduled(cron = "0 5-55/10 * * * *")
  @SchedulerLock(name = "publishEvents")
  @Transactional
  fun publishEvents() {
    if (!adminService.isFeatureEnabled(Feature.PUBLISH_EVENTS)) {
      log.info("Publishing events not running - disabled")
      return
    }
    log.info("Publishing events starting")
    val eventsToRaise = eventToRaiseRepository.findAll()
    val failedToPublish = mutableListOf<String>()
    eventsToRaise.forEach {
      runCatching {
        publishOffenceChangedEvent(it.offenceCode)
      }.onFailure { error ->
        failedToPublish.add(it.offenceCode)
        log.error(
          "Failed to send changed-event for offence code  ${it.offenceCode}",
          error,
        )
      }
    }

    val eventsSuccessfullyPublished = eventsToRaise.filter { !failedToPublish.contains(it.offenceCode) }
    eventToRaiseRepository.deleteAll(eventsSuccessfullyPublished)
    log.info("Finished publishing events")
  }

  private val domainTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw IllegalStateException("hmppseventtopic topic not found")
  }

  fun publishOffenceChangedEvent(offenceCode: String) {
    val event =
      OffenceUpdatedDomainEvent(additionalInformation = (OffenceAdditionalInformation(offenceCode = offenceCode)))
    domainTopic.snsClient.publish(
      PublishRequest.builder()
        .topicArn(domainTopic.arn)
        .message(mapper.writeValueAsString(event))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(event.eventType).build(),
          ),
        )
        .build(),
    )
    log.info("Published 'offence changed event' for: $offenceCode")
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
  override val eventType: String = "XX-manage-offences.offence.changed",
  override var description: String = "XX An offence has been updated/created LOCAL TESTING",
  override val additionalInformation: OffenceAdditionalInformation,
  override val version: Int = 1,
) : OffenceDomainEvent()
