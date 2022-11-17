package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.INSERT
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.UPDATE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisChangeType.HOME_OFFICE_CODE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisChangeType.OFFENCE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisChangeType.STATUTE
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.NomisChangeHistory
import java.time.LocalDateTime

class ChangeHistoryControllerIntTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/insert-nomis-change-history.sql"
  )
  fun `Get offences by offence code`() {
    val result = webTestClient.get().uri("/change-history/nomis?from=2022-11-16&to=2022-11-16")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(NomisChangeHistory::class.java)
      .returnResult().responseBody

    assertThat(result)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes("sentToNomisDate")
      .ignoringFieldsMatchingRegexes("id")
      .isEqualTo(
        listOf(
          NomisChangeHistory(
            id = -1,
            code = "BL21014",
            description = "Master of vessel navigated whilst unfit through drink or drugs - Windermere Navigation Byelaws 2008",
            changeType = INSERT,
            nomisChangeType = OFFENCE,
            sentToNomisDate = LocalDateTime.of(2022, 11, 16, 10, 46, 6)
          ),
          NomisChangeHistory(
            id = -1,
            code = "BL22",
            description = "Contrary to byelaws 63(1), 65 and 69 of the Tyne Tunnel Byelaws 2021.",
            changeType = INSERT,
            nomisChangeType = STATUTE,
            sentToNomisDate = LocalDateTime.of(2022, 11, 16, 10, 46, 5)
          ),
          NomisChangeHistory(
            id = -1,
            code = "AP04003",
            description = "Knowingly furnish false/misleading info to person acting in execution of these Reg's",
            changeType = UPDATE,
            nomisChangeType = OFFENCE,
            sentToNomisDate = LocalDateTime.of(2022, 11, 16, 10, 46, 4)
          ),
          NomisChangeHistory(
            id = -1,
            code = "097/01",
            description = "097/01",
            changeType = INSERT,
            nomisChangeType = HOME_OFFICE_CODE,
            sentToNomisDate = LocalDateTime.of(2022, 11, 16, 0, 0, 1)
          ),
        )
      )
  }
}
