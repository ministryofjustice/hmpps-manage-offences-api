package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.SUCCESS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.FULL_LOAD
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.ControlTableRecord
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GatewayOperationTypeResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetControlTableResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageBodyResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadStatusHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadStatusRepository
import java.time.LocalDate
import java.time.LocalDateTime

class SDRSServiceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var sdrsService: SDRSService

  @Autowired
  lateinit var offenceRepository: OffenceRepository

  @Autowired
  lateinit var sdrsLoadStatusRepository: SdrsLoadStatusRepository

  @Autowired
  lateinit var sdrsLoadStatusHistoryRepository: SdrsLoadStatusHistoryRepository

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql"
  )
  fun `Save all offences retrieved from SDRS`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForA()
    sdrsService.loadAllOffences()
    val offences = offenceRepository.findAll()
    val statusRecords = sdrsLoadStatusRepository.findAll()
    val statusHistoryRecords = sdrsLoadStatusHistoryRepository.findAll()

    assertThat(offences)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdDate", "lastUpdatedDate")
      .isEqualTo(
        listOf(
          Offence(
            code = "XX99001",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            changedDate = null
          ),
          Offence(
            code = "XX99001",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 354116,
            startDate = LocalDate.of(2005, 9, 2),
            endDate = LocalDate.of(2005, 9, 3),
            changedDate = null
          ),
        )
      )

    assertThat(statusRecords.size).isEqualTo(26)
    statusRecords.forEach {
      assertThat(it.status).isEqualTo(SUCCESS)
      assertThat(it.loadType).isEqualTo(FULL_LOAD)
    }

    assertThat(statusHistoryRecords.size).isEqualTo(26)
    statusHistoryRecords.forEach {
      assertThat(it.status).isEqualTo(SUCCESS)
      assertThat(it.loadType).isEqualTo(FULL_LOAD)
    }
  }

  @Test
  fun `Make request to control table `() {
    sdrsApiMockServer.stubControlTableRequest()
    val sdrsResponse = sdrsService.makeControlTableRequest(LocalDateTime.now())
    assertThat(sdrsResponse.messageBody)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*lastUpdate")
      .isEqualTo(
        MessageBodyResponse(
          GatewayOperationTypeResponse(
            getControlTableResponse = GetControlTableResponse(
              referenceDataSet = listOf(
                ControlTableRecord(
                  dataSet = "offence_A",
                  lastUpdate = LocalDateTime.now()
                ),
                ControlTableRecord(
                  dataSet = "offence_B",
                  lastUpdate = LocalDateTime.now()
                ),
              )
            )
          )
        )
      )
  }
}
