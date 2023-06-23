package uk.gov.justice.digital.hmpps.manageoffencesapi.service

// TODO temporarily commented out whilst diagnosing aws integration issue
class EventServiceTest {

  // private val objectMapper = ObjectMapper()
  // private val hmppsQueueService = mock<HmppsQueueService>()
  // private val eventService = EventService(hmppsQueueService, objectMapper)
  // private val topicSnsClient = mock<SnsAsyncClient>()
  // private val hmppsEventsTopic = HmppsTopic("hmppseventstopic", "some_arn", topicSnsClient)

  // @BeforeEach
  // fun `setup mocks`() {
  //   whenever(hmppsQueueService.findByTopicId(anyString())).thenReturn(hmppsEventsTopic)
  // }
  //
  // @Nested
  // inner class OffenceUpdatedEvent {
  //   @Test
  //   fun `should include event type as a message attribute`() {
  //     eventService.publishOffenceChangedEvent("some_code")
  //
  //     verify(topicSnsClient).publish(
  //       any<PublishRequest>(),
  //     )
  //   }
  //
  //   @Test
  //   fun `should not swallow exceptions`() {
  //     whenever(topicSnsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException::class.java)
  //
  //     assertThatThrownBy {
  //       eventService.publishOffenceChangedEvent("some_offence_code")
  //     }.isInstanceOf(RuntimeException::class.java)
  //   }
  // }
}
