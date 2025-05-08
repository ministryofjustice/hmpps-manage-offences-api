package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.github.tomakehurst.wiremock.client.WireMock
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase

class OffenceServiceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var offenceService: OffenceService

  @Nested
  inner class FullSyncTests {
    @Test
    @Sql(
      "classpath:test_data/reset-all-data.sql",
      "classpath:test_data/insert-offence-data.sql",
      "classpath:test_data/insert-offence-data-that-exists-in-nomis.sql",
    )
    fun `Fully sync with NOMIS - includes creating statute and ho-code`() {
      ('A'..'Z').forEach { alphaChar ->
        prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
      }
      prisonApiMockServer.stubFindByOffenceCodeStartsWith("M")
      prisonApiMockServer.stubCreateHomeOfficeCode()
      prisonApiMockServer.stubCreateStatute()
      prisonApiMockServer.stubCreateOffence()
      prisonApiMockServer.stubUpdateOffence()

      offenceService.fullSyncWithNomis()

      verifyPostOffenceToPrisonApi(FULL_SYNC_CREATE_OFFENCES)
    }

    @Test
    @Sql(
      "classpath:test_data/reset-all-data.sql",
      "classpath:test_data/insert-offence-data-with-spaces.sql",
      "classpath:test_data/insert-offence-data-that-exists-in-nomis.sql",
    )
    fun `Fully sync with NOMIS when offence includes leading and trailing spaces`() {
      ('A'..'Z').forEach { alphaChar ->
        prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
      }
      prisonApiMockServer.stubFindByOffenceCodeStartsWith("M")
      prisonApiMockServer.stubCreateHomeOfficeCode()
      prisonApiMockServer.stubCreateStatute()
      prisonApiMockServer.stubCreateOffence()
      prisonApiMockServer.stubUpdateOffence()

      offenceService.fullSyncWithNomis()

      verifyPostOffenceToPrisonApi(FULL_SYNC_CREATE_OFFENCES)
    }

    @Test
    @Sql(
      "classpath:test_data/reset-all-data.sql",
      "classpath:test_data/insert-offence-data.sql",
      "classpath:test_data/insert-offence-data-that-is-reactivated-nomis.sql",
    )
    fun `Sync with NOMIS when an offence has been reactivated in NOMIS`() {
      ('A'..'Z').forEach { alphaChar ->
        prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
      }

      prisonApiMockServer.stubFindByOffenceCode("M")
      prisonApiMockServer.stubCreateHomeOfficeCode()
      prisonApiMockServer.stubCreateStatute()
      prisonApiMockServer.stubCreateOffence()
      prisonApiMockServer.stubUpdateOffence()

      offenceService.fullSyncWithNomis()

      verifyPutOffenceToPrisonApi(M1119999_OFFENCE)
    }

    @Nested
    inner class DeltaSyncTests {
      @Test
      @Sql(
        "classpath:test_data/reset-all-data.sql",
        "classpath:test_data/enable-delta-sync-nomis.sql",
        "classpath:test_data/insert-offence-data-that-exists-in-nomis.sql",
        "classpath:test_data/insert-ho-update-to-sync-with-nomis.sql",
      )
      fun `When there are offences to push from non-sdrs sources (ho code update) they get pushed`() {
        ('A'..'Z').forEach { alphaChar ->
          prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
        }
        prisonApiMockServer.stubFindByOffenceCodeStartsWith("M11")
        prisonApiMockServer.stubCreateHomeOfficeCode()
        prisonApiMockServer.stubCreateStatute()
        prisonApiMockServer.stubCreateOffence()
        prisonApiMockServer.stubUpdateOffence()

        offenceService.deltaSyncWithNomis()

        verifyPutOffenceToPrisonApi(M1119999_OFFENCE)
      }
    }

    @Nested
    inner class OtherTests {
      @Test
      @Sql("classpath:test_data/reset-all-data.sql")
      fun `Get ho-code for offence that doesnt exist throws 404`() {
        Assertions.assertThatThrownBy {
          offenceService.findHoCodeByOffenceCode("NOT_EXIST")
        }
          .isInstanceOf(EntityNotFoundException::class.java)
          .hasMessage("No offence exists for the passed in offence code")
      }
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
  }

  private fun verifyPutOffenceToPrisonApi(json: String) = prisonApiMockServer.verify(
    WireMock.putRequestedFor(WireMock.urlEqualTo("/api/offences/offence"))
      .withRequestBody(WireMock.equalToJson(json, true, true)),
  )

  private fun verifyPostOffenceToPrisonApi(json: String) = prisonApiMockServer.verify(
    WireMock.postRequestedFor(WireMock.urlEqualTo("/api/offences/offence"))
      .withRequestBody(WireMock.equalToJson(json, true, true)),
  )

  companion object {

    private val M1119999_OFFENCE = """
      [
       {
          "code" : "M1119999",
          "description" : "Actual bodily harm UPDATED",
          "statuteCode" : {
            "code" : "M111",
            "description" : "Statute M111",
            "legislatingBodyCode" : "UK",
            "activeFlag" : "Y"
            },
          "hoCode" : {
            "code" : "091/81",
            "description" : "091/81",
            "activeFlag" : "Y",
            "expiryDate" : null
          },
          "severityRanking" : "91",
          "activeFlag" : "Y",
          "listSequence" : null,
          "expiryDate" : null
        }
    ] 
    """.trimIndent()

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
