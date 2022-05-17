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
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableScheduling
class WebClientConfiguration(
  @Value("\${api.base.url.sdrs}") private val standingDataReferenceServiceApiUrl: String,
  @Value("\${api.base.url.prison.api}") private val prisonApiUrl: String,
  private val webClientBuilder: WebClient.Builder,
  private val webClientBuilderWithAuth: WebClient.Builder,
) {
  @Bean
  fun standingDataReferenceServiceApiWebClient(): WebClient {
    return webClientBuilder.baseUrl(standingDataReferenceServiceApiUrl)
      .defaultHeaders { headers -> headers.addAll(createHeaders()) }
      .build()
  }

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prison-api")
    return webClientBuilderWithAuth
      .baseUrl(prisonApiUrl)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository, oAuth2AuthorizedClientService
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun createHeaders(): MultiValueMap<String, String> {
    val headers = HttpHeaders()
    headers.add(HttpHeaders.CONTENT_TYPE, "application/json")
    headers.add(HttpHeaders.ACCEPT, "application/json")
    return headers
  }
}
