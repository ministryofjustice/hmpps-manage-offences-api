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
    "classpath:test_data/insert-offence-data.sql"
  )
  fun `Create schedule with 3 parts, link offences then unlink the offences`() {
    createSchedule()
    val allSchedules = getAllSchedules()
    assertThat(allSchedules)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes("id")
      .isEqualTo(
        listOf(
          SCHEDULE.copy(scheduleParts = null)
        )
      )

    val createdScheduleId = allSchedules!![0].id
    val scheduleBefore = getScheduleDetails(createdScheduleId)

    assertThatScheduleMatches(scheduleBefore, SCHEDULE)

    val firstSchedulePartId = scheduleBefore!!.scheduleParts!![0].id
    val secondSchedulePartId = scheduleBefore.scheduleParts!![1].id
    val offencesAB = getOffences("AB")
    val offencesAF = getOffences("AF")

    linkOffencesToSchedulePart(firstSchedulePartId, offencesAB!!)
    linkOffencesToSchedulePart(secondSchedulePartId, offencesAF!!)

    val scheduleAfterLinkingOffences = getScheduleDetails(createdScheduleId)

    assertThatScheduleMatches(scheduleAfterLinkingOffences, SCHEDULE.copy(
      scheduleParts = listOf(
        SchedulePart(partNumber = 1, offences = offencesAB),
        SchedulePart(partNumber = 2, offences = offencesAF),
        SchedulePart(partNumber = 3),
      )
    ))

    val linkedScheduleAndOffenceIds = mutableListOf<SchedulePartIdAndOffenceId>()
    extract(scheduleAfterLinkingOffences!!.scheduleParts?.get(0)!!, linkedScheduleAndOffenceIds)
    extract(scheduleAfterLinkingOffences.scheduleParts?.get(1)!!, linkedScheduleAndOffenceIds)
    unlinkOffencesToSchedulePart(linkedScheduleAndOffenceIds)
    val scheduleAfterUnlinkingOffences = getScheduleDetails(createdScheduleId)

    assertThatScheduleMatches(scheduleAfterUnlinkingOffences, SCHEDULE)
  }

  private fun assertThatScheduleMatches(scheduleBefore: Schedule?, schedule: Schedule) {
    assertThat(scheduleBefore)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*id")
      .isEqualTo(schedule)
  }

  private fun extract(
    part: SchedulePart,
    schedulePartIdAndOffenceIds: MutableList<SchedulePartIdAndOffenceId>
  ) {
    part.offences?.forEach {
      schedulePartIdAndOffenceIds.add(
        SchedulePartIdAndOffenceId(
          schedulePartId = part.id!!,
          offenceId = it.id
        )
      )
    }
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

  private fun createSchedule() =
    webTestClient.post().uri("/schedule/create")
      .headers(setAuthorisation())
      .bodyValue(SCHEDULE)
      .exchange()
      .expectStatus().isOk

  private fun linkOffencesToSchedulePart(
    schedulePartId: Long?,
    offences: MutableList<Offence>
  ) = webTestClient.post().uri("/schedule/link-offences/$schedulePartId")
    .headers(setAuthorisation())
    .bodyValue(offences.map { it.id })
    .exchange()
    .expectStatus().isOk

  private fun unlinkOffencesToSchedulePart(schedulePartIdAndOffenceIds: List<SchedulePartIdAndOffenceId>) =
    webTestClient.post().uri("/schedule/unlink-offences")
      .headers(setAuthorisation())
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
      act = "Act 1",
      url = "http://legisltaion",
      code = "18Z",
      scheduleParts = listOf(
        SchedulePart(partNumber = 1),
        SchedulePart(partNumber = 2),
        SchedulePart(partNumber = 3),
      )
    )
  }
}
