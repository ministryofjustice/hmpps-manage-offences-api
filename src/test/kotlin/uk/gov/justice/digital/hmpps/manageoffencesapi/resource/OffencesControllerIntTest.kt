package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ScheduleDetails
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.SDRSService
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence as ModelOffence

class OffencesControllerIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var sdrsService: SDRSService

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
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
      .ignoringFieldsMatchingRegexes("id")
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
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000),
            offenceType = "CI",
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
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data.sql"
  )
  fun `Get results of latest load`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForA()
    sdrsService.fullSynchroniseWithSdrs()
    val results = webTestClient.get().uri("/offences/load-results")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(MostRecentLoadResult::class.java)
      .returnResult().responseBody

    SdrsCache.values().forEach { cache ->
      val result = results?.find { e -> e.sdrsCache == cache }
      assertThat(result)
        .usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes(".*dDate")
        .isEqualTo(
          MostRecentLoadResult(
            sdrsCache = cache,
            status = result!!.status,
            type = result.type,
          )
        )
    }
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-schedule-and-offence-data.sql"
  )
  fun `Get offences with attached schedules`() {
    val result = webTestClient.get().uri("/offences/code/XX")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(ModelOffence::class.java)
      .returnResult().responseBody

    assertThat(result)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*dDate")
      .ignoringFieldsMatchingRegexes(".*id")
      .ignoringFieldsMatchingRegexes(".*schedules")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ModelOffence(
            id = 1,
            code = "XX99001",
            description = "Fail to give to an authorised person information / assistance / provide facilities that person may require",
            cjsTitle = "CJS Title Fail to give to an authorised person information / assistance / provide facilities that person may require",
            revisionId = 574449,
            startDate = LocalDate.of(2015, 3, 13),
            endDate = null,
            changedDate = LocalDateTime.of(2020, 6, 17, 16, 31, 26),
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000)
          ),
          ModelOffence(
            id = 1,
            code = "XX99002",
            description = "2Fail to give to an authorised person information / assistance / provide facilities that person may require",
            cjsTitle = "CJS Title 2Fail to give to an authorised person information / assistance / provide facilities that person may require",
            revisionId = 574449,
            startDate = LocalDate.of(2015, 3, 13),
            endDate = null,
            changedDate = LocalDateTime.of(2020, 6, 17, 16, 31, 26),
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000),
          )
        )
      )

    assertThat(result!!.first { it.code == "XX99001" }!!.schedules)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*id")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ScheduleDetails(
            act = "Criminal Justice Act 2003",
            code = "15",
            url = "https://www.legislation.gov.uk/ukpga/2003/44/schedule/15",
            schedulePartNumbers = listOf(1, 2)
          ),
          ScheduleDetails(
            act = "Sentencing Act 2020",
            code = "13",
            url = "https://www.legislation.gov.uk/ukpga/2020/17/schedule/13",
            schedulePartNumbers = listOf(1)
          )
        )
      )

    assertThat(result.first { it.code == "XX99002" }!!.schedules)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*id")
      .isEqualTo(
        listOf(
          ScheduleDetails(
            act = "Criminal Justice Act 2003",
            code = "15",
            url = "https://www.legislation.gov.uk/ukpga/2003/44/schedule/15",
            schedulePartNumbers = listOf(1)
          )
        )
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data-with-children.sql",
  )
  fun `Get offences by offence code where offence has associated children`() {
    val result = webTestClient.get().uri("/offences/code/AF")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(ModelOffence::class.java)
      .returnResult().responseBody

    val parentOffenceId = result!!.first { it.code == "AF06999" }.id
    val childOffenceIds = result.filter { it.code != "AF06999" }.map { it.id }

    assertThat(result)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*dDate")
      .ignoringFieldsMatchingRegexes("id")
      .isEqualTo(
        listOf(
          ModelOffence(
            id = 1,
            code = "AF06999",
            description = "Brought before the court as being absent without leave from the Armed Forces",
            cjsTitle = "Brought before the court as being absent without leave from the Armed Forces",
            revisionId = 570173,
            startDate = LocalDate.of(2009, 11, 2),
            endDate = null,
            changedDate = LocalDateTime.of(2020, 6, 17, 16, 31, 26),
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000),
            childOffenceIds = childOffenceIds,
          ),
          ModelOffence(
            id = 2,
            code = "AF06999A",
            description = "Inchoate A",
            cjsTitle = "Inchoate A",
            revisionId = 570173,
            startDate = LocalDate.of(2009, 11, 2),
            endDate = null,
            changedDate = LocalDateTime.of(2020, 6, 17, 16, 31, 26),
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000),
            parentOffenceId = parentOffenceId,
            isChild = true,
          ),
          ModelOffence(
            id = 3,
            code = "AF06999B",
            description = "Inchoate B",
            cjsTitle = "Inchoate B",
            revisionId = 570173,
            startDate = LocalDate.of(2009, 11, 2),
            endDate = null,
            changedDate = LocalDateTime.of(2020, 6, 17, 16, 31, 26),
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000),
            parentOffenceId = parentOffenceId,
            isChild = true,
          ),
          ModelOffence(
            id = 4,
            code = "AF06999C",
            description = "Inchoate C",
            cjsTitle = "Inchoate C",
            revisionId = 570173,
            startDate = LocalDate.of(2009, 11, 2),
            endDate = null,
            changedDate = LocalDateTime.of(2020, 6, 17, 16, 31, 26),
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000),
            parentOffenceId = parentOffenceId,
            isChild = true,
          ),
        )
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data-with-ho-code.sql",
  )
  fun `Get HO Code associated with an offence`() {
    val result = webTestClient.get().uri("/offences/ho-code/HO06999")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody(String::class.java)
      .returnResult().responseBody

    assertThat(result).isEqualTo("001/13")
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
  )
  fun `Get HO Code when offence doesnt exist throws 404`() {
    webTestClient.get().uri("/offences/ho-code/NO_OFFENCE")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isNotFound
  }
}
