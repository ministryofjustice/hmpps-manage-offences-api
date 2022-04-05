package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GatewayOperationTypeRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetControlTableRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetOffenceRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageBodyRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageHeader
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageID
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SDRSService(private val sdrsApiClient: SDRSApiClient, private val offenceRepository: OffenceRepository) {
  fun findAllCurrentOffences(): SDRSResponse {
    log.info("Fetching all offences from SDRS")
    val sdrsRequest = createOffenceRequest("")
    return sdrsApiClient.callSDRS(sdrsRequest)
  }

  fun findAllOffencesAndSave() {
    val sdrsResponse = findAllCurrentOffences()
    transform(sdrsResponse)
    offenceRepository.saveAll(transform(sdrsResponse))
  }

  fun findOffenceByOffenceCode(offenceCode: String): SDRSResponse {
    log.info("Fetching a single offence from SDRS")
    val sdrsRequest = createOffenceRequest(offenceCode)
    return sdrsApiClient.callSDRS(sdrsRequest)
  }

  fun makeControlTableRequest(changedDateTime: LocalDateTime): SDRSResponse {
    log.info("Making a control table request from SDRS")
    val sdrsRequest = createControlTableRequest(changedDateTime)
    return sdrsApiClient.callSDRS(sdrsRequest)
  }

  private fun createSDRSRequest(gatewayOperationTypeRequest: GatewayOperationTypeRequest, messageType: String) =
    SDRSRequest(
      messageHeader = MessageHeader(
        messageID = MessageID(
          uuid = UUID.randomUUID(),
          relatesTo = ""
        ),
        timeStamp = ZonedDateTime.now(),
        messageType = messageType,
        from = "CONSUMER_APPLICATION",
        to = "SDRS_AZURE"
      ),
      messageBody = MessageBodyRequest(
        gatewayOperationTypeRequest
      )
    )

  private fun createOffenceRequest(offenceCode: String) =
    createSDRSRequest(
      GatewayOperationTypeRequest(
        getOffenceRequest = GetOffenceRequest(
          cjsCode = offenceCode,
          alphaChar = "",
          allOffences = "CURRENT",
        )
      ),
      "GetOffence"
    )

  private fun createControlTableRequest(changedDateTime: LocalDateTime) =
    createSDRSRequest(
      GatewayOperationTypeRequest(
        getControlTableRequest = GetControlTableRequest(
          changedDateTime = changedDateTime
        )
      ),
      "GetControlTable"
    )

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
