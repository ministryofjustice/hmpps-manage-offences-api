package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSResponse

@Service
class SDRSApiClient(@Qualifier("standingDataReferenceServiceApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  // This one endpoint in the SDRS API serves all variations of offence lookup
  fun callSDRS(sdrsRequest: SDRSRequest): SDRSResponse = webClient.post()
    .uri("/cld_StandingDataReferenceService/service/sdrs/sdrs/sdrsApi")
    .bodyValue(sdrsRequest)
    .retrieve()
    .bodyToMono(typeReference<SDRSResponse>())
    .block()!!
}
