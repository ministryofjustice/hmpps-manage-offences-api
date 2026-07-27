package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.NomisScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisScheduleName
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ScheduleStatus
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.UpdateSchedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.OffenceToScheduleMappingDto
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.FeatureToggleRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule as ModelSchedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePart as ModelSchedulePart

class ScheduleServiceTest {

  private val scheduleRepository = mock<ScheduleRepository>()
  private val schedulePartRepository = mock<SchedulePartRepository>()
  private val offenceScheduleMappingRepository = mock<OffenceScheduleMappingRepository>()
  private val offenceRepository = mock<OffenceRepository>()
  private val featureToggleRepository = mock<FeatureToggleRepository>()
  private val nomisScheduleMappingRepository = mock<NomisScheduleMappingRepository>()
  private val prisonApiUserClient = mock<PrisonApiUserClient>()
  private val cacheConfiguration = mock<CacheConfiguration>()
  private val scheduleVisibilityService = mock<ScheduleVisibilityService>()

  private val scheduleService =
    ScheduleService(
      scheduleRepository,
      schedulePartRepository,
      offenceScheduleMappingRepository,
      offenceRepository,
      featureToggleRepository,
      prisonApiUserClient,
      nomisScheduleMappingRepository,
      cacheConfiguration,
      scheduleVisibilityService,
    )

  @Nested
  inner class LinkOffencesTests {
    @Test
    fun `Link an offence does call prison api if the schedule is defined in nomis and also if the offence is a PCSC offence`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_15_PART_1))
      whenever(offenceRepository.findById(OFFENCE_ID_91)).thenReturn(Optional.of(BASE_OFFENCE))
      whenever(offenceRepository.findByParentOffenceId(OFFENCE_ID_91)).thenReturn(emptyList())
      whenever(nomisScheduleMappingRepository.findOneBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(
        NOMIS_SCHEDULE_MAPPING,
      )
      whenever(
        offenceScheduleMappingRepository.findBySchedulePartScheduleActAndSchedulePartScheduleCode(
          "Criminal Justice Act 2003",
          "15",
        ),
      ).thenReturn(
        listOf(
          OFFENCE_SCHEDULE_MAPPING_S15_P1_LIFE,
          OFFENCE_SCHEDULE_MAPPING_S15_P2_LIFE,
        ),
      )

      scheduleService.linkOffences(LINK_OFFENCE)

      verify(offenceScheduleMappingRepository).saveAll(
        listOf(
          OffenceScheduleMapping(
            schedulePart = SCHEDULE_15_PART_1,
            offence = BASE_OFFENCE,
          ),
        ),
      )
      verify(prisonApiUserClient).linkToSchedule(
        listOf(
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NOMIS_SCHEDULE_MAPPING.nomisScheduleName,
          ),
        ),
      )
      verify(prisonApiUserClient).linkToSchedule(
        listOf(
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.SCHEDULE_15_ATTRACTS_LIFE.name,
          ),
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.PCSC_SDS.name,
          ),
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.PCSC_SEC_250.name,
          ),
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.PCSC_SDS_PLUS.name,
          ),
        ),
      )
      verify(cacheConfiguration).cacheEvict()
    }

    @Test
    fun `Link an offence doesnt call prison api if the schedule isn't defined in nomis`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_13_PART_1))
      whenever(offenceRepository.findById(OFFENCE_ID_91)).thenReturn(Optional.of(OFFENCE_1))
      whenever(offenceRepository.findByParentOffenceId(OFFENCE_ID_91)).thenReturn(emptyList())
      whenever(nomisScheduleMappingRepository.findOneBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(null)

      scheduleService.linkOffences(LINK_OFFENCE)

      verify(offenceScheduleMappingRepository).saveAll(
        listOf(
          OffenceScheduleMapping(
            schedulePart = SCHEDULE_13_PART_1,
            offence = OFFENCE_1,
          ),
        ),
      )
      verifyNoInteractions(prisonApiUserClient)
      verify(cacheConfiguration).cacheEvict()
    }
  }

  @Nested
  inner class UnlinkOffencesTests {
    @Test
    fun `Unlink an offence does call prison api if the schedule is defined in nomis and also calls nomis for PCSC offences`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_15_PART_1))
      whenever(offenceRepository.findAllById(listOf(OFFENCE_ID_91))).thenReturn(listOf(BASE_OFFENCE.copy(id = OFFENCE_ID_91)))
      whenever(offenceRepository.findByParentOffenceId(OFFENCE_ID_91)).thenReturn(emptyList())
      whenever(nomisScheduleMappingRepository.findOneBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(
        NOMIS_SCHEDULE_MAPPING,
      )

      whenever(scheduleRepository.findOneByActAndCode("Criminal Justice Act 2003", "15")).thenReturn(SCHEDULE_15)
      whenever(schedulePartRepository.findByScheduleId(SCHEDULE_15.id)).thenReturn(
        listOf(
          SCHEDULE_15_PART_1,
          SCHEDULE_15_PART_2,
        ),
      )
      whenever(
        offenceScheduleMappingRepository.findBySchedulePartScheduleActAndSchedulePartScheduleCode(
          "Criminal Justice Act 2003",
          "15",
        ),
      ).thenReturn(
        listOf(
          OFFENCE_SCHEDULE_MAPPING_S15_P1_LIFE,
          OFFENCE_SCHEDULE_MAPPING_S15_P2_LIFE,
        ),
      )

      scheduleService.unlinkOffences(listOf(UNLINK_OFFENCE))

      verify(offenceScheduleMappingRepository).deleteBySchedulePartIdAndOffenceId(
        SCHEDULE_PART_ID_92,
        OFFENCE_ID_91,
      )

      verify(prisonApiUserClient).unlinkFromSchedule(
        listOf(
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NOMIS_SCHEDULE_MAPPING.nomisScheduleName,
          ),
        ),
      )

      verify(prisonApiUserClient).unlinkFromSchedule(
        listOf(
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.SCHEDULE_15_ATTRACTS_LIFE.name,
          ),
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.PCSC_SDS.name,
          ),
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.PCSC_SEC_250.name,
          ),
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.PCSC_SDS_PLUS.name,
          ),
        ),
      )
      verify(cacheConfiguration).cacheEvict()
    }

    @Test
    fun `Unlink an offence does not call prison api if the schedule isnt defined in nomis`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_13_PART_1))
      whenever(offenceRepository.findAllById(listOf(OFFENCE_ID_91))).thenReturn(listOf(OFFENCE_1.copy(id = OFFENCE_ID_91)))
      whenever(offenceRepository.findByParentOffenceId(OFFENCE_ID_91)).thenReturn(emptyList())
      whenever(nomisScheduleMappingRepository.findOneBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(null)

      scheduleService.unlinkOffences(listOf(UNLINK_OFFENCE))

      verify(offenceScheduleMappingRepository).deleteBySchedulePartIdAndOffenceId(
        SCHEDULE_PART_ID_92,
        OFFENCE_ID_91,
      )

      verifyNoInteractions(prisonApiUserClient)
      verify(cacheConfiguration).cacheEvict()
    }
  }

  @Nested
  inner class CreateScheduleTests {
    @Test
    fun `Creating a schedule that does not already exist saves the schedule and its parts`() {
      whenever(scheduleRepository.findOneByActAndCode("Act", "15")).thenReturn(null)
      whenever(scheduleRepository.save(any<Schedule>())).thenReturn(SCHEDULE_15)

      scheduleService.createSchedule(NEW_SCHEDULE)

      verify(scheduleRepository).save(any<Schedule>())
      verify(schedulePartRepository).saveAll(any<List<SchedulePart>>())
    }

    @Test
    fun `Creating a schedule that already exists throws and saves nothing`() {
      whenever(scheduleRepository.findOneByActAndCode("Act", "15")).thenReturn(SCHEDULE_15)

      assertThatThrownBy { scheduleService.createSchedule(NEW_SCHEDULE) }
        .isInstanceOf(EntityExistsException::class.java)

      verify(scheduleRepository, never()).save(any<Schedule>())
      verifyNoInteractions(schedulePartRepository)
    }
  }

  @Nested
  inner class ScheduleVisibilityTests {
    @Test
    fun `A caller without the admin role is only offered live schedules`() {
      whenever(scheduleVisibilityService.canViewDrafts()).thenReturn(false)
      whenever(scheduleRepository.findAllByStatus(ScheduleStatus.LIVE)).thenReturn(listOf(SCHEDULE_15))

      val schedules = scheduleService.findAllSchedules()

      assertThat(schedules).extracting("code").containsExactly("15")
      verify(scheduleRepository, never()).findAll()
    }

    @Test
    fun `An admin is offered drafts alongside live schedules`() {
      whenever(scheduleVisibilityService.canViewDrafts()).thenReturn(true)
      whenever(scheduleRepository.findAll()).thenReturn(listOf(SCHEDULE_15, DRAFT_SCHEDULE))

      val schedules = scheduleService.findAllSchedules()

      assertThat(schedules).extracting("code").containsExactlyInAnyOrder("15", "99")
      verify(scheduleRepository, never()).findAllByStatus(any())
    }

    @Test
    fun `Fetching a draft by id without the admin role is indistinguishable from it not existing`() {
      whenever(scheduleVisibilityService.canViewDrafts()).thenReturn(false)
      whenever(scheduleRepository.findById(DRAFT_SCHEDULE_ID)).thenReturn(Optional.of(DRAFT_SCHEDULE))

      assertThatThrownBy { scheduleService.findScheduleById(DRAFT_SCHEDULE_ID) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("No schedule exists for $DRAFT_SCHEDULE_ID")
    }
  }

  @Nested
  inner class SchedulePartTests {
    @Test
    fun `A part cannot be added twice with the same number`() {
      whenever(scheduleRepository.findById(15L)).thenReturn(Optional.of(SCHEDULE_15))
      whenever(schedulePartRepository.findByScheduleIdAndPartNumber(15L, 1)).thenReturn(SCHEDULE_15_PART_1)

      assertThatThrownBy { scheduleService.addSchedulePart(15L, 1) }
        .isInstanceOf(EntityExistsException::class.java)

      verify(schedulePartRepository, never()).save(any<SchedulePart>())
    }

    @Test
    fun `A part with linked offences cannot be deleted`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_15_PART_1))
      whenever(offenceScheduleMappingRepository.countBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(3L)

      assertThatThrownBy { scheduleService.deleteSchedulePart(SCHEDULE_PART_ID_92) }
        .isInstanceOf(ValidationException::class.java)

      verify(schedulePartRepository, never()).delete(any<SchedulePart>())
    }

    @Test
    fun `An empty part is deleted`() {
      whenever(schedulePartRepository.findById(SCHEDULE_PART_ID_92)).thenReturn(Optional.of(SCHEDULE_15_PART_1))
      whenever(offenceScheduleMappingRepository.countBySchedulePartId(SCHEDULE_PART_ID_92)).thenReturn(0L)

      scheduleService.deleteSchedulePart(SCHEDULE_PART_ID_92)

      verify(schedulePartRepository).delete(SCHEDULE_15_PART_1)
    }
  }

  @Nested
  inner class UpdateScheduleTests {
    @Test
    fun `The act and code of a published schedule cannot be changed`() {
      whenever(scheduleRepository.findById(15L)).thenReturn(Optional.of(SCHEDULE_15))

      assertThatThrownBy { scheduleService.updateSchedule(15L, UpdateSchedule(act = "Act", code = "16", url = "url")) }
        .isInstanceOf(ValidationException::class.java)

      verify(scheduleRepository, never()).save(any<Schedule>())
    }

    @Test
    fun `The url of a published schedule can be changed`() {
      whenever(scheduleRepository.findById(15L)).thenReturn(Optional.of(SCHEDULE_15))
      whenever(scheduleRepository.save(any<Schedule>())).thenReturn(SCHEDULE_15.copy(url = "new-url"))

      scheduleService.updateSchedule(15L, UpdateSchedule(act = "Act", code = "15", url = "new-url"))

      verify(scheduleRepository).save(SCHEDULE_15.copy(url = "new-url"))
    }

    @Test
    fun `The act and code of a draft schedule can be changed`() {
      whenever(scheduleRepository.findById(DRAFT_SCHEDULE_ID)).thenReturn(Optional.of(DRAFT_SCHEDULE))
      whenever(scheduleRepository.findOneByActAndCode("Act", "100")).thenReturn(null)
      whenever(scheduleRepository.save(any<Schedule>())).thenReturn(DRAFT_SCHEDULE.copy(code = "100"))

      scheduleService.updateSchedule(DRAFT_SCHEDULE_ID, UpdateSchedule(act = "Act", code = "100", url = "url"))

      verify(scheduleRepository).save(DRAFT_SCHEDULE.copy(code = "100"))
    }
  }

  companion object {
    private const val OFFENCE_ID_91 = 91L
    private const val SCHEDULE_PART_ID_92 = 92L
    private const val SCHEDULE_PART_ID_93 = 93L

    private val UNLINK_OFFENCE = SchedulePartIdAndOffenceId(
      offenceId = OFFENCE_ID_91,
      schedulePartId = SCHEDULE_PART_ID_92,
    )
    private val LINK_OFFENCE = LinkOffence(
      offenceId = OFFENCE_ID_91,
      schedulePartId = SCHEDULE_PART_ID_92,
    )

    private val SCHEDULE_13 = Schedule(code = "13", id = 15, act = "Act", url = "url")
    private val SCHEDULE_13_PART_1 = SchedulePart(id = SCHEDULE_PART_ID_92, partNumber = 1, schedule = SCHEDULE_13)

    private val SCHEDULE_15 = Schedule(code = "15", id = 15, act = "Act", url = "url", status = ScheduleStatus.LIVE)
    private const val DRAFT_SCHEDULE_ID = 99L
    private val DRAFT_SCHEDULE =
      Schedule(code = "99", id = DRAFT_SCHEDULE_ID, act = "Act", url = "url", status = ScheduleStatus.DRAFT)
    private val NEW_SCHEDULE = ModelSchedule(
      id = 0,
      act = "Act",
      code = "15",
      url = "url",
      scheduleParts = listOf(ModelSchedulePart(id = 0, partNumber = 1)),
    )
    private val SCHEDULE_15_PART_1 = SchedulePart(id = SCHEDULE_PART_ID_92, partNumber = 1, schedule = SCHEDULE_15)
    private val SCHEDULE_15_PART_2 = SchedulePart(id = SCHEDULE_PART_ID_93, partNumber = 2, schedule = SCHEDULE_15)
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
    val BEFORE_SDS_LIST_A_CUT_OFF_DATE = LocalDate.of(2022, 6, 27)
    private val OFFENCE_SCHEDULE_MAPPING_S15_P1_LIFE = OffenceScheduleMapping(
      offence = BASE_OFFENCE.copy(maxPeriodIsLife = true, startDate = BEFORE_SDS_LIST_A_CUT_OFF_DATE),
      schedulePart = SCHEDULE_15_PART_1,
      paragraphNumber = "65",
    )
    private val OFFENCE_SCHEDULE_MAPPING_S15_P2_LIFE = OffenceScheduleMapping(
      offence = BASE_OFFENCE.copy(maxPeriodIsLife = true, startDate = BEFORE_SDS_LIST_A_CUT_OFF_DATE),
      schedulePart = SCHEDULE_15_PART_2,
      paragraphNumber = "65",
    )

    private val OFFENCE_SCHEDULE_MAPPING_S15_P1_LIFE_AFTER_CUTOFF = OffenceScheduleMapping(
      offence = BASE_OFFENCE.copy(maxPeriodIsLife = true),
      schedulePart = SCHEDULE_15_PART_1,
      paragraphNumber = "65",
    )
  }

  @Nested
  inner class LinkOffenceToParentSchedulesTests {

    @Test
    fun `link offences to parent schedules when parent offence has children`() {
      val baseOffenceWithParent = BASE_OFFENCE.copy(parentOffenceId = 123)
      val parentOffence = OFFENCE_1.copy(id = 123)

      whenever(baseOffenceWithParent.parentOffenceId?.let { offenceRepository.findByParentOffenceId(it) }).thenReturn(
        listOf(parentOffence),
      )
      whenever(offenceScheduleMappingRepository.findByOffenceId(parentOffence.id)).thenReturn(
        listOf(
          OffenceScheduleMapping(
            offence = parentOffence,
            schedulePart = SCHEDULE_15_PART_1,
          ),
        ),
      )
      whenever(nomisScheduleMappingRepository.findOneBySchedulePartId(SCHEDULE_15_PART_1.id)).thenReturn(
        NOMIS_SCHEDULE_MAPPING,
      )

      scheduleService.linkOffenceToParentSchedules(baseOffenceWithParent)

      verify(offenceScheduleMappingRepository).saveAll(
        listOf(
          OffenceScheduleMapping(
            offence = baseOffenceWithParent,
            schedulePart = SCHEDULE_15_PART_1,
          ),
        ),
      )
      verify(prisonApiUserClient).linkToSchedule(
        listOf(
          OffenceToScheduleMappingDto(
            offenceCode = baseOffenceWithParent.code,
            schedule = NOMIS_SCHEDULE_MAPPING.nomisScheduleName,
          ),
        ),
      )
    }

    @Test
    fun `do nothing when parent offenceId is null`() {
      val offenceWithoutParent = BASE_OFFENCE.copy(parentOffenceId = null)

      scheduleService.linkOffenceToParentSchedules(offenceWithoutParent)

      verifyNoInteractions(offenceScheduleMappingRepository, prisonApiUserClient)
    }
  }
}
