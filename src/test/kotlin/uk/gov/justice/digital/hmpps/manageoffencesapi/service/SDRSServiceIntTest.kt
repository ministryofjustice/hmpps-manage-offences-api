package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GatewayOperationTypeResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetOffenceResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageBodyResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.Offence
import java.time.LocalDate

class SDRSServiceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var sdrsService: SDRSService

  @Test
  fun `Get all offences`() {
    sdrsApiMockServer.stubGetAllOffences()
    val sdrsResponse = sdrsService.findAllOffences()
    assertThat(sdrsResponse.messageBody).isEqualTo(
      MessageBodyResponse(
        GatewayOperationTypeResponse(
          GetOffenceResponse(
            listOf(
              Offence(
                offenceRevisionId = 410082,
                offenceStartDate = LocalDate.of(2013, 3, 1),
                offenceEndDate = LocalDate.of(2013, 3, 2),
                code = "XX99001"
              ),
              Offence(
                offenceRevisionId = 354116,
                offenceStartDate = LocalDate.of(2005, 9, 2),
                offenceEndDate = LocalDate.of(2005, 9, 3),
                code = "XX99001"
              ),
            )
          )
        )
      )
    )
  }
}
