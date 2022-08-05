package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
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
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data.sql"
  )
  fun `Get results of latest load`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForA()
    sdrsService.fullSynchroniseWithSdrs()
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
            loadDate = LocalDateTime.of(2022, 4, 7, 17, 5, 58, 178000000),
            schedules = listOf(
              ScheduleDetails(
                act = "Criminal Justice Act",
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
            schedules = listOf(
              ScheduleDetails(
                act = "Criminal Justice Act",
                code = "15",
                url = "https://www.legislation.gov.uk/ukpga/2003/44/schedule/15",
                schedulePartNumbers = listOf(1)
              )
            )
          )
        )
      )
  }
}
