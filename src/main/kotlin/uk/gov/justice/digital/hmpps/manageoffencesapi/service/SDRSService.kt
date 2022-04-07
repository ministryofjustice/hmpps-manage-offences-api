package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
  private fun findAllOffencesByAlphaChar(alphaChar: Char): SDRSResponse {
    // TODO at the moment this only fetches CURRENT offences, change to ALL after some analysis on any Transformation that may be required
    val sdrsRequest = createOffenceRequest(alphaChar = alphaChar)
    return sdrsApiClient.callSDRS(sdrsRequest)
  }

  @Transactional
  fun loadAllOffences() {
    offenceRepository.deleteAll()
    ('A'..'Z').forEach {
      log.info("Starting full load for alphachar {} ", it)
      val sdrsResponse = findAllOffencesByAlphaChar(it)
      if (sdrsResponse.messageStatus.status == "ERRORED") {
        log.error("Request to SDRS API failed for alpha char {} ", it)
        log.error("Response details: {}", sdrsResponse)
      } else {
        log.info(
          "Fetched {} records from SDRS for alphachar {} ",
          sdrsResponse.messageBody.gatewayOperationType.getOffenceResponse!!.offences.size,
          it
        )
        offenceRepository.saveAll(transform(sdrsResponse))
      }
    }
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

  private fun createOffenceRequest(
    offenceCode: String? = null,
    changedDate: LocalDateTime? = null,
    alphaChar: Char? = null
  ) =
    createSDRSRequest(
      GatewayOperationTypeRequest(
        getOffenceRequest = GetOffenceRequest(
          cjsCode = offenceCode,
          alphaChar = alphaChar,
          allOffences = "CURRENT",
          changedDate = changedDate,
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
