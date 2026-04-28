package uk.gov.justice.digital.hmpps.manageoffencesapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableScheduling
class WebClientConfiguration(
  @Value("\${api.base.url.sdrs}") private val standingDataReferenceServiceApiUrl: String,
  @Value("\${api.base.url.prison.api}") private val prisonApiUrl: String,
) {
  @Bean
  fun standingDataReferenceServiceApiWebClient(): WebClient = WebClient.builder()
    .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES) }
    .baseUrl(standingDataReferenceServiceApiUrl)
    .defaultHeaders { headers -> headers.addAll(createHeaders()) }
    .build()

  @Bean
  fun prisonApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prison-api")
    return WebClient.builder()
      .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES) }
      .baseUrl(prisonApiUrl)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun prisonApiUserWebClient(): WebClient = WebClient.builder()
    .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES) }
    .baseUrl(prisonApiUrl)
    .filter(addAuthHeaderFilterFunction())
    .build()

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request, next ->
    val servletRequest = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
    val authHeader = servletRequest?.getHeader(HttpHeaders.AUTHORIZATION)

    val filtered = ClientRequest.from(request).apply {
      if (!authHeader.isNullOrBlank()) {
        header(HttpHeaders.AUTHORIZATION, authHeader)
      }
    }.build()

    next.exchange(filtered)
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun createHeaders(): HttpHeaders = HttpHeaders().apply {
    add(HttpHeaders.CONTENT_TYPE, "application/json")
    add(HttpHeaders.ACCEPT, "application/json")
  }

  private companion object {
    private const val MAX_IN_MEMORY_SIZE_BYTES = 50 * 1024 * 1024
  }
}
