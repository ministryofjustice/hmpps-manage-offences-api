package uk.gov.justice.digital.hmpps.manageoffencesapi.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.manageoffencesapi.helpers.JwtAuthHelper
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.wiremock.SDRSApiMockServer

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import(IntegrationTestBase.MockS3Beans::class)
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var cacheConfiguration: CacheConfiguration

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var s3Client: S3Client

  @Autowired
  lateinit var s3AsyncClient: S3AsyncClient

  @BeforeEach
  fun increaseWebClientBuffer() {
    webTestClient = webTestClient.mutate()
      .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
      .build()
  }

  @TestConfiguration
  class MockS3Beans {
    @Bean
    @Primary
    fun mockS3Client(): S3Client = mock()

    @Bean
    @Primary
    fun mockS3AsyncClient(): S3AsyncClient = mock()

    @Bean
    @Primary
    fun awsCredentialsProvider(): AwsCredentialsProvider = mock()
  }

  protected fun resetCache() {
    cacheConfiguration.cacheEvict()
  }

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
