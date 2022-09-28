package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceSchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceToScheduleHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.DELETE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.INSERT
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceSchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceToScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import java.util.Optional

class ScheduleServiceTest {
  private val scheduleRepository = mock<ScheduleRepository>()
  private val schedulePartRepository = mock<SchedulePartRepository>()
  private val offenceSchedulePartRepository = mock<OffenceSchedulePartRepository>()
  private val offenceRepository = mock<OffenceRepository>()
  private val offenceToScheduleHistoryRepository = mock<OffenceToScheduleHistoryRepository>()

  private val scheduleService =
    ScheduleService(
      scheduleRepository,
      schedulePartRepository,
      offenceSchedulePartRepository,
      offenceRepository,
      offenceToScheduleHistoryRepository,
    )

  private inline fun <reified T : Any> argumentCaptor(): ArgumentCaptor<T> = ArgumentCaptor.forClass(T::class.java)

  @Test
  fun `Link offences links passed in offences and any associated children`() {
    whenever(schedulePartRepository.findById(SCHEDULE_PART_1.id)).thenReturn(Optional.of(SCHEDULE_PART_1))
    whenever(
      offenceRepository.findByParentOffenceIdIn(
        setOf(
          OFFENCE_B123AA6.id,
          OFFENCE_A123AA6.id
        )
      )
    ).thenReturn(listOf(OFFENCE_B123AA6A_CHILD))
    whenever(offenceRepository.findAllById(setOf(OFFENCE_B123AA6.id, OFFENCE_A123AA6.id))).thenReturn(
      listOf(
        OFFENCE_B123AA6,
        OFFENCE_A123AA6
      )
    )

    scheduleService.linkOffences(SCHEDULE_PART_1.id, setOf(OFFENCE_B123AA6.id, OFFENCE_A123AA6.id))

    val captor = argumentCaptor<List<OffenceSchedulePart>>()

    verify(offenceSchedulePartRepository, times(1)).saveAll(captor.capture())

    assertThat(captor.value)
      .usingRecursiveComparison()
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          OSP_A123AA6,
          OSP_B123AA6,
          OSP_B123AA6A_CHILD,
        )
      )
  }

  @Test
  fun `Link offences does not link any orphaned children that are passed in`() {
    whenever(schedulePartRepository.findById(SCHEDULE_PART_1.id)).thenReturn(Optional.of(SCHEDULE_PART_1))
    whenever(
      offenceRepository.findByParentOffenceIdIn(
        setOf(
          OFFENCE_A123AA6.id,
          OFFENCE_Z123AA6A_ORPHAN.id
        )
      )
    ).thenReturn(emptyList())
    whenever(offenceRepository.findAllById(setOf(OFFENCE_A123AA6.id, OFFENCE_Z123AA6A_ORPHAN.id))).thenReturn(
      listOf(
        OFFENCE_A123AA6,
        OFFENCE_Z123AA6A_ORPHAN,
      )
    )

    scheduleService.linkOffences(SCHEDULE_PART_1.id, setOf(OFFENCE_A123AA6.id, OFFENCE_Z123AA6A_ORPHAN.id))

    verify(offenceSchedulePartRepository, times(1)).saveAll(
      listOf(
        OffenceSchedulePart(
          schedulePart = SCHEDULE_PART_1,
          offence = OFFENCE_A123AA6
        )
      )
    )
  }

  @Test
  fun `History records are saved when linking offences`() {
    whenever(schedulePartRepository.findById(SCHEDULE_PART_1.id)).thenReturn(Optional.of(SCHEDULE_PART_1))
    whenever(offenceRepository.findByParentOffenceIdIn(setOf(OFFENCE_A123AA6.id))).thenReturn(emptyList())
    whenever(offenceRepository.findAllById(setOf(OFFENCE_A123AA6.id))).thenReturn(listOf(OFFENCE_A123AA6))
    whenever(offenceSchedulePartRepository.saveAll(listOf(OSP_A123AA6))).thenReturn(listOf(OSP_A123AA6))

    scheduleService.linkOffences(SCHEDULE_PART_1.id, setOf(OFFENCE_A123AA6.id))

    val historyCaptor = argumentCaptor<List<OffenceToScheduleHistory>>()
    verify(offenceToScheduleHistoryRepository, times(1)).saveAll(historyCaptor.capture())
    assertThat(historyCaptor.value)
      .usingRecursiveComparison()
      .ignoringFields("createdDate")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          OffenceToScheduleHistory(
            offenceCode = OFFENCE_A123AA6.code,
            changeType = INSERT,
            schedulePartId = SCHEDULE_PART_1.id,
            schedulePartNumber = SCHEDULE_PART_1.partNumber,
            scheduleCode = SCHEDULE.code,
            offenceId = OFFENCE_A123AA6.id,
          )
        )
      )
  }

  @Test
  fun `Unlink offences passed in offences and any associated children`() {
    whenever(schedulePartRepository.findById(SCHEDULE_PART_1.id)).thenReturn(Optional.of(SCHEDULE_PART_1))
    whenever(offenceRepository.findAllById(listOf(OFFENCE_B123AA6.id, OFFENCE_Z123AA6A_ORPHAN.id))).thenReturn(
      listOf(
        OFFENCE_B123AA6
      )
    )
    whenever(offenceRepository.findByParentOffenceId(OFFENCE_B123AA6.id)).thenReturn(listOf(OFFENCE_B123AA6A_CHILD))
    whenever(
      offenceSchedulePartRepository.findOneBySchedulePartIdAndOffenceId(SCHEDULE_PART_1.id, OFFENCE_B123AA6.id)
    ).thenReturn(
      Optional.of(OSP_B123AA6)
    )
    whenever(
      offenceSchedulePartRepository.findOneBySchedulePartIdAndOffenceId(
        SCHEDULE_PART_1.id,
        OFFENCE_B123AA6A_CHILD.id
      )
    ).thenReturn(
      Optional.of(OSP_B123AA6A_CHILD)
    )

    scheduleService.unlinkOffences(
      listOf(
        SchedulePartIdAndOffenceId(SCHEDULE_PART_1.id, OFFENCE_B123AA6.id),
        SchedulePartIdAndOffenceId(SCHEDULE_PART_1.id, OFFENCE_Z123AA6A_ORPHAN.id),
      )
    )

    verify(
      offenceToScheduleHistoryRepository,
      times(1)
    ).save(argThat { offenceCode == OFFENCE_B123AA6.code && changeType == DELETE })
    verify(
      offenceToScheduleHistoryRepository,
      times(1)
    ).save(argThat { offenceCode == OFFENCE_B123AA6A_CHILD.code && changeType == DELETE })
    verify(offenceSchedulePartRepository, times(1)).deleteBySchedulePartIdAndOffenceId(
      SCHEDULE_PART_1.id,
      OFFENCE_B123AA6.id
    )
    verify(offenceSchedulePartRepository, times(1)).deleteBySchedulePartIdAndOffenceId(
      SCHEDULE_PART_1.id,
      OFFENCE_B123AA6A_CHILD.id
    )
  }

  companion object {
    private val SCHEDULE = Schedule(
      act = "Sentencing Act 2020",
      url = "https://www.legislation.gov.uk/ukpga/2020/17/schedule/13",
      code = "13",
    )

    private val SCHEDULE_PART_1 = SchedulePart(
      id = 991,
      schedule = SCHEDULE,
      partNumber = 1
    )

    private val OFFENCE_B123AA6 = Offence(
      id = 981,
      code = "B123AA6",
      description = "B Desc 1",
    )

    private val OFFENCE_B123AA6A_CHILD = Offence(
      id = 982,
      code = "B123AA6A",
      description = "Inchoate B Desc 1",
    )

    private val OFFENCE_A123AA6 = Offence(
      id = 983,
      code = "A123AA6",
      description = "A Desc 1",
      actsAndSections = "Statute desc A123",
    )

    private val OFFENCE_Z123AA6A_ORPHAN = Offence(
      id = 984,
      code = "Z123AA6A",
      description = "Z Desc 1",
      actsAndSections = "Statute desc Z123AA6A",
    )

    private val OSP_A123AA6 = OffenceSchedulePart(
      schedulePart = SCHEDULE_PART_1,
      offence = OFFENCE_A123AA6
    )

    private val OSP_B123AA6 = OffenceSchedulePart(
      schedulePart = SCHEDULE_PART_1,
      offence = OFFENCE_B123AA6
    )

    private val OSP_B123AA6A_CHILD = OffenceSchedulePart(
      schedulePart = SCHEDULE_PART_1,
      offence = OFFENCE_B123AA6A_CHILD
    )
  }
}
