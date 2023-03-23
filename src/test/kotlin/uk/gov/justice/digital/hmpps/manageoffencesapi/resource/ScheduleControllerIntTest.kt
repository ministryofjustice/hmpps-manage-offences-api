package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId

class ScheduleControllerIntTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data-with-children.sql",
  )
  fun `Link an offence that has children to a schedule, then unlink the offence and check all children are unlinked`() {
    val allSchedules = getAllSchedules()
    val schedule13Id = allSchedules!!.filter { it.code == "13" }[0].id
    val scheduleBefore = getScheduleDetails(schedule13Id)

    assertThatScheduleMatches(scheduleBefore, SCHEDULE)

    val firstSchedulePartId = scheduleBefore!!.scheduleParts!!.first { it.partNumber == 1 }.id
    val offenceParent = getOffences("AF06999")!!.first { it.code == "AF06999" }

    linkOffencesToSchedulePart(LinkOffence(offenceId = offenceParent.id, schedulePartId = firstSchedulePartId))

    val scheduleAfterLinkingOffences = getScheduleDetails(schedule13Id)
    val offences = scheduleAfterLinkingOffences?.scheduleParts?.first { it.partNumber == 1 }?.offences
    assertThat(offences).hasSize(4)

    val linkedOffenceCodes = offences?.map { it.code }
    assertThat(linkedOffenceCodes).isEqualTo(listOf("AF06999", "AF06999A", "AF06999B", "AF06999C"))
    unlinkOffencesToSchedulePart(
      listOf(
        SchedulePartIdAndOffenceId(
          schedulePartId = firstSchedulePartId,
          offenceId = offenceParent.id,
        ),
      ),
    )
    val scheduleAfterUnlinkingOffences = getScheduleDetails(schedule13Id)
    assertThat(scheduleAfterUnlinkingOffences?.scheduleParts?.first { it.partNumber == 1 }?.offences).isNull()
  }

  private fun assertThatScheduleMatches(scheduleBefore: Schedule?, schedule: Schedule) {
    assertThat(scheduleBefore)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*id")
      .isEqualTo(schedule)
  }

  private fun getScheduleDetails(createdScheduleId: Long?) =
    webTestClient.get().uri("/schedule/by-id/$createdScheduleId")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody(Schedule::class.java)
      .returnResult().responseBody

  private fun getAllSchedules() = webTestClient.get().uri("/schedule/all")
    .headers(setAuthorisation())
    .exchange()
    .expectStatus().isOk
    .expectBodyList(Schedule::class.java)
    .returnResult().responseBody

  private fun linkOffencesToSchedulePart(linkOffence: LinkOffence) = webTestClient.post().uri("/schedule/link-offence")
    .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_OFFENCE_SCHEDULES")))
    .bodyValue(linkOffence)
    .exchange()
    .expectStatus().isOk

  private fun unlinkOffencesToSchedulePart(schedulePartIdAndOffenceIds: List<SchedulePartIdAndOffenceId>) =
    webTestClient.post().uri("/schedule/unlink-offences")
      .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_OFFENCE_SCHEDULES")))
      .bodyValue(schedulePartIdAndOffenceIds)
      .exchange()
      .expectStatus().isOk

  private fun getOffences(offenceCode: String) = webTestClient.get().uri("/offences/code/$offenceCode")
    .headers(setAuthorisation())
    .exchange()
    .expectStatus().isOk
    .expectBodyList(Offence::class.java)
    .returnResult().responseBody

  companion object {
    private val SCHEDULE = Schedule(
      id = 1,
      act = "Sentencing Act 2020",
      url = "https://www.legislation.gov.uk/ukpga/2020/17/schedule/13",
      code = "13",
      scheduleParts = listOf(
        SchedulePart(id = 1, partNumber = 1),
        SchedulePart(id = 2, partNumber = 2),
      ),
    )
  }
}
