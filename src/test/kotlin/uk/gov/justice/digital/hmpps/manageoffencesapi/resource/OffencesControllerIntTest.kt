package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.SDRSService
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence as ModelOffence

class OffencesControllerIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var sdrsService: SDRSService

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql",
    "classpath:test_data/insert-offence-data.sql"
  )
  fun `Get offences by offence code`() {
    val result = webTestClient.get().uri("/offences/code/aB")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(ModelOffence::class.java)
      .returnResult().responseBody

    assertThat(result)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*dDate")
      .isEqualTo(
        listOf(
          ModelOffence(
            id = 2,
            code = "AB14001",
            description = "Fail to comply with an animal by-product requirement",
            cjsTitle = "Fail to comply with an animal by-product requirement",
            revisionId = 574415,
            startDate = LocalDate.of(2015, 3, 13),
            endDate = null,
            changedDate = LocalDateTime.of(2020, 6, 17, 16, 31, 26),
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000)
          ),
          ModelOffence(
            id = 3,
            code = "AB14002",
            description = "Intentionally obstruct an authorised person",
            cjsTitle = "Intentionally obstruct an authorised person",
            revisionId = 574487,
            startDate = LocalDate.of(2015, 3, 13),
            endDate = null,
            changedDate = LocalDateTime.of(2020, 6, 17, 16, 31, 26),
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000)
          ),
          ModelOffence(
            id = 4,
            code = "AB14003",
            description = "Fail to give to an authorised person information / assistance / provide facilities that person may require",
            cjsTitle = "CJS Title Fail to give to an authorised person information / assistance / provide facilities that person may require",
            revisionId = 574449,
            startDate = LocalDate.of(2015, 3, 13),
            endDate = null,
            changedDate = LocalDateTime.of(2020, 6, 17, 16, 31, 26),
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000)
          )
        )
      )
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql",
    "classpath:test_data/insert-offence-data.sql"
  )
  fun `Get results of latest load`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForA()
    sdrsService.synchroniseWithSdrs()
    val results = webTestClient.get().uri("/offences/load-results")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(MostRecentLoadResult::class.java)
      .returnResult().responseBody

    ('A'..'Z').forEach { alphaChar ->
      val result = results?.find { e -> e.alphaChar == alphaChar.toString() }
      assertThat(result)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*dDate")
        .isEqualTo(
          MostRecentLoadResult(
            alphaChar = alphaChar.toString(),
            status = result!!.status,
            type = result.type,
          )
        )
    }
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql",
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

    webTestClient.post()
      .uri("/offences/full-sync-nomis")
      .headers(setAuthorisation())
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk

    prisonApiMockServer.verify(
      WireMock.postRequestedFor(WireMock.urlEqualTo("/api/offences/offence"))
        .withRequestBody(equalToJson(FULL_SYNC_CREATE_OFFENCES, true, true))
    )
  }

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
