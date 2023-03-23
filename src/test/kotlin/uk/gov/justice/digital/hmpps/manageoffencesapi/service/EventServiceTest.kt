package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.amazonaws.services.sns.AmazonSNS
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic

class EventServiceTest {

  private val objectMapper = ObjectMapper()
  private val hmppsQueueService = mock<HmppsQueueService>()
  private val eventService = EventService(hmppsQueueService, objectMapper)
  private val topicSnsClient = mock<AmazonSNS>()
  private val hmppsEventsTopic = HmppsTopic("hmppseventstopic", "some_arn", topicSnsClient)

  @BeforeEach
  fun `setup mocks`() {
    whenever(hmppsQueueService.findByTopicId(anyString())).thenReturn(hmppsEventsTopic)
  }

  @Nested
  inner class OffenceUpdatedEvent {
    @Test
    fun `should include event type as a message attribute`() {
      eventService.publishOffenceChangedEvent("some_code")

      verify(topicSnsClient).publish(
        check {
          assertThat(it.messageAttributes["eventType"]?.stringValue).isEqualTo("manage-offences.offence.changed")
        },
      )
    }

    @Test
    fun `should not swallow exceptions`() {
      whenever(topicSnsClient.publish(any())).thenThrow(RuntimeException::class.java)

      assertThatThrownBy {
        eventService.publishOffenceChangedEvent("some_offence_code")
      }.isInstanceOf(RuntimeException::class.java)
    }
  }
}
