package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.NomisScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceToSyncWithNomis
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisScheduleName
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisSyncType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.OffenceToScheduleMappingDto
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.FeatureToggleRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceToSyncWithNomisRepository
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
  private val featureToggleRepository = mock<FeatureToggleRepository>()
  private val nomisScheduleMappingRepository = mock<NomisScheduleMappingRepository>()
  private val prisonApiUserClient = mock<PrisonApiUserClient>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val offenceToSyncWithNomisRepository = mock<OffenceToSyncWithNomisRepository>()
  private val adminService = mock<AdminService>()
  private val cacheConfiguration = mock<CacheConfiguration>()

  private val scheduleService =
    ScheduleService(
      scheduleRepository,
      schedulePartRepository,
      offenceScheduleMappingRepository,
      offenceRepository,
      featureToggleRepository,
      prisonApiUserClient,
      prisonApiClient,
      nomisScheduleMappingRepository,
      offenceToSyncWithNomisRepository,
      adminService,
      cacheConfiguration,
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
  inner class NomisScheduleMappingMigrationTests {
    @Test
    fun `Test scheduled job that unlinks schedule mappings to NOMIS`() {
      whenever(adminService.isFeatureEnabled(Feature.UNLINK_SCHEDULES_NOMIS)).thenReturn(true)
      whenever(offenceToSyncWithNomisRepository.findByNomisSyncType(NomisSyncType.UNLINK_SCHEDULE_FROM_OFFENCE)).thenReturn(
        listOf(S15_OFFENCE_TO_SYNC_WITH_NOMIS),
      )

      scheduleService.unlinkScheduleMappingsToNomis()

      verify(prisonApiClient).unlinkFromSchedule(
        listOf(
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.SCHEDULE_15.name,
          ),
        ),
      )
      verify(offenceToSyncWithNomisRepository).deleteAllById(listOf(S15_OFFENCE_TO_SYNC_WITH_NOMIS.id))
      verify(cacheConfiguration).cacheEvict()
    }

    @Test
    fun `Test scheduled job that links schedule mappings to NOMIS`() {
      whenever(adminService.isFeatureEnabled(Feature.LINK_SCHEDULES_NOMIS)).thenReturn(true)
      whenever(offenceToSyncWithNomisRepository.findByNomisSyncType(NomisSyncType.LINK_SCHEDULE_TO_OFFENCE)).thenReturn(
        listOf(S15_OFFENCE_TO_SYNC_WITH_NOMIS, POTENTIAL_LINK_PCSC_OFFENCE_TO_LINK_WITH_NOMIS),
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

      scheduleService.linkScheduleMappingsToNomis()

      verify(prisonApiClient).linkToSchedule(
        listOf(
          OffenceToScheduleMappingDto(
            BASE_OFFENCE.code,
            NomisScheduleName.SCHEDULE_15.name,
          ),
        ),
      )
      verify(prisonApiClient).linkToSchedule(
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
      verify(offenceToSyncWithNomisRepository).deleteAllById(
        listOf(
          S15_OFFENCE_TO_SYNC_WITH_NOMIS.id,
          POTENTIAL_LINK_PCSC_OFFENCE_TO_LINK_WITH_NOMIS.id,
        ),
      )
      verify(cacheConfiguration).cacheEvict()
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

    private val SCHEDULE_15 = Schedule(code = "15", id = 15, act = "Act", url = "url")
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

    private val S15_OFFENCE_TO_SYNC_WITH_NOMIS = OffenceToSyncWithNomis(
      offenceCode = BASE_OFFENCE.code,
      nomisScheduleName = NomisScheduleName.SCHEDULE_15,
      nomisSyncType = NomisSyncType.UNLINK_SCHEDULE_FROM_OFFENCE,
    )

    private val POTENTIAL_LINK_PCSC_OFFENCE_TO_LINK_WITH_NOMIS = OffenceToSyncWithNomis(
      offenceCode = BASE_OFFENCE.code,
      nomisScheduleName = NomisScheduleName.POTENTIAL_LINK_PCSC,
      nomisSyncType = NomisSyncType.UNLINK_SCHEDULE_FROM_OFFENCE,
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
