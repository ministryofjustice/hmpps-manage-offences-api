package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.RestResponsePage
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.HoCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.OffenceToScheduleMappingDto
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Statute

@Service
class PrisonApiClient(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
  private val log = LoggerFactory.getLogger(this::class.java)

  fun findByOffenceCodeStartsWith(offenceCode: String, pageNumber: Int): RestResponsePage<Offence> {
    log.info("Fetching all offences from prison-api for page number $pageNumber")
    return webClient.get()
      .uri("/api/offences/code/$offenceCode?page=$pageNumber&size=1000&sort=code,ASC")
      .retrieve()
      .bodyToMono(typeReference<RestResponsePage<Offence>>())
      .block()!!
  }

  fun createHomeOfficeCodes(prisonApiHoCode: List<HoCode>) {
    log.info("Making prison-api call to create home office offence codes")
    webClient.post()
      .uri("/api/offences/ho-code")
      .bodyValue(prisonApiHoCode)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun createStatutes(prisonApiStatute: List<Statute>) {
    log.info("Making prison-api call to create statutes")
    webClient.post()
      .uri("/api/offences/statute")
      .bodyValue(prisonApiStatute)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun createOffences(offences: List<Offence>) {
    log.info("Making prison-api call to create offences")
    webClient.post()
      .uri("/api/offences/offence")
      .bodyValue(offences)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun updateOffences(updatedNomisOffences: List<Offence>) {
    log.info("Making prison-api call to update offences")
    webClient.put()
      .uri("/api/offences/offence")
      .bodyValue(updatedNomisOffences)
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
