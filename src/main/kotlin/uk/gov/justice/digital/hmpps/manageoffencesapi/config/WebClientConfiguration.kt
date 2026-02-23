package uk.gov.justice.digital.hmpps.manageoffencesapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableScheduling
class WebClientConfiguration(
  @Value("\${api.base.url.sdrs}") private val standingDataReferenceServiceApiUrl: String,
  @Value("\${api.base.url.prison.api}") private val prisonApiUrl: String,
  private val objectMapper: ObjectMapper,
) {

  // ✅ Uses Boot-managed builder → inherits spring.codec.max-in-memory-size from application.yml
  @Bean
  fun standingDataReferenceServiceApiWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl(standingDataReferenceServiceApiUrl)
    .defaultHeaders { headers -> headers.addAll(createHeaders()) }
    .codecs { configurer ->
      // Augment codecs without resetting the already-applied maxInMemorySize
      configurer.defaultCodecs().jackson2JsonEncoder(
        Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON),
      )
      configurer.defaultCodecs().jackson2JsonDecoder(
        Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON),
      )
    }
    .build()

  @Bean
  fun prisonApiWebClient(
    builder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prison-api")
    return builder
      .baseUrl(prisonApiUrl)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun prisonApiUserWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl(prisonApiUrl)
    .filter(addAuthHeaderFilterFunction())
    .build()

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request, next ->
    val filtered = ClientRequest.from(request)
      .header(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
      .build()
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
}
