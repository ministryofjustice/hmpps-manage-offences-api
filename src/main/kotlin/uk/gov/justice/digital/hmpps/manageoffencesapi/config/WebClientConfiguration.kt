package uk.gov.justice.digital.hmpps.manageoffencesapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.sdrs}") private val standingDataReferenceServiceApiUrl: String,
  private val webClientBuilder: WebClient.Builder,
) {
  @Bean
  fun standingDataReferenceServiceApiWebClient(): WebClient {
    return webClientBuilder.baseUrl(standingDataReferenceServiceApiUrl)
      .defaultHeaders { headers -> headers.addAll(createHeaders()) }
      .build()
  }

  private fun createHeaders(): MultiValueMap<String, String> {
    val headers = HttpHeaders()
    headers.add(HttpHeaders.CONTENT_TYPE, "application/json")
    headers.add(HttpHeaders.ACCEPT, "application/json")
    return headers
  }
}
