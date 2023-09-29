package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.NomisScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.OffenceToScheduleMappingDto
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class ScheduleServiceTest {

  private val scheduleRepository = mock<ScheduleRepository>()
  private val schedulePartRepository = mock<SchedulePartRepository>()
  private val offenceScheduleMappingRepository = mock<OffenceScheduleMappingRepository>()
  private val offenceRepository = mock<OffenceRepository>()
  private val nomisScheduleMappingRepository = mock<NomisScheduleMappingRepository>()
  private val prisonApiUserClient = mock<PrisonApiUserClient>()

  private val scheduleService =
    ScheduleService(
      scheduleRepository,
      schedulePartRepository,
      offenceScheduleMappingRepository,
      offenceRepository,
      prisonApiUserClient,
      nomisScheduleMappingRepository,
    )

  @Nested
  inner class LinkOffencesTests {
    @Test
    fun `Link an offence does call prison api if the schedule is defined in nomis`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_PART))
      whenever(offenceRepository.findById(OFFENCE_ID_91)).thenReturn(Optional.of(OFFENCE_1))
      whenever(offenceRepository.findByParentOffenceId(OFFENCE_ID_91)).thenReturn(emptyList())
      whenever(nomisScheduleMappingRepository.findOneBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(
        NOMIS_SCHEDULE_MAPPING,
      )

      scheduleService.linkOffences(LINK_OFFENCE)

      verify(offenceScheduleMappingRepository).saveAll(
        listOf(
          OffenceScheduleMapping(
            schedulePart = SCHEDULE_PART,
            offence = OFFENCE_1,
          ),
        ),
      )
      verify(prisonApiUserClient).linkToSchedule(
        listOf(
          OffenceToScheduleMappingDto(
            OFFENCE_1.code,
            NOMIS_SCHEDULE_MAPPING.nomisScheduleName,
          ),
        ),
      )
    }

    @Test
    fun `Link an offence doesnt call prison api if the schedule isn't defined in nomis`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_PART))
      whenever(offenceRepository.findById(OFFENCE_ID_91)).thenReturn(Optional.of(OFFENCE_1))
      whenever(offenceRepository.findByParentOffenceId(OFFENCE_ID_91)).thenReturn(emptyList())
      whenever(nomisScheduleMappingRepository.findOneBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(null)

      scheduleService.linkOffences(LINK_OFFENCE)

      verify(offenceScheduleMappingRepository).saveAll(
        listOf(
          OffenceScheduleMapping(
            schedulePart = SCHEDULE_PART,
            offence = OFFENCE_1,
          ),
        ),
      )
      verifyNoInteractions(prisonApiUserClient)
    }
  }

  @Nested
  inner class UnlinkOffencesTests {
    @Test
    fun `Unlink an offence does call prison api if the schedule is defined in nomis`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_PART))
      whenever(offenceRepository.findAllById(listOf(OFFENCE_ID_91))).thenReturn(listOf(OFFENCE_1.copy(id = OFFENCE_ID_91)))
      whenever(offenceRepository.findByParentOffenceId(OFFENCE_ID_91)).thenReturn(emptyList())
      whenever(nomisScheduleMappingRepository.findOneBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(
        NOMIS_SCHEDULE_MAPPING,
      )

      scheduleService.unlinkOffences(listOf(UNLINK_OFFENCE))

      verify(offenceScheduleMappingRepository).deleteBySchedulePartIdAndOffenceId(
        SCHEDULE_PART_ID_92,
        OFFENCE_ID_91,
      )

      verify(prisonApiUserClient).unlinkFromSchedule(
        listOf(
          OffenceToScheduleMappingDto(
            OFFENCE_1.code,
            NOMIS_SCHEDULE_MAPPING.nomisScheduleName,
          ),
        ),
      )
    }

    @Test
    fun `Unlink an offence does not call prison api if the schedule isnt defined in nomis`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_PART))
      whenever(offenceRepository.findAllById(listOf(OFFENCE_ID_91))).thenReturn(listOf(OFFENCE_1.copy(id = OFFENCE_ID_91)))
      whenever(offenceRepository.findByParentOffenceId(OFFENCE_ID_91)).thenReturn(emptyList())
      whenever(nomisScheduleMappingRepository.findOneBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(null)

      scheduleService.unlinkOffences(listOf(UNLINK_OFFENCE))

      verify(offenceScheduleMappingRepository).deleteBySchedulePartIdAndOffenceId(
        SCHEDULE_PART_ID_92,
        OFFENCE_ID_91,
      )

      verifyNoInteractions(prisonApiUserClient)
    }
  }

  companion object {
    private const val OFFENCE_ID_91 = 91L
    private const val SCHEDULE_PART_ID_92 = 92L

    private val UNLINK_OFFENCE = SchedulePartIdAndOffenceId(
      offenceId = OFFENCE_ID_91,
      schedulePartId = SCHEDULE_PART_ID_92,
    )
    private val LINK_OFFENCE = LinkOffence(
      offenceId = OFFENCE_ID_91,
      schedulePartId = SCHEDULE_PART_ID_92,
    )
    private val SCHEDULE = Schedule(code = "15", id = 15, act = "Act", url = "url")
    private val SCHEDULE_PART = SchedulePart(id = SCHEDULE_PART_ID_92, partNumber = 1, schedule = SCHEDULE)
    private val NOMIS_SCHEDULE_MAPPING =
      NomisScheduleMapping(schedulePartId = SCHEDULE_PART_ID_92, nomisScheduleName = "S15")

    private val BASE_OFFENCE = Offence(
      code = "AABB011",
      changedDate = LocalDateTime.now(),
      revisionId = 1,
      startDate = LocalDate.now(),
      sdrsCache = SdrsCache.OFFENCES_A,
    )
    val OFFENCE_1 = BASE_OFFENCE.copy(code = "OFF1")
  }
}
