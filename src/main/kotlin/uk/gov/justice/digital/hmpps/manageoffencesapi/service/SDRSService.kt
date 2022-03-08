package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.SDRSResponse

@Service
class SDRSService(private val sdrsApiClient: SDRSApiClient) {
  fun findOffences(): SDRSResponse {
    log.info("Fetching all offences from SDRS")
    return sdrsApiClient.getAllOffences()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
