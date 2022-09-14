package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId

class ScheduleControllerIntTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data-with-children.sql"
  )
  fun `Create schedule with 3 parts, link offence that has children, then unlink the offence and check all children are unlinked`() {
    val allSchedules = getAllSchedules()
    val schedule13Id = allSchedules!!.filter { it.code == "13" }[0].id
    val scheduleBefore = getScheduleDetails(schedule13Id)

    assertThatScheduleMatches(scheduleBefore, SCHEDULE)

    val firstSchedulePartId = scheduleBefore!!.scheduleParts!!.first { it.partNumber == 1 }.id
    val secondSchedulePartId = scheduleBefore.scheduleParts!!.first { it.partNumber == 2 }.id
    val offenceParent = getOffences("AF06999")!!.first { it.code == "AF06999" }
    val allAFOffences = getOffences("AF")
    val singleOrphan = getOffences("AG06999C")
    val allAHOffences = getOffences("AH")

    linkOffencesToSchedulePart(firstSchedulePartId, mutableListOf(offenceParent))
    linkOffencesToSchedulePart(firstSchedulePartId, mutableListOf(singleOrphan!!.first()))
    linkOffencesToSchedulePart(secondSchedulePartId, allAHOffences!!)

    val scheduleAfterLinkingOffences = getScheduleDetails(schedule13Id)

    assertThat(allAFOffences).hasSize(4)
    assertThat(allAHOffences).hasSize(2)
    assertThat(singleOrphan).hasSize(1)
    assertThatScheduleMatches(
      scheduleAfterLinkingOffences,
      SCHEDULE.copy(
        scheduleParts = listOf(
          SchedulePart(partNumber = 1, offences = allAFOffences!!.map { it.copy(childOffenceIds = emptyList()) }),
          SchedulePart(partNumber = 2, offences = allAHOffences.map { it.copy(childOffenceIds = emptyList()) }),
        )
      )
    )

    unlinkOffencesToSchedulePart(
      listOf(
        SchedulePartIdAndOffenceId(
          schedulePartId = firstSchedulePartId!!,
          offenceId = offenceParent.id,
        ),
        SchedulePartIdAndOffenceId(
          schedulePartId = secondSchedulePartId!!,
          offenceId = allAHOffences[0].id,
        ),
        SchedulePartIdAndOffenceId(
          schedulePartId = secondSchedulePartId,
          offenceId = allAHOffences[1].id,
        )
      )
    )
    val scheduleAfterUnlinkingOffences = getScheduleDetails(schedule13Id)

    assertThatScheduleMatches(scheduleAfterUnlinkingOffences, SCHEDULE)
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

  private fun linkOffencesToSchedulePart(
    schedulePartId: Long?,
    offences: MutableList<Offence>
  ) = webTestClient.post().uri("/schedule/link-offences/$schedulePartId")
    .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_OFFENCE_SCHEDULES")))
    .bodyValue(offences.map { it.id })
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
      act = "Sentencing Act 2020",
      url = "https://www.legislation.gov.uk/ukpga/2020/17/schedule/13",
      code = "13",
      scheduleParts = listOf(
        SchedulePart(partNumber = 1),
        SchedulePart(partNumber = 2),
      )
    )
  }
}
