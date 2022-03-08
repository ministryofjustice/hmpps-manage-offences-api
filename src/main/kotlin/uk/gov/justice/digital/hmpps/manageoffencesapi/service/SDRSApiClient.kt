package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.SDRSResponse

@Service
class SDRSApiClient(@Qualifier("standingDataReferenceServiceApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  fun getAllOffences(): SDRSResponse {
    //    TODO Replace URL with correct one when known
    return webClient.get()
      .uri("/todo")
      .retrieve()
      .bodyToMono(typeReference<SDRSResponse>())
      .block()!!
  }
}
