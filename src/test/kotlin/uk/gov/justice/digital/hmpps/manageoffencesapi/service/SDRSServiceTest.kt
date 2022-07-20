package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.SYNC_SDRS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.SUCCESS
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.ControlTableRecord
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GatewayOperationTypeResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetControlTableResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetOffenceResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageBodyResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageStatusResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import java.time.LocalDateTime
import java.util.Optional

class SDRSServiceTest {
  private val offenceRepository = mock<OffenceRepository>()
  private val sdrsLoadResultRepository = mock<SdrsLoadResultRepository>()
  private val sdrsLoadResultHistoryRepository = mock<SdrsLoadResultHistoryRepository>()
  private val adminService = mock<AdminService>()
  private val offenceService = mock<OffenceService>()
  private val sdrsApiClient = mock<SDRSApiClient>()

  private val sdrsService = SDRSService(
    sdrsApiClient,
    offenceRepository,
    sdrsLoadResultRepository,
    sdrsLoadResultHistoryRepository,
    offenceService,
    adminService
  )

  @BeforeEach
  fun setup() {
    ('A'..'Z').forEach { alphaChar ->
      whenever(sdrsLoadResultRepository.findById(alphaChar.toString())).thenReturn(
        Optional.of(
          SdrsLoadResult(
            alphaChar = alphaChar.toString(),
            status = SUCCESS,
            lastSuccessfulLoadDate = NOW
          )
        )
      )
    }
    val loadResults = ('A'..'Z').map { alphaChar ->
      SdrsLoadResult(
        alphaChar = alphaChar.toString(),
        status = SUCCESS,
        lastSuccessfulLoadDate = NOW
      )
    }

    whenever(sdrsLoadResultRepository.findAll()).thenReturn(loadResults)
    whenever(adminService.isFeatureEnabled(SYNC_SDRS)).thenReturn(true)
    whenever(adminService.isFeatureEnabled(DELTA_SYNC_NOMIS)).thenReturn(true)
  }

  @Test
  fun `Ensure sync with SDRS doesnt occur if associated feature toggle is disabled`() {
    whenever(adminService.isFeatureEnabled(SYNC_SDRS)).thenReturn(false)

    sdrsService.synchroniseWithSdrs()

    verifyNoInteractions(sdrsApiClient)
  }

  @Test
  fun `Ensure delta sync with nomis is called for only the alphaChar affected (there are records to update for A) `() {
    whenever(sdrsApiClient.callSDRS(argThat { messageHeader.messageType == "GetControlTable" })).thenReturn(CONTROL_TABLE_RESPONSE)
    whenever(sdrsApiClient.callSDRS(argThat { messageHeader.messageType == "GetOffence" })).thenReturn(GET_OFFENCE_RESPONSE)

    sdrsService.synchroniseWithSdrs()

    verify(offenceService, times(1)).fullySyncOffenceGroupWithNomis("A")
  }

  @Test
  fun `Ensure delta sync with nomis doesnt occur if associated feature toggle is disabled`() {
    whenever(sdrsApiClient.callSDRS(any())).thenReturn(CONTROL_TABLE_RESPONSE)
    whenever(adminService.isFeatureEnabled(DELTA_SYNC_NOMIS)).thenReturn(false)

    sdrsService.synchroniseWithSdrs()

    verifyNoInteractions(offenceService)
  }

  companion object {
    private val NOW = LocalDateTime.now()

    private val CONTROL_TABLE_RESPONSE = SDRSResponse(
      messageBody = MessageBodyResponse(
        gatewayOperationType = GatewayOperationTypeResponse(
          getControlTableResponse = GetControlTableResponse(
            referenceDataSet = listOf(ControlTableRecord("offence_A", lastUpdate = NOW))
          )
        ),
      ),
      messageStatus = MessageStatusResponse(status = "SUCCESS")
    )

    private val GET_OFFENCE_RESPONSE = SDRSResponse(
      messageBody = MessageBodyResponse(
        gatewayOperationType = GatewayOperationTypeResponse(
          getOffenceResponse = GetOffenceResponse(
            offences = emptyList()
          )
        ),
      ),
      messageStatus = MessageStatusResponse(status = "SUCCESS")
    )
  }
}
