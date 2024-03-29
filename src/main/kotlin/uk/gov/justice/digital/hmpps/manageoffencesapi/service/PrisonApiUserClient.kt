package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.OffenceActivationDto
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.OffenceToScheduleMappingDto

// To be used when passing through the users token - rather than using system credentials
@Service
class PrisonApiUserClient(@Qualifier("prisonApiUserWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
  private val log = LoggerFactory.getLogger(this::class.java)

  fun changeOffenceActiveFlag(offenceActivationDto: OffenceActivationDto) {
    log.info("Making prison-api call to change offence active flag")
    webClient.put()
      .uri("/api/offences/update-active-flag")
      .bodyValue(offenceActivationDto)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun linkToSchedule(offenceToScheduleMappingDtos: List<OffenceToScheduleMappingDto>) {
    log.info("Making prison-api call to link offences to schedules")
    webClient.post()
      .uri("/api/offences/link-to-schedule")
      .bodyValue(offenceToScheduleMappingDtos)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun unlinkFromSchedule(offenceToScheduleMappingDtos: List<OffenceToScheduleMappingDto>) {
    log.info("Making prison-api call to unlink offences from schedules")
    webClient.post()
      .uri("/api/offences/unlink-from-schedule")
      .bodyValue(offenceToScheduleMappingDtos)
      .retrieve()
      .toBodilessEntity()
      .block()
  }
}
