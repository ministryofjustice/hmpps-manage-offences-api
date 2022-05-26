package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiStatute
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.RestResponsePage
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository

class OffenceServiceTest {
  private val offenceRepository = mock<OffenceRepository>()
  private val sdrsLoadResultRepository = mock<SdrsLoadResultRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()

  private val offenceService = OffenceService(offenceRepository, sdrsLoadResultRepository, prisonApiClient)

  @BeforeEach
  fun setup() {
    val emptyPrisonApiOffences = createPrisonApiOffencesResponse(0, emptyList())
    ('A'..'Z').forEach { alphaChar ->
      whenever(prisonApiClient.findByOffenceCodeStartsWith(alphaChar.toString(), 0)).thenReturn(emptyPrisonApiOffences)
    }
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

    verify(prisonApiClient, times(1)).createStatute(NOMIS_STATUTE_B123.copy(description = NOMIS_STATUTE_B123.code))
  }

  @Test
  fun `When creating a statute in NOMIS, the statute description should be set to the ActsAndSections value (if it exists)`() {
    whenever(offenceRepository.findByCodeStartsWithIgnoreCase("B")).thenReturn(listOf(OFFENCE_B123AA6.copy(actsAndSections = "Statute description B123")))

    offenceService.fullSyncWithNomis()

    verify(prisonApiClient, times(1)).createStatute(NOMIS_STATUTE_B123.copy(description = "Statute description B123"))
  }

  companion object {
    private val OFFENCE_B123AA6 = Offence(
      code = "B123AA6",
      description = "B Desc 1",
    )
    private val NOMIS_STATUTE_B123 =
      PrisonApiStatute(code = "B123", description = "Statute desc", activeFlag = "Y", legislatingBodyCode = "UK")
    private val NOMIS_STATUTE_A123 =
      PrisonApiStatute(code = "A123", description = "Statute desc", activeFlag = "Y", legislatingBodyCode = "UK")
    private val NOMIS_OFFENCE_A1234AAA = PrisonApiOffence(
      code = "A1234AAA",
      description = "A Desc 1",
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
