package uk.gov.justice.digital.hmpps.manageoffencesapi.service

class EventServiceTest {
  //
  // private val objectMapper = ObjectMapper()
  // private val hmppsQueueService = mock<HmppsQueueService>()
  // private val adminService = mock<AdminService>()
  // private val eventToRaiseRepository = mock<EventToRaiseRepository>()
  // private val eventService = EventService(hmppsQueueService, adminService, objectMapper, eventToRaiseRepository)
  // private val topicSnsClient = mock<SnsAsyncClient>()
  // private val hmppsEventsTopic = HmppsTopic("hmppseventstopic", "some_arn", topicSnsClient)
  //
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
  //
  //   @Test
  //   fun `Events are published correctly`() {
  //     whenever(adminService.isFeatureEnabled(Feature.PUBLISH_EVENTS)).thenReturn(true)
  //     whenever(eventToRaiseRepository.findAll()).thenReturn(
  //       listOf(
  //         EventToRaise(
  //           eventType = EventType.OFFENCE_CHANGED,
  //           offenceCode = "OF1",
  //         ),
  //       ),
  //     )
  //
  //     eventService.publishEvents()
  //
  //     verify(eventToRaiseRepository).deleteAll(
  //       listOf(
  //         EventToRaise(
  //           eventType = EventType.OFFENCE_CHANGED,
  //           offenceCode = "OF1",
  //         ),
  //       ),
  //     )
  //     verify(topicSnsClient).publish(any<PublishRequest>())
  //   }
  // }
}
