package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.NomisChangeHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.INSERT
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.UPDATE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisChangeType.OFFENCE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisChangeType.STATUTE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache.OFFENCES_A
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache.OFFENCES_B
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.RestResponsePage
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Statute
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisChangeHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence as ModelOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence as PrisonApiOffence

class OffenceServiceTest {
  private val offenceRepository = mock<OffenceRepository>()
  private val offenceScheduleMappingRepository = mock<OffenceScheduleMappingRepository>()
  private val sdrsLoadResultRepository = mock<SdrsLoadResultRepository>()
  private val nomisChangeHistoryRepository = mock<NomisChangeHistoryRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val adminService = mock<AdminService>()

  private val offenceService =
    OffenceService(
      offenceRepository,
      offenceScheduleMappingRepository,
      sdrsLoadResultRepository,
      nomisChangeHistoryRepository,
      prisonApiClient,
      adminService
    )

  private inline fun <reified T : Any> argumentCaptor(): ArgumentCaptor<T> = ArgumentCaptor.forClass(T::class.java)

  @BeforeEach
  fun setup() {
    val emptyPrisonApiOffences = createPrisonApiOffencesResponse(0, emptyList())
    ('A'..'Z').forEach { alphaChar ->
      whenever(prisonApiClient.findByOffenceCodeStartsWith(alphaChar.toString(), 0)).thenReturn(emptyPrisonApiOffences)
    }
    whenever(adminService.isFeatureEnabled(FULL_SYNC_NOMIS)).thenReturn(true)
  }

  @Test
  fun `Ensure prison api client is invoked one time when there is only one page of responses`() {
    whenever(prisonApiClient.findByOffenceCodeStartsWith("A", 0)).thenReturn(
      createPrisonApiOffencesResponse(
        1,
        listOf(
          NOMIS_OFFENCE_A1234AAA
        )
      )
    )

    offenceService.fullSyncWithNomis()

    verify(prisonApiClient, times(1)).findByOffenceCodeStartsWith("A", 0)
    verify(prisonApiClient, never()).findByOffenceCodeStartsWith("A", 1)
  }

  @Test
  fun `Ensure prison api client is invoked multiple times when there are multiple pages of offences`() {
    whenever(prisonApiClient.findByOffenceCodeStartsWith("A", 0)).thenReturn(PAGE_1_OF_2)
    whenever(prisonApiClient.findByOffenceCodeStartsWith("A", 1)).thenReturn(PAGE_2_OF_2)

    offenceService.fullSyncWithNomis()

    verify(prisonApiClient, times(1)).findByOffenceCodeStartsWith("A", 0)
    verify(prisonApiClient, times(1)).findByOffenceCodeStartsWith("A", 1)
    verify(prisonApiClient, never()).findByOffenceCodeStartsWith("A", 2)
  }

  @Test
  fun `When creating a statute in NOMIS, the description should be set to the statute code if there is no ActsAndSections value`() {
    whenever(offenceRepository.findByCodeStartsWithIgnoreCase("A")).thenReturn(listOf(OFFENCE_B123AA6))

    offenceService.fullSyncWithNomis()

    verify(
      prisonApiClient,
      times(1)
    ).createStatutes(listOf(NOMIS_STATUTE_B123.copy(description = NOMIS_STATUTE_B123.code)))
  }

  @Test
  fun `When creating a statute in NOMIS, the statute description should be set to the ActsAndSections value (if it exists)`() {
    whenever(offenceRepository.findByCodeStartsWithIgnoreCase("B")).thenReturn(
      listOf(
        OFFENCE_B123AA6.copy(
          legislation = "Statute description B123"
        )
      )
    )

    offenceService.fullSyncWithNomis()

    verify(
      prisonApiClient,
      times(1)
    ).createStatutes(listOf(NOMIS_STATUTE_B123.copy(description = "Statute description B123")))

    val nomisChangeHistoryCaptor = argumentCaptor<List<NomisChangeHistory>>()
    verify(nomisChangeHistoryRepository, times(2)).saveAll(nomisChangeHistoryCaptor.capture())
    assertThat(nomisChangeHistoryCaptor.allValues[0])
      .usingRecursiveComparison()
      .ignoringFields("sentToNomisDate")
      .isEqualTo(
        listOf(
          NomisChangeHistory(
            id = -1,
            code = NOMIS_STATUTE_B123.code,
            description = "Statute description B123",
            changeType = INSERT,
            nomisChangeType = STATUTE,
            sentToNomisDate = LocalDateTime.now()
          )
        ),
      )
    assertThat(nomisChangeHistoryCaptor.allValues[1])
      .usingRecursiveComparison()
      .ignoringFields("sentToNomisDate")
      .isEqualTo(
        listOf(
          NomisChangeHistory(
            id = -1,
            code = OFFENCE_B123AA6.code,
            description = OFFENCE_B123AA6.description!!,
            changeType = INSERT,
            nomisChangeType = OFFENCE,
            sentToNomisDate = LocalDateTime.now()
          )
        ),
      )
  }

  @Test
  fun `One offence to update and one to create makes the correct prison-api calls`() {
    whenever(prisonApiClient.findByOffenceCodeStartsWith("A", 0)).thenReturn(
      createPrisonApiOffencesResponse(
        1,
        listOf(
          NOMIS_OFFENCE_A1234AAA
        )
      )
    )
    whenever(offenceRepository.findByCodeStartsWithIgnoreCase("A")).thenReturn(
      listOf(
        OFFENCE_A123AA6,
        OFFENCE_A1234AAA
      )
    )

    offenceService.fullSyncWithNomis()

    ('A'..'Z').forEach { alphaChar ->
      verify(prisonApiClient, times(1)).findByOffenceCodeStartsWith(alphaChar.toString(), 0)
    }
    verify(prisonApiClient, times(1)).createOffences(listOf(NOMIS_OFFENCE_A123AA6))
    verify(prisonApiClient, times(1)).updateOffences(listOf(NOMIS_OFFENCE_A1234AAA_UPDATED))
    verifyNoMoreInteractions(prisonApiClient)
    val nomisChangeHistoryCaptor = argumentCaptor<List<NomisChangeHistory>>()
    verify(nomisChangeHistoryRepository, times(2)).saveAll(nomisChangeHistoryCaptor.capture())
    assertThat(nomisChangeHistoryCaptor.allValues[0])
      .usingRecursiveComparison()
      .ignoringFields("sentToNomisDate")
      .isEqualTo(
        listOf(
          NomisChangeHistory(
            id = -1,
            code = NOMIS_OFFENCE_A123AA6.code,
            description = NOMIS_OFFENCE_A123AA6.description,
            changeType = INSERT,
            nomisChangeType = OFFENCE,
            sentToNomisDate = LocalDateTime.now()
          )
        ),
      )
    assertThat(nomisChangeHistoryCaptor.allValues[1])
      .usingRecursiveComparison()
      .ignoringFields("sentToNomisDate")
      .isEqualTo(
        listOf(
          NomisChangeHistory(
            id = -1,
            code = NOMIS_OFFENCE_A1234AAA_UPDATED.code,
            description = NOMIS_OFFENCE_A1234AAA_UPDATED.description,
            changeType = UPDATE,
            nomisChangeType = OFFENCE,
            sentToNomisDate = LocalDateTime.now()
          )
        ),
      )
  }

  @Test
  fun `Does not call update if the offence details are the same in prison-api and manage-offences`() {
    whenever(prisonApiClient.findByOffenceCodeStartsWith("A", 0)).thenReturn(
      createPrisonApiOffencesResponse(
        1,
        listOf(
          NOMIS_OFFENCE_A1234AAA
        )
      )
    )
    whenever(offenceRepository.findBySdrsCache(OFFENCES_A)).thenReturn(
      listOf(
        OFFENCE_A1234AAA.copy(description = NOMIS_OFFENCE_A1234AAA.description)
      )
    )

    offenceService.fullSyncWithNomis()

    ('A'..'Z').forEach { alphaChar ->
      verify(prisonApiClient, times(1)).findByOffenceCodeStartsWith(alphaChar.toString(), 0)
    }
    verifyNoMoreInteractions(prisonApiClient)
  }

  @Test
  fun `Ensure sync with nomis doesnt occur if associated feature toggle is disabled`() {
    whenever(adminService.isFeatureEnabled(FULL_SYNC_NOMIS)).thenReturn(false)

    offenceService.fullSyncWithNomis()

    verifyNoInteractions(prisonApiClient)
  }

  @Test
  fun `When creating a statute the correct statute description is selected from the offences`() {
    whenever(offenceRepository.findByCodeStartsWithIgnoreCase("A")).thenReturn(
      listOf(
        OFFENCE_A123992,
        OFFENCE_A123991,
        OFFENCE_A123993,
        OFFENCE_A123995,
        OFFENCE_A167996,
      )
    )

    offenceService.fullSyncWithNomis()

    verify(prisonApiClient, times(1)).createStatutes(listOf(NOMIS_STATUTE_A123, NOMIS_STATUTE_A167))
    verify(prisonApiClient, times(1)).createStatutes(any())
  }

  @Test
  fun `Finding a parent offence returns all associated children`() {
    val matchingOffences = listOf(
      OFFENCE_A123992,
      OFFENCE_A123991,
      OFFENCE_A123996A,
    )
    whenever(offenceRepository.findByCodeStartsWithIgnoreCase("A")).thenReturn(
      matchingOffences
    )
    whenever(offenceRepository.findByParentOffenceIdIn(matchingOffences.map { it.id }.toSet())).thenReturn(
      listOf(
        OFFENCE_A123993,
        OFFENCE_A123995,
      )
    )

    val offences = offenceService.findOffencesByCode("A")

    assertThat(offences)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*dDate")
      .isEqualTo(
        listOf(
          MODEL_OFFENCE_A123991,
          MODEL_OFFENCE_A123992,
          MODEL_OFFENCE_A123996A
        )
      )
  }

  companion object {
    private val BASE_OFFENCE = Offence(code = "AABB011", changedDate = LocalDateTime.now(), revisionId = 1, startDate = LocalDate.now(), sdrsCache = OFFENCES_A)
    private val BASE_MODEL_OFFENCE = ModelOffence(code = "AABB", id = 1, changedDate = LocalDateTime.now(), revisionId = 1, startDate = LocalDate.now())
    private val OFFENCE_B123AA6 = BASE_OFFENCE.copy(
      sdrsCache = OFFENCES_B,
      code = "B123AA6",
      description = "B Desc 1",
    )
    private val OFFENCE_A123AA6 = BASE_OFFENCE.copy(
      sdrsCache = OFFENCES_A,
      code = "A123AA6",
      description = "A Desc 1",
      legislation = "Statute desc A123",
    )
    private val OFFENCE_A1234AAA = BASE_OFFENCE.copy(
      sdrsCache = OFFENCES_A,
      code = "A1234AAA",
      description = "A NEW DESC",
      legislation = "Statute desc A123",
    )

    val OFFENCE_A123992 = BASE_OFFENCE.copy(
      sdrsCache = OFFENCES_A,
      id = 992,
      category = 1,
      subCategory = 2,
      cjsTitle = "Descriptiom",
      code = "A123992",
      startDate = LocalDate.of(2021, 6, 1),
      legislation = "Statute 992"
    )

    val OFFENCE_A123991 = BASE_OFFENCE.copy(
      sdrsCache = OFFENCES_A,
      id = 991,
      cjsTitle = "Descriptiom",
      code = "A123991",
      startDate = LocalDate.of(2021, 5, 6),
      legislation = "Statute 991"
    )

    val OFFENCE_A123993 = BASE_OFFENCE.copy(
      sdrsCache = OFFENCES_A,
      id = 993,
      cjsTitle = "Descriptiom",
      code = "A123993",
      startDate = LocalDate.of(2022, 7, 7),
      legislation = "Statute 993",
      parentOffenceId = 992,
    )

    val OFFENCE_A123995 = BASE_OFFENCE.copy(
      sdrsCache = OFFENCES_A,
      id = 995,
      cjsTitle = "Descriptiom",
      code = "A123995",
      startDate = LocalDate.of(2022, 5, 7),
      endDate = LocalDate.of(2022, 5, 8),
      legislation = "Statute 995",
      parentOffenceId = 992,
    )

    val OFFENCE_A167996 = BASE_OFFENCE.copy(
      sdrsCache = OFFENCES_A,
      id = 996,
      cjsTitle = "Descriptiom",
      code = "A167995",
    )

    val OFFENCE_A123996A = BASE_OFFENCE.copy(
      sdrsCache = OFFENCES_A,
      id = 997,
      cjsTitle = "Descriptiom",
      code = "A123996A",
      startDate = LocalDate.of(2021, 5, 6),
      legislation = "Statute 997",
      parentOffenceId = 996,
    )

    private val NOMIS_STATUTE_B123 =
      Statute(code = "B123", description = "Statute desc", activeFlag = "Y", legislatingBodyCode = "UK")
    private val NOMIS_STATUTE_A123 =
      Statute(code = "A123", description = "Statute 993", activeFlag = "Y", legislatingBodyCode = "UK")
    private val NOMIS_STATUTE_A167 =
      Statute(code = "A167", description = "A167", activeFlag = "Y", legislatingBodyCode = "UK")
    private val NOMIS_OFFENCE_A1234AAA = PrisonApiOffence(
      code = "A1234AAA",
      description = "A Desc 1",
      statuteCode = NOMIS_STATUTE_A123,
      activeFlag = "Y"
    )

    private val NOMIS_OFFENCE_A123AA6 = PrisonApiOffence(
      code = "A123AA6",
      description = "A Desc 1",
      statuteCode = NOMIS_STATUTE_A123,
      severityRanking = "99",
      activeFlag = "Y"
    )
    private val NOMIS_OFFENCE_A1234AAA_UPDATED = PrisonApiOffence(
      code = "A1234AAA",
      description = "A NEW DESC",
      statuteCode = NOMIS_STATUTE_A123,
      activeFlag = "Y"
    )
    private val NOMIS_OFFENCE_A1234AAB = PrisonApiOffence(
      code = "A1234AAB",
      description = "A Desc 2",
      statuteCode = NOMIS_STATUTE_A123,
      activeFlag = "Y"
    )
    val PAGE_1_OF_2 = createPrisonApiOffencesResponse(
      2,
      listOf(
        NOMIS_OFFENCE_A1234AAA
      )
    )

    val PAGE_2_OF_2 = createPrisonApiOffencesResponse(
      2,
      listOf(
        NOMIS_OFFENCE_A1234AAB
      )
    )

    val MODEL_OFFENCE_A123992 = BASE_MODEL_OFFENCE.copy(
      id = 992,
      code = "A123992",
      startDate = LocalDate.of(2021, 6, 1),
      description = "Descriptiom",
      childOffenceIds = listOf(993, 995),
      homeOfficeStatsCode = "001/02",
      legislation = "Statute 992",
      maxPeriodIsLife = false,
    )

    val MODEL_OFFENCE_A123991 = BASE_MODEL_OFFENCE.copy(
      id = 991,
      code = "A123991",
      startDate = LocalDate.of(2021, 5, 6),
      description = "Descriptiom",
      childOffenceIds = emptyList(),
      legislation = "Statute 991",
      maxPeriodIsLife = false,
    )

    val MODEL_OFFENCE_A123996A = BASE_MODEL_OFFENCE.copy(
      id = 997,
      code = "A123996A",
      startDate = LocalDate.of(2021, 5, 6),
      description = "Descriptiom",
      isChild = true,
      childOffenceIds = emptyList(),
      parentOffenceId = 996,
      legislation = "Statute 997",
      maxPeriodIsLife = false,
    )

    private fun createPrisonApiOffencesResponse(
      totalPages: Int,
      content: List<PrisonApiOffence>,
    ): RestResponsePage<PrisonApiOffence> = RestResponsePage(
      content = content,
      number = 1,
      size = 1,
      totalElements = 0L,
      pageable = JacksonUtil.toJsonNode("{}"),
      last = true,
      totalPages = totalPages,
      sort = JacksonUtil.toJsonNode("{}"),
      first = true,
      numberOfElements = 0,
    )
  }
}
