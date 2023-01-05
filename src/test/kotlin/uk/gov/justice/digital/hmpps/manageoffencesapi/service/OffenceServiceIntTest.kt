package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase

class OffenceServiceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var offenceService: OffenceService

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data.sql",
    "classpath:test_data/insert-offence-data-that-exists-in-nomis.sql"
  )
  fun `Fully sync with NOMIS - includes creating statute and ho-code`() {
    ('A'..'Z').forEach { alphaChar ->
      prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
    }
    prisonApiMockServer.stubFindByOffenceCodeStartsWith('M')
    prisonApiMockServer.stubCreateHomeOfficeCode()
    prisonApiMockServer.stubCreateStatute()
    prisonApiMockServer.stubCreateOffence()
    prisonApiMockServer.stubUpdateOffence()

    offenceService.fullSyncWithNomis()

    verifyPostOffenceToPrisonApi(FULL_SYNC_CREATE_OFFENCES)
  }

  @Test
  @Sql("classpath:test_data/reset-all-data.sql")
  fun `Get ho-code for offence that doesnt exist returns null`() {
    val res = offenceService.findHoCodeByOffenceCode("NOT_EXIST")

    assertThat(res).isNull()
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data.sql",
  )
  fun `Get ho-code for offence that has empty ho_code`() {
    val res = offenceService.findHoCodeByOffenceCode("AF06999")

    assertThat(res).isNull()
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data-with-ho-code.sql",
  )
  fun `Get ho-code for an offence code`() {
    val res = offenceService.findHoCodeByOffenceCode("ho06999")

    assertThat(res).isEqualTo("001/13")
  }

  private fun verifyPostOffenceToPrisonApi(json: String) =
    prisonApiMockServer.verify(
      WireMock.postRequestedFor(WireMock.urlEqualTo("/api/offences/offence"))
        .withRequestBody(WireMock.equalToJson(json, true, true))
    )

  companion object {
    private val FULL_SYNC_CREATE_OFFENCES = """
      [
       {
          "code" : "AF06999",
          "description" : "Brought before the court as being absent without leave from the Armed Forces",
          "statuteCode" : {
            "code" : "AF06",
            "description" : "Contrary to section 19 of the Zoo Licensing Act 1981",
            "legislatingBodyCode" : "UK",
            "activeFlag" : "Y"
            },
          "hoCode" : null,
          "severityRanking" : "99",
          "activeFlag" : "Y",
          "listSequence" : null,
          "expiryDate" : null
        },
        {
          "code" : "AB14001",
          "description" : "Fail to comply with an animal by-product requirement",
          "statuteCode" : {
            "code" : "AB14",
            "description" : "AB14",
            "legislatingBodyCode" : "UK",
            "activeFlag" : "Y"
          },
          "hoCode" : null,
          "severityRanking" : "99",
          "activeFlag" : "Y",
          "listSequence" : null,
          "expiryDate" : null
        },
        {
          "code" : "AB14002",
          "description" : "Intentionally obstruct an authorised person",
          "statuteCode" : {
            "code" : "AB14",
            "description" : "AB14",
            "legislatingBodyCode" : "UK",
            "activeFlag" : "Y"
          },
          "hoCode" : null,
          "severityRanking" : "99",
          "activeFlag" : "Y",
          "listSequence" : null,
          "expiryDate" : null
        },
        {
          "code" : "AB14003",
          "description" : "CJS Title Fail to give to an authorised person information / assistance / provide facilities that person may require",
          "statuteCode" : {
            "code" : "AB14",
            "description" : "AB14",
            "legislatingBodyCode" : "UK",
            "activeFlag" : "Y"
          },
          "hoCode" : null,
          "severityRanking" : "99",
          "activeFlag" : "Y",
          "listSequence" : null,
          "expiryDate" : null
        } 
    ] 
    """.trimIndent()
  }
}
