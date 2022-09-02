package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiStatute
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.RestResponsePage
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceSchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence as ModelOffence

class OffenceServiceTest {
  private val offenceRepository = mock<OffenceRepository>()
  private val offenceSchedulePartRepository = mock<OffenceSchedulePartRepository>()
  private val sdrsLoadResultRepository = mock<SdrsLoadResultRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val adminService = mock<AdminService>()

  private val offenceService =
    OffenceService(
      offenceRepository,
      offenceSchedulePartRepository,
      sdrsLoadResultRepository,
      prisonApiClient,
      adminService
    )

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
    whenever(offenceRepository.findByCodeStartsWithIgnoreCase("B")).thenReturn(listOf(OFFENCE_B123AA6))

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
          actsAndSections = "Statute description B123"
        )
      )
    )

    offenceService.fullSyncWithNomis()

    verify(
      prisonApiClient,
      times(1)
    ).createStatutes(listOf(NOMIS_STATUTE_B123.copy(description = "Statute description B123")))
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
    whenever(offenceRepository.findByCodeStartsWithIgnoreCase("A")).thenReturn(
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
    whenever(offenceRepository.findByCodeStartsWithIgnoreCase("A")).thenReturn(
      listOf(
        OFFENCE_A123992,
        OFFENCE_A123991,
        OFFENCE_A123996A,
      )
    )
    whenever(offenceRepository.findByParentOffenceId(OFFENCE_A123992.id)).thenReturn(
      listOf(
        OFFENCE_A123993,
        OFFENCE_A123995,
      )
    )

    val offences = offenceService.findOffencesByCode("A")

    assertThat(offences)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("loadDate")
      .isEqualTo(
        listOf(
          MODEL_OFFENCE_A123992,
          MODEL_OFFENCE_A123991,
          MODEL_OFFENCE_A123996A
        )
      )
  }

  companion object {
    private val OFFENCE_B123AA6 = Offence(
      code = "B123AA6",
      description = "B Desc 1",
    )
    private val OFFENCE_A123AA6 = Offence(
      code = "A123AA6",
      description = "A Desc 1",
      actsAndSections = "Statute desc A123",
    )
    private val OFFENCE_A1234AAA = Offence(
      code = "A1234AAA",
      description = "A NEW DESC",
      actsAndSections = "Statute desc A123",
    )

    val OFFENCE_A123992 = Offence(
      id = 992,
      category = 1,
      subCategory = 2,
      cjsTitle = "Descriptiom",
      code = "A123992",
      startDate = LocalDate.of(2021, 6, 1),
      actsAndSections = "Statute 992"
    )

    val OFFENCE_A123991 = Offence(
      id = 991,
      cjsTitle = "Descriptiom",
      code = "A123991",
      startDate = LocalDate.of(2021, 5, 6),
      actsAndSections = "Statute 991"
    )

    val OFFENCE_A123993 = Offence(
      id = 993,
      cjsTitle = "Descriptiom",
      code = "A123993",
      startDate = LocalDate.of(2022, 7, 7),
      actsAndSections = "Statute 993"
    )

    val OFFENCE_A123995 = Offence(
      id = 995,
      cjsTitle = "Descriptiom",
      code = "A123995",
      startDate = LocalDate.of(2022, 5, 7),
      endDate = LocalDate.of(2022, 5, 8),
      actsAndSections = "Statute 995"
    )

    val OFFENCE_A167996 = Offence(
      id = 996,
      cjsTitle = "Descriptiom",
      code = "A167995",
    )

    val OFFENCE_A123996A = Offence(
      id = 997,
      cjsTitle = "Descriptiom",
      code = "A123996A",
      startDate = LocalDate.of(2021, 5, 6),
      actsAndSections = "Statute 997",
      parentOffenceId = 996,
    )

    private val NOMIS_STATUTE_B123 =
      PrisonApiStatute(code = "B123", description = "Statute desc", activeFlag = "Y", legislatingBodyCode = "UK")
    private val NOMIS_STATUTE_A123 =
      PrisonApiStatute(code = "A123", description = "Statute 993", activeFlag = "Y", legislatingBodyCode = "UK")
    private val NOMIS_STATUTE_A167 =
      PrisonApiStatute(code = "A167", description = "A167", activeFlag = "Y", legislatingBodyCode = "UK")
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

    val MODEL_OFFENCE_A123992 = ModelOffence(
      id = 992,
      code = "A123992",
      startDate = LocalDate.of(2021, 6, 1),
      cjsTitle = "Descriptiom",
      childOffenceIds = listOf(993, 995),
      homeOfficeStatsCode = "001/02"
    )

    val MODEL_OFFENCE_A123991 = ModelOffence(
      id = 991,
      code = "A123991",
      startDate = LocalDate.of(2021, 5, 6),
      cjsTitle = "Descriptiom",
      childOffenceIds = emptyList()
    )

    val MODEL_OFFENCE_A123996A = ModelOffence(
      id = 997,
      code = "A123996A",
      startDate = LocalDate.of(2021, 5, 6),
      cjsTitle = "Descriptiom",
      isChild = true,
      childOffenceIds = emptyList(),
      parentOffenceId = 996,
    )

    private fun createPrisonApiOffencesResponse(
      totalPages: Int,
      content: List<PrisonApiOffence>
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
