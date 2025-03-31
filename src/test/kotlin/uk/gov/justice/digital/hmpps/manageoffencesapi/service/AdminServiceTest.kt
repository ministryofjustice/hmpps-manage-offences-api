package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import io.hypersistence.utils.hibernate.type.json.internal.JacksonUtil
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.EventToRaise
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.EventType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache.OFFENCES_A
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.RestResponsePage
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Statute
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.EventToRaiseRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.FeatureToggleRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceReactivatedInNomisRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.FeatureToggle as FeatureToggleEntity

class AdminServiceTest {
  private val featureToggleRepository = mock<FeatureToggleRepository>()
  private val offenceRepository = mock<OffenceRepository>()
  private val offenceReactivatedInNomisRepository = mock<OffenceReactivatedInNomisRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val prisonApiUserClient = mock<PrisonApiUserClient>()
  private val eventToRaiseRepository = mock<EventToRaiseRepository>()
  private val scheduleService = mock<ScheduleService>()

  private val adminService = AdminService(
    featureToggleRepository,
    offenceRepository,
    offenceReactivatedInNomisRepository,
    prisonApiClient,
    prisonApiUserClient,
    eventToRaiseRepository,
    scheduleService,
  )

  @Test
  fun `If the feature doesnt exist then no save is performed`() {
    whenever(featureToggleRepository.findById(FULL_SYNC_NOMIS)).thenReturn(Optional.empty())

    adminService.toggleFeature(listOf(FeatureToggle(FULL_SYNC_NOMIS, true)))

    verify(featureToggleRepository, times(1)).findById(FULL_SYNC_NOMIS)
    verifyNoMoreInteractions(featureToggleRepository)
  }

  @Test
  fun `Toggling a feature gets saved`() {
    whenever(featureToggleRepository.findById(FULL_SYNC_NOMIS)).thenReturn(
      Optional.of(
        FeatureToggleEntity(
          FULL_SYNC_NOMIS,
          false,
        ),
      ),
    )

    adminService.toggleFeature(listOf(FeatureToggle(FULL_SYNC_NOMIS, true)))

    verify(featureToggleRepository, times(1)).save(FeatureToggleEntity(FULL_SYNC_NOMIS, true))
  }

  @Test
  fun `Get all toggles`() {
    whenever(featureToggleRepository.findAll()).thenReturn(listOf(FeatureToggleEntity(FULL_SYNC_NOMIS, false)))

    val res = adminService.getAllToggles()

    assertThat(res).isEqualTo(listOf(FeatureToggle(FULL_SYNC_NOMIS, false)))
  }

  @Test
  fun `Create valid Encouragement offence`() {
    whenever(offenceRepository.findById(10)).thenReturn(Optional.of(PARENT_OFFENCE))
    whenever(offenceRepository.findByParentOffenceId(10)).thenReturn(listOf(CHILD_OFFENCE_ONE, CHILD_OFFENCE_TWO))
    whenever(prisonApiClient.findByOffenceCodeStartsWith(PARENT_OFFENCE.code, 0)).thenReturn(
      createPrisonApiOffencesResponse(1, listOf(NOMIS_OFFENCE_AABB010)),
    )

    val encouragementCode = PARENT_OFFENCE.code + 'E'
    val encouragementCjsDesc = "Encouragement to ${PARENT_OFFENCE.description}"

    val savedOffence = PARENT_OFFENCE.copy(
      id = 999,
      code = encouragementCode,
      description = encouragementCjsDesc,
      cjsTitle = "Encouragement to ${PARENT_OFFENCE.cjsTitle}",
    )

    whenever(offenceRepository.save(any())).thenReturn(savedOffence)
    doNothing().`when`(prisonApiClient).createOffences(any())

    val res = adminService.createEncouragementOffence(10)

    verify(prisonApiClient).createOffences(
      listOf(
        NOMIS_OFFENCE_AABB010.copy(
          code = encouragementCode,
          description = encouragementCjsDesc,
        ),
      ),
    )

    verify(eventToRaiseRepository).save(
      EventToRaise(
        offenceCode = PARENT_OFFENCE.code + 'E',
        eventType = EventType.OFFENCE_CHANGED,
      ),
    )

    assertThat(res).isEqualTo(
      transform(savedOffence, childOffenceIds = listOf()),
    )
  }

  @Test
  fun `Encouragement offence request with existing Encouragement offence should fail`() {
    val encouragementCode = PARENT_OFFENCE.code + 'E'
    whenever(offenceRepository.findById(10)).thenReturn(Optional.of(PARENT_OFFENCE))
    whenever(offenceRepository.findByParentOffenceId(10)).thenReturn(
      listOf(
        CHILD_OFFENCE_ONE.copy(
          code = encouragementCode,
        ),
        CHILD_OFFENCE_TWO,
      ),
    )

    assertThatThrownBy {
      adminService.createEncouragementOffence(10)
    }.isInstanceOf(ValidationException::class.java)
      .hasMessage("Encouragement offence already exists for offence code $encouragementCode")
  }

  @Test
  fun `Encouragement offence request with end date prior to 2008-02-15 should fail`() {
    val cutOffDate = LocalDate.parse("2008-02-15")
    whenever(offenceRepository.findById(10)).thenReturn(
      Optional.of(
        PARENT_OFFENCE.copy(endDate = LocalDate.parse("2008-02-14")),
      ),
    )

    assertThatThrownBy {
      adminService.createEncouragementOffence(10)
    }.isInstanceOf(ValidationException::class.java)
      .hasMessage("Offence must have an end date post $cutOffDate")
  }

  @Test
  fun `Encouragement offence request with no Prison API record should fail`() {
    whenever(offenceRepository.findById(10)).thenReturn(Optional.of(PARENT_OFFENCE))
    whenever(offenceRepository.findByParentOffenceId(10)).thenReturn(listOf(CHILD_OFFENCE_ONE, CHILD_OFFENCE_TWO))
    whenever(prisonApiClient.findByOffenceCodeStartsWith(PARENT_OFFENCE.code, 0)).thenReturn(
      createPrisonApiOffencesResponse(0, listOf()),
    )
    assertThatThrownBy {
      adminService.createEncouragementOffence(10)
    }.isInstanceOf(ValidationException::class.java)
      .hasMessage("No prison record was found for offence code ${PARENT_OFFENCE.code}")
  }

  private fun createPrisonApiOffencesResponse(
    totalPages: Int,
    content: List<uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence>,
  ): RestResponsePage<uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence> = RestResponsePage(
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

  companion object {
    private val PARENT_OFFENCE = Offence(
      id = 10,
      parentOffenceId = 10,
      code = "AABB010",
      description = "Desc",
      cjsTitle = "Cjs title",
      changedDate = LocalDateTime.now(),
      revisionId = 1,
      startDate = LocalDate.now(),
      endDate = LocalDate.parse("2028-01-01"),
      sdrsCache = OFFENCES_A,
    )
    private val CHILD_OFFENCE_ONE = PARENT_OFFENCE.copy(
      id = 11,
      code = "AABB011",
      parentOffenceId = 10,
    )
    private val CHILD_OFFENCE_TWO = PARENT_OFFENCE.copy(
      id = 12,
      code = "AABB012",
      parentOffenceId = 10,
    )
    private val NOMIS_OFFENCE_AABB010 = uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence(
      code = "AABB010",
      description = "Parent Desc",
      statuteCode = Statute(code = "A123", description = "Statute desc", activeFlag = "Y", legislatingBodyCode = "UK"),
      severityRanking = "99",
      activeFlag = "Y",
    )
  }
}
