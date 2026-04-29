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
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SdsOffenceDetails
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.FeatureToggleRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceToSyncWithNomisRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.ScheduleService.Companion.NATIONAL_SECURITY_LEGISLATION
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.ScheduleService.Companion.SCHEDULE_13
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.ScheduleService.Companion.SEXUAL_OFFENCES_LEGISLATION
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
      whenever(schedulePartRepository.findByScheduleId(SCHEDULE_15_ENTITY.id)).thenReturn(
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
      whenever(schedulePartRepository.findByScheduleId(SCHEDULE_15_ENTITY.id)).thenReturn(
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
      whenever(schedulePartRepository.findByScheduleId(SCHEDULE_15_ENTITY.id)).thenReturn(
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

  @Nested
  inner class CombinedSDSMarkersTests {
    @Test
    fun `Can fetch SDS markers for offence with PCSC markers and include SDS 40 exclusion`() {
      whenever(schedulePartRepository.findByScheduleId(SCHEDULE_15_ENTITY.id)).thenReturn(
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
      val res = isOffenceInScheduleService.getSdsOffenceDetails(listOf(BASE_OFFENCE.code))
      assertThat(res).isEqualTo(
        listOf(
          SdsOffenceDetails(
            offenceCode = BASE_OFFENCE.code,
            PcscMarkers(
              inListA = false,
              inListB = true,
              inListC = true,
              inListD = true,
            ),
            listOf(OffenceSdsExclusionIndicator.VIOLENT),
          ),
        ),
      )
    }

    @Test
    fun `Can fetch SDS markers for offence with no PCSC markers but with an SDS 40 exclusions`() {
      whenever(schedulePartRepository.findByScheduleId(SCHEDULE_15_ENTITY.id)).thenReturn(
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
      ).thenReturn(emptyList())
      whenever(offenceRepository.findByLegislationLikeIgnoreCase(SEXUAL_OFFENCES_LEGISLATION))
        .thenReturn(listOf(BASE_OFFENCE))

      val res = isOffenceInScheduleService.getSdsOffenceDetails(listOf(BASE_OFFENCE.code))
      assertThat(res).isEqualTo(
        listOf(
          SdsOffenceDetails(
            offenceCode = BASE_OFFENCE.code,
            PcscMarkers(
              inListA = false,
              inListB = false,
              inListC = false,
              inListD = false,
            ),
            listOf(OffenceSdsExclusionIndicator.SEXUAL),
          ),
        ),
      )
    }

    @Test
    fun `Can fetch SDS markers for offence without PCSC markers but with both a SDS40 and progression model exclusion`() {
      whenever(
        offenceRepository.findByLegislationLikeIgnoreCase(
          NATIONAL_SECURITY_LEGISLATION[0],
          NATIONAL_SECURITY_LEGISLATION[1],
          NATIONAL_SECURITY_LEGISLATION[2],
          NATIONAL_SECURITY_LEGISLATION[3],
        ),
      )
        .thenReturn(listOf(BASE_OFFENCE))

      whenever(
        offenceScheduleMappingRepository.findBySchedulePartScheduleActAndSchedulePartScheduleCode(
          SCHEDULE_13.act,
          SCHEDULE_13.code,
        ),
      ).thenReturn(listOf(OFFENCE_SCHEDULE_MAPPING_S13_P3))

      val res = isOffenceInScheduleService.getSdsOffenceDetails(listOf(BASE_OFFENCE.code))
      assertThat(res).isEqualTo(
        listOf(
          SdsOffenceDetails(
            offenceCode = BASE_OFFENCE.code,
            PcscMarkers(
              inListA = false,
              inListB = false,
              inListC = false,
              inListD = false,
            ),
            listOf(OffenceSdsExclusionIndicator.NATIONAL_SECURITY, OffenceSdsExclusionIndicator.SCHEDULE_13_PART_3),
          ),
        ),
      )
    }
  }

  companion object {
    private const val SCHEDULE_PART_ID_92 = 92L
    private const val SCHEDULE_PART_ID_93 = 93L
    private val SCHEDULE_15_ENTITY = Schedule(code = "15", id = 15, act = "Act", url = "url")
    private val SCHEDULE_15_PART_1 = SchedulePart(id = SCHEDULE_PART_ID_92, partNumber = 1, schedule = SCHEDULE_15_ENTITY)
    private val SCHEDULE_15_PART_2 = SchedulePart(id = SCHEDULE_PART_ID_93, partNumber = 2, schedule = SCHEDULE_15_ENTITY)

    private const val SCHEDULE_PART_ID_99 = 99L
    private val SCHEDULE_13_ENTITY = Schedule(code = "13", id = 13, act = "Act", url = "url")
    private val SCHEDULE_13_PART_3 = SchedulePart(id = SCHEDULE_PART_ID_99, partNumber = 3, schedule = SCHEDULE_13_ENTITY)

    private val BASE_OFFENCE = Offence(
      code = "AABB011",
      changedDate = LocalDateTime.now(),
      revisionId = 1,
      startDate = LocalDate.now(),
      sdrsCache = SdrsCache.OFFENCES_A,
    )
    private val BEFORE_SDS_LIST_A_CUT_OFF_DATE = LocalDate.of(2022, 6, 27)
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
    private val OFFENCE_SCHEDULE_MAPPING_S13_P3 = OffenceScheduleMapping(
      offence = BASE_OFFENCE,
      schedulePart = SCHEDULE_13_PART_3,
    )
  }
}
