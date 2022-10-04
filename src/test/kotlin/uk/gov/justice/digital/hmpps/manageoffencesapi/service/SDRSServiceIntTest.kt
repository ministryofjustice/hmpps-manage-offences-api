package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.FAIL
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.SUCCESS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.FULL_LOAD
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.UPDATE
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceSchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import java.time.LocalDate

class SDRSServiceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var sdrsService: SDRSService

  @Autowired
  lateinit var offenceRepository: OffenceRepository

  @Autowired
  lateinit var sdrsLoadResultRepository: SdrsLoadResultRepository

  @Autowired
  lateinit var sdrsLoadResultHistoryRepository: SdrsLoadResultHistoryRepository

  @Autowired
  lateinit var offenceSchedulePartRepository: OffenceSchedulePartRepository

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-schedule-and-offence-data.sql",
    "classpath:test_data/set-full-sdrs-load-toggle.sql",
  )
  fun `Perform a full load of offences retrieved from SDRS`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForAMultipleOffences()

    sdrsService.fullSynchroniseWithSdrs()

    val offences = offenceRepository.findAll()
    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()
    val offenceScheduleParts = offenceSchedulePartRepository.findAll()

    assertThat(offences)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdDate", "lastUpdatedDate")
      .isEqualTo(
        listOf(
          Offence(
            code = "XX99001",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            category = 195,
            subCategory = 99,
            changedDate = null
          ),
          Offence(
            code = "XX99002",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            category = 195,
            subCategory = 99,
            changedDate = null
          ),
        )
      )

    assertThat(statusRecords.size).isEqualTo(26)
    statusRecords.forEach {
      assertThat(it.status).isEqualTo(SUCCESS)
      assertThat(it.loadType).isEqualTo(FULL_LOAD)
    }

    assertThat(statusHistoryRecords.size).isEqualTo(26)
    statusHistoryRecords.forEach {
      assertThat(it.status).isEqualTo(SUCCESS)
      assertThat(it.loadType).isEqualTo(FULL_LOAD)
    }

    assertThat(offenceScheduleParts.size).isEqualTo(4)
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/set-success-all-load-results.sql",
  )
  fun `Update any offences that have changed`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetChangedOffencesForA()
    sdrsApiMockServer.stubControlTableRequest()
    ('A'..'Z').forEach { alphaChar ->
      prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
    }

    sdrsService.deltaSynchroniseWithSdrs()

    val offences = offenceRepository.findAll()
    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()

    assertThat(offences)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdDate", "lastUpdatedDate")
      .isEqualTo(
        listOf(
          Offence(
            code = "XX99001",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 99991,
            startDate = LocalDate.of(2014, 1, 1),
            endDate = null,
            changedDate = null
          ),
        )
      )

    statusRecords
      .filter { it.alphaChar == "A" || it.alphaChar == "B" }
      .forEach {
        assertThat(it.status).isEqualTo(SUCCESS)
        assertThat(it.loadType).isEqualTo(UPDATE)
      }

    statusHistoryRecords
      .filter { it.alphaChar == "A" || it.alphaChar == "B" }
      .forEach {
        assertThat(it.status).isEqualTo(SUCCESS)
        assertThat(it.loadType).isEqualTo(UPDATE)
      }
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/set-full-sdrs-load-toggle.sql",
    "classpath:test_data/set-success-all-load-results.sql",
  )
  fun `Handle SDRS-99918 as a success ie no offences exist for that cache (cache doesnt exist)`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForQHasNoCache()
    sdrsApiMockServer.stubControlTableRequest()
    sdrsService.fullSynchroniseWithSdrs()
    val offences = offenceRepository.findAll()
    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()

    assertThat(offences).isEmpty()

    assertThat(statusRecords.size).isEqualTo(26)
    statusRecords.forEach {
      assertThat(it.status).isEqualTo(SUCCESS)
      assertThat(it.loadType).isEqualTo(FULL_LOAD)
    }

    assertThat(statusHistoryRecords.size).isEqualTo(26)
    statusHistoryRecords.forEach {
      assertThat(it.status).isEqualTo(SUCCESS)
      assertThat(it.loadType).isEqualTo(FULL_LOAD)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/set-success-all-load-results.sql",
  )
  fun `Handle unexpected exception from SDRS - Bad JSON is returned from SDRS thus causing a generic exception`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetChangedOffencesForAHasBadJson()
    sdrsApiMockServer.stubControlTableRequest()
    sdrsService.deltaSynchroniseWithSdrs()

    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()

    assertThat(statusRecords.first { it.alphaChar == "A" }.status).isEqualTo(FAIL)
    assertThat(statusRecords.first { it.alphaChar == "A" }.loadType).isEqualTo(UPDATE)
    assertThat(statusHistoryRecords.first { it.alphaChar == "A" }.status).isEqualTo(FAIL)
    assertThat(statusHistoryRecords.first { it.alphaChar == "A" }.loadType).isEqualTo(UPDATE)
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/set-full-sdrs-load-toggle.sql",
  )
  fun `Perform a full load of offences when there are incohate offences`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesWithChildren()

    sdrsService.fullSynchroniseWithSdrs()

    val offences = offenceRepository.findAll()
    val parentOne = offences.first { it.code == "AX99001" }
    val parentTwo = offences.first { it.code == "AX99002" }

    assertThat(offences)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdDate", "lastUpdatedDate")
      .containsAnyElementsOf(
        listOf(
          Offence(
            code = "AX99001",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            category = 195,
            subCategory = 99,
            changedDate = null,
            parentOffenceId = null,
          ),
          Offence(
            code = "AX99001A",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            category = 195,
            subCategory = 99,
            changedDate = null,
            parentOffenceId = parentOne.id,
          ),
          Offence(
            code = "AX99001B",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            category = 195,
            subCategory = 99,
            changedDate = null,
            parentOffenceId = parentOne.id,
          ),
          Offence(
            code = "AX99002",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            category = 195,
            subCategory = 99,
            changedDate = null,
            parentOffenceId = null,
          ),
          Offence(
            code = "AX99002B",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            category = 195,
            subCategory = 99,
            changedDate = null,
            parentOffenceId = parentTwo.id,
          ),
        )
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-offence-to-schedule-history.sql",
    "classpath:test_data/set-delta-sync-toggle.sql",
  )
  fun `Send offence-to-schedule mappings to NOMIS `() {
    sdrsApiMockServer.stubControlTableRequest()
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    prisonApiMockServer.stubLinkOffence()
    prisonApiMockServer.stubUnlinkOffence()

    sdrsService.deltaSynchroniseWithSdrs()
  }
}
