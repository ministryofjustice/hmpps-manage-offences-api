package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.GatewayOperationType
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.GetOffenceResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.MessageBody
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.SDRSResponse
import java.time.LocalDate

class SDRSServiceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var sdrsService: SDRSService

  @Test
  fun `Get all offences`() {
    sdrsApiMockServer.stubGetAllOffences()
    val sdrsResponse = sdrsService.findOffences()
    assertThat(sdrsResponse.messageBody).isEqualTo(
      MessageBody(
        GatewayOperationType(
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
