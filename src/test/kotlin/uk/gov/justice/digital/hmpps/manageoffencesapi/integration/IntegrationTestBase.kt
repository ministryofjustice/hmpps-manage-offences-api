package uk.gov.justice.digital.hmpps.manageoffencesapi.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.justice.digital.hmpps.manageoffencesapi.helpers.JwtAuthHelper
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.wiremock.SDRSApiMockServer

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @MockitoBean
  lateinit var s3Client: S3Client

  @MockitoBean
  lateinit var s3AsyncClient: S3AsyncClient

  companion object {
    @JvmField
    internal val prisonApiMockServer = PrisonApiMockServer()

    @JvmField
    internal val hmppsAuthMockServer = HmppsAuthMockServer()

    @JvmField
    internal val sdrsApiMockServer = SDRSApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      hmppsAuthMockServer.start()
      hmppsAuthMockServer.stubGrantToken()
      prisonApiMockServer.start()
      sdrsApiMockServer.start()
      sdrsApiMockServer.addMockServiceRequestListener { request, response ->
        println("Request: ${request.bodyAsString}")
        println("Response: ${response.bodyAsString}")
      }
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      sdrsApiMockServer.stop()
      prisonApiMockServer.stop()
      hmppsAuthMockServer.stop()
    }

    private val pgContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
        registry.add("spring.flyway.url", pgContainer::getJdbcUrl)
        registry.add("spring.flyway.user", pgContainer::getUsername)
        registry.add("spring.flyway.password", pgContainer::getPassword)
      }

      System.setProperty("aws.region", "eu-west-2")
    }
  }

  internal fun setAuthorisation(
    user: String = "test-client",
    roles: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)
}
