package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffencePcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolent
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.NONE
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.SEXUAL
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.VIOLENT
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PcscMarkers
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
    prisonApiMockServer.stubLinkOffence()
    prisonApiMockServer.stubUnlinkOffence()
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

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data-pcsc.sql",
  )
  fun `Get PCSC indicators for multiple offences by offence codes`() {
    val result = webTestClient.get().uri("/schedule/pcsc-indicators?offenceCodes=AB14001,AB14002")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(OffencePcscMarkers::class.java)
      .returnResult().responseBody

    assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(
        listOf(
          OffencePcscMarkers(
            offenceCode = "AB14001",
            PcscMarkers(
              inListA = true,
              inListB = true,
              inListC = true,
              inListD = true,
            ),
          ),
          OffencePcscMarkers(
            offenceCode = "AB14002",
            PcscMarkers(
              inListA = false,
              inListB = false,
              inListC = false,
              inListD = false,
            ),
          ),
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data-sexual-or-violent.sql",
  )
  fun `Get Sexual or Violent indicators for multiple offences by offence codes`() {
    val result = webTestClient.get().uri("/schedule/sexual-or-violent?offenceCodes=AB14001,AB14002,AB14003,AF06999")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBodyList(OffenceSexualOrViolent::class.java)
      .returnResult().responseBody

    assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(
        listOf(
          OffenceSexualOrViolent(offenceCode = "AB14001", schedulePart = VIOLENT),
          OffenceSexualOrViolent(offenceCode = "AB14002", schedulePart = SEXUAL),
          OffenceSexualOrViolent(offenceCode = "AB14003", schedulePart = SEXUAL),
          OffenceSexualOrViolent(offenceCode = "AF06999", schedulePart = NONE),
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-data-pcsc.sql",
  )
  fun `Unlink a PCSC offence from S15 and associated pcsc schedules`() {
    prisonApiMockServer.stubLinkPcscOffence()
    prisonApiMockServer.stubUnlinkPcscOffenceSchedule15()
    prisonApiMockServer.stubUnlinkPcscOffence()
    val allSchedules = getAllSchedules()
    val schedule15Id = allSchedules!!.filter { it.code == "15" }[0].id
    val scheduleBefore = getScheduleDetails(schedule15Id)

    val firstSchedulePartId = scheduleBefore!!.scheduleParts!!.first { it.partNumber == 1 }.id
    val offenceParent = getOffences("AB14001")!!.first { it.code == "AB14001" }

    unlinkOffencesToSchedulePart(
      listOf(
        SchedulePartIdAndOffenceId(
          schedulePartId = firstSchedulePartId,
          offenceId = offenceParent.id,
        ),
      ),
    )
    val scheduleAfterUnlinkingOffences = getScheduleDetails(schedule15Id)
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
