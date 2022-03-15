package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GatewayOperationTypeRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetOffenceRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageBodyRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageHeader
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageID
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SDRSService(private val sdrsApiClient: SDRSApiClient, private val offenceRepository: OffenceRepository) {
  fun findAllOffences(): SDRSResponse {
    log.info("Fetching all offences from SDRS")
    val sdrsRequest = SDRSRequest(
      messageHeader = MessageHeader(
        messageID = MessageID(
          uuid = UUID.randomUUID(),
          relatesTo = ""
        ),
        timeStamp = ZonedDateTime.now(),
        messageType = "GetOffence",
        from = "MANAGE_OFFENCES",
        to = "SDRS_AZURE"
      ),
      messageBody = MessageBodyRequest(
        GatewayOperationTypeRequest(
          GetOffenceRequest(
            cjsCode = "",
            alphaChar = "X",
            allOffences = "ALL",
          )
        )
      )
    )
    return sdrsApiClient.getAllOffences(sdrsRequest)
  }

  fun findAllOffencesAndSave() {
    val sdrsResponse = findAllOffences()
    transform(sdrsResponse)
    offenceRepository.saveAll(transform(sdrsResponse))
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
