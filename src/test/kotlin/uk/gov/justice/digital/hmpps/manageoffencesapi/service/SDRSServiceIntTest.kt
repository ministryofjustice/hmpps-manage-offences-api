package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.ControlTableRecord
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GatewayOperationTypeResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetControlTableResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetOffenceResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageBodyResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.Offence as SDRSOffence

class SDRSServiceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var sdrsService: SDRSService

  @Autowired
  lateinit var offenceRepository: OffenceRepository

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql"
  )
  fun `Get all offences`() {
    sdrsApiMockServer.stubGetAllOffences()
    val sdrsResponse = sdrsService.findAllCurrentOffences()
    assertThat(sdrsResponse.messageBody).isEqualTo(
      MessageBodyResponse(
        GatewayOperationTypeResponse(
          GetOffenceResponse(
            listOf(
              SDRSOffence(
                offenceRevisionId = 410082,
                offenceStartDate = LocalDate.of(2013, 3, 1),
                offenceEndDate = LocalDate.of(2013, 3, 2),
                description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
                code = "XX99001"
              ),
              SDRSOffence(
                offenceRevisionId = 354116,
                offenceStartDate = LocalDate.of(2005, 9, 2),
                offenceEndDate = LocalDate.of(2005, 9, 3),
                description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
                code = "XX99001"
              ),
            )
          )
        )
      )
    )
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql"
  )
  fun `Save all offences retrieved from SDRS`() {
    sdrsApiMockServer.stubGetAllOffences()
    sdrsService.findAllOffencesAndSave()
    val offences = offenceRepository.findAll()
    assertThat(offences).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id").isEqualTo(
      listOf(
        Offence(
          code = "XX99001",
          description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE"
        ),
        Offence(
          code = "XX99001",
          description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE"
        ),
      )
    )
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
