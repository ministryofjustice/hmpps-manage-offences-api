package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResultHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.FAIL
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.SUCCESS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.FULL_LOAD
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GatewayOperationTypeRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetControlTableRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetOffenceRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageBodyRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageHeader
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageID
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadStatusHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadStatusRepository
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.EntityNotFoundException

@Service
class SDRSService(
  private val sdrsApiClient: SDRSApiClient,
  private val offenceRepository: OffenceRepository,
  private val sdrsLoadStatusRepository: SdrsLoadStatusRepository,
  private val sdrsLoadStatusHistoryRepository: SdrsLoadStatusHistoryRepository,
) {
  private fun findAllOffencesByAlphaChar(alphaChar: Char): SDRSResponse {
    // TODO at the moment this fetches ALL offences, which means multiple revisions of the same offence could be returned
    //  we may need to sanitise so that only the latest version of each offence is stored
    val sdrsRequest = createOffenceRequest(alphaChar = alphaChar)
    return sdrsApiClient.callSDRS(sdrsRequest)
  }

  @Transactional
  fun loadAllOffences() {
    offenceRepository.deleteAll()
    val loadDate = LocalDateTime.now()
    ('A'..'Z').forEach { alphaChar ->
      log.info("Starting full load for alphachar {} ", alphaChar)
      val sdrsResponse = findAllOffencesByAlphaChar(alphaChar)
      if (sdrsResponse.messageStatus.status == "ERRORED") {
        log.error("Request to SDRS API failed for alpha char {} ", alphaChar)
        log.error("Response details: {}", sdrsResponse)
        saveLoad(alphaChar, loadDate, FAIL, FULL_LOAD)
      } else {
        log.info(
          "Fetched {} records from SDRS for alphachar {} ",
          sdrsResponse.messageBody.gatewayOperationType.getOffenceResponse!!.offences.size,
          alphaChar
        )
        offenceRepository.saveAll(transform(sdrsResponse))
        saveLoad(alphaChar, loadDate, SUCCESS, FULL_LOAD)
      }
    }
  }

  private fun saveLoad(alphaChar: Char, loadDate: LocalDateTime?, status: LoadStatus, type: LoadType) {
    val loadStatusExisting = sdrsLoadStatusRepository.findById(alphaChar.toString())
      .orElseThrow { EntityNotFoundException("No record exists for alphaChar $alphaChar") }
    val loadStatus = if (status == SUCCESS) {
      loadStatusExisting.copy(
        status = status,
        loadType = type,
        loadDate = loadDate,
        lastSuccessfulLoadDate = loadDate
      )
    } else {
      loadStatusExisting.copy(
        status = status,
        loadType = type,
        loadDate = loadDate,
      )
    }
    sdrsLoadStatusRepository.save(loadStatus)

    val loadStatusHistory = SdrsLoadResultHistory(
      alphaChar = alphaChar.toString(),
      status = status,
      loadType = type,
      loadDate = loadDate,
    )
    sdrsLoadStatusHistoryRepository.save(loadStatusHistory)
  }

  fun findOffenceByOffenceCode(offenceCode: String): SDRSResponse {
    log.info("Fetching a single offence from SDRS")
    val sdrsRequest = createOffenceRequest(offenceCode = offenceCode, allOffences = "CURRENT")
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
    alphaChar: Char? = null,
    allOffences: String = "ALL"
  ) =
    createSDRSRequest(
      GatewayOperationTypeRequest(
        getOffenceRequest = GetOffenceRequest(
          cjsCode = offenceCode,
          alphaChar = alphaChar,
          allOffences = allOffences,
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
