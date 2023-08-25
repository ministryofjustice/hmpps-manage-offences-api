package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.EventToRaise
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.EventType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_SDRS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.SUCCESS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetControlTable
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.ControlTableRecord
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GatewayOperationTypeResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetControlTableResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetOffenceResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageBodyResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageStatusResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.EventToRaiseRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.LegacySdrsHoCodeMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceToSyncWithNomisRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class SDRSServiceTest {
  private val offenceRepository = mock<OffenceRepository>()
  private val sdrsLoadResultRepository = mock<SdrsLoadResultRepository>()
  private val sdrsLoadResultHistoryRepository = mock<SdrsLoadResultHistoryRepository>()
  private val offenceScheduleMappingRepository = mock<OffenceScheduleMappingRepository>()
  private val legacySdrsHoCodeMappingRepository = mock<LegacySdrsHoCodeMappingRepository>()
  private val offenceToSyncWithNomisRepository = mock<OffenceToSyncWithNomisRepository>()
  private val eventToRaiseRepository = mock<EventToRaiseRepository>()
  private val adminService = mock<AdminService>()
  private val sdrsApiClient = mock<SDRSApiClient>()

  private val sdrsService = SDRSService(
    sdrsApiClient,
    offenceRepository,
    sdrsLoadResultRepository,
    sdrsLoadResultHistoryRepository,
    offenceScheduleMappingRepository,
    legacySdrsHoCodeMappingRepository,
    offenceToSyncWithNomisRepository,
    eventToRaiseRepository,
    adminService,
  )

  @BeforeEach
  fun setup() {
    SdrsCache.values().forEach { cache ->
      whenever(sdrsLoadResultRepository.findById(cache)).thenReturn(
        Optional.of(
          SdrsLoadResult(
            cache = cache,
            status = SUCCESS,
            lastSuccessfulLoadDate = NOW,
          ),
        ),
      )
    }
    val loadResults = SdrsCache.values().map { cache ->
      SdrsLoadResult(
        cache = cache,
        status = SUCCESS,
        lastSuccessfulLoadDate = NOW,
      )
    }

    whenever(sdrsLoadResultRepository.findAll()).thenReturn(loadResults)
    whenever(adminService.isFeatureEnabled(DELTA_SYNC_SDRS)).thenReturn(true)
    whenever(adminService.isFeatureEnabled(DELTA_SYNC_NOMIS)).thenReturn(true)
  }

  @Test
  fun `Ensure sync with SDRS doesnt occur if associated feature toggle is disabled`() {
    whenever(adminService.isFeatureEnabled(DELTA_SYNC_SDRS)).thenReturn(false)

    sdrsService.deltaSynchroniseWithSdrs()

    verifyNoInteractions(sdrsApiClient)
  }

  @Test
  fun `Ensure delta sync with nomis is called for only the alphaChar affected (there are records to update for A) `() {
    whenever(sdrsApiClient.callSDRS(argThat { messageHeader.messageType == GetControlTable })).thenReturn(
      CONTROL_TABLE_RESPONSE,
    )
    whenever(sdrsApiClient.callSDRS(argThat { messageHeader.messageType == GetOffence })).thenReturn(
      GET_OFFENCE_RESPONSE_NO_OFFENCES,
    )

    sdrsService.deltaSynchroniseWithSdrs()
  }

  @Test
  fun `Ensure children are updated with parent offence id (there are records to update for A) `() {
    whenever(sdrsApiClient.callSDRS(argThat { messageHeader.messageType == GetControlTable })).thenReturn(
      CONTROL_TABLE_RESPONSE,
    )
    whenever(sdrsApiClient.callSDRS(argThat { messageHeader.messageType == GetOffence })).thenReturn(
      GET_OFFENCE_RESPONSE_NO_OFFENCES,
    )
    whenever(offenceRepository.findChildOffencesWithNoParent(SdrsCache.OFFENCES_A)).thenReturn(listOf(OFFENCE_B123AA6A))
    whenever(offenceRepository.findOneByCode(OFFENCE_B123AA6A.parentCode!!)).thenReturn(Optional.of(OFFENCE_B123AA6))

    sdrsService.deltaSynchroniseWithSdrs()

    verify(offenceRepository, times(1)).save(OFFENCE_B123AA6A.copy(parentOffenceId = OFFENCE_B123AA6.id))
  }

  @Test
  fun `Ensure event is published when an offence is updated`() {
    whenever(sdrsApiClient.callSDRS(argThat { messageHeader.messageType == GetControlTable })).thenReturn(
      CONTROL_TABLE_RESPONSE,
    )
    whenever(sdrsApiClient.callSDRS(argThat { messageHeader.messageType == GetOffence })).thenReturn(
      GET_OFFENCE_RESPONSE,
    )

    sdrsService.deltaSynchroniseWithSdrs()

    verify(eventToRaiseRepository).save(
      EventToRaise(
        offenceCode = SDRS_OFFENCE.code,
        eventType = EventType.OFFENCE_CHANGED,
      ),
    )
  }

  companion object {
    private val NOW = LocalDateTime.now()

    private val CONTROL_TABLE_RESPONSE = SDRSResponse(
      messageBody = MessageBodyResponse(
        gatewayOperationType = GatewayOperationTypeResponse(
          getControlTableResponse = GetControlTableResponse(
            referenceDataSet = listOf(ControlTableRecord("offence_A", lastUpdate = NOW)),
          ),
        ),
      ),
      messageStatus = MessageStatusResponse(status = "SUCCESS"),
    )

    private val GET_OFFENCE_RESPONSE_NO_OFFENCES = SDRSResponse(
      messageBody = MessageBodyResponse(
        gatewayOperationType = GatewayOperationTypeResponse(
          getOffenceResponse = GetOffenceResponse(
            offences = emptyList(),
          ),
        ),
      ),
      messageStatus = MessageStatusResponse(status = "SUCCESS"),
    )

    val SDRS_OFFENCE = uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.Offence(
      offenceRevisionId = 1,
      code = "OF1",
      changedDate = LocalDateTime.now(),
      offenceStartDate = LocalDate.now(),
    )
    private val GET_OFFENCE_RESPONSE = SDRSResponse(
      messageBody = MessageBodyResponse(
        gatewayOperationType = GatewayOperationTypeResponse(
          getOffenceResponse = GetOffenceResponse(
            offences = listOf(SDRS_OFFENCE),
          ),
        ),
      ),
      messageStatus = MessageStatusResponse(status = "SUCCESS"),
    )

    private val BASE_OFFENCE = Offence(
      code = "AABB011",
      changedDate = LocalDateTime.now(),
      revisionId = 1,
      startDate = LocalDate.now(),
      sdrsCache = SdrsCache.OFFENCES_A,
    )

    private val OFFENCE_B123AA6 = BASE_OFFENCE.copy(
      id = 991,
      code = "B123AA6",
      description = "B Desc 1 -= parent",
    )

    private val OFFENCE_B123AA6A = BASE_OFFENCE.copy(
      id = 992,
      code = "B123AA6A",
      description = "B Desc 1 - child",
    )
  }
}
