package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffencePcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.FeatureToggleRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceToSyncWithNomisRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import java.time.LocalDate
import java.time.LocalDateTime

class IsOffenceInScheduleServiceTest {

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

  private val cachedScheduleService = CachedScheduleService(scheduleService)
  private val isOffenceInScheduleService = IsOffenceInScheduleService(featureToggleRepository, cachedScheduleService)

  @Nested
  inner class PcscTests {
    @Test
    fun `Determine PCSC offences when all indicators are false`() {
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
        emptyList(),
      )

      val res = isOffenceInScheduleService.findPcscMarkers(listOf(BASE_OFFENCE.code))

      assertThat(res).isEqualTo(
        listOf(
          OffencePcscMarkers(
            offenceCode = BASE_OFFENCE.code,
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
    fun `Determine PCSC offences when all indicators are true`() {
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
        ),
      )

      val res = isOffenceInScheduleService.findPcscMarkers(listOf(BASE_OFFENCE.code))

      assertThat(res).isEqualTo(
        listOf(
          OffencePcscMarkers(
            offenceCode = BASE_OFFENCE.code,
            PcscMarkers(
              inListA = true,
              inListB = true,
              inListC = true,
              inListD = true,
            ),
          ),
        ),
      )
    }

    @Test
    fun `Determine PCSC offences with a mix of indicators true and false`() {
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
          OFFENCE_SCHEDULE_MAPPING_S15_P1_LIFE_AFTER_CUTOFF,
        ),
      )

      val res = isOffenceInScheduleService.findPcscMarkers(listOf(BASE_OFFENCE.code))

      assertThat(res).isEqualTo(
        listOf(
          OffencePcscMarkers(
            offenceCode = BASE_OFFENCE.code,
            PcscMarkers(
              inListA = false,
              inListB = true,
              inListC = true,
              inListD = true,
            ),
          ),
        ),
      )
    }
  }

  companion object {
    private const val SCHEDULE_PART_ID_92 = 92L
    private const val SCHEDULE_PART_ID_93 = 93L
    private val SCHEDULE_15 = Schedule(code = "15", id = 15, act = "Act", url = "url")
    private val SCHEDULE_15_PART_1 = SchedulePart(id = SCHEDULE_PART_ID_92, partNumber = 1, schedule = SCHEDULE_15)
    private val SCHEDULE_15_PART_2 = SchedulePart(id = SCHEDULE_PART_ID_93, partNumber = 2, schedule = SCHEDULE_15)

    private val BASE_OFFENCE = Offence(
      code = "AABB011",
      changedDate = LocalDateTime.now(),
      revisionId = 1,
      startDate = LocalDate.now(),
      sdrsCache = SdrsCache.OFFENCES_A,
    )
    val BEFORE_SDS_LIST_A_CUT_OFF_DATE = LocalDate.of(2022, 6, 27)
    private val OFFENCE_SCHEDULE_MAPPING_S15_P1_LIFE = OffenceScheduleMapping(
      offence = BASE_OFFENCE.copy(maxPeriodIsLife = true, startDate = BEFORE_SDS_LIST_A_CUT_OFF_DATE),
      schedulePart = SCHEDULE_15_PART_1,
      paragraphNumber = "65",
    )
    private val OFFENCE_SCHEDULE_MAPPING_S15_P1_LIFE_AFTER_CUTOFF = OffenceScheduleMapping(
      offence = BASE_OFFENCE.copy(maxPeriodIsLife = true),
      schedulePart = SCHEDULE_15_PART_1,
      paragraphNumber = "65",
    )
  }
}
