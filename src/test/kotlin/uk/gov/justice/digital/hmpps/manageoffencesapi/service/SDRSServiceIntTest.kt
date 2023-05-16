package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.LegacySdrsHoCodeMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.FAIL
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.SUCCESS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.FULL_LOAD
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.UPDATE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache.OFFENCES_A
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache.OFFENCES_B
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.LegacySdrsHoCodeMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import java.time.LocalDate
import java.time.LocalDateTime

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
  lateinit var offenceScheduleMappingRepository: OffenceScheduleMappingRepository

  @Autowired
  lateinit var legacySdrsHoCodeMappingRepository: LegacySdrsHoCodeMappingRepository

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/insert-schedule-and-offence-data.sql",
    "classpath:test_data/set-full-sdrs-load-toggle.sql",
  )
  fun `Perform a full load of offences retrieved from SDRS`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForAMultipleOffences()

    sdrsService.fullSynchroniseWithSdrs()

    val offences = offenceRepository.findAll()
    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()
    val offenceScheduleParts = offenceScheduleMappingRepository.findAll()
    val legacyRecords = legacySdrsHoCodeMappingRepository.findAll()

    assertThat(offences)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdDate", "lastUpdatedDate", "changedDate")
      .isEqualTo(
        listOf(
          Offence(
            code = "XX99001",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            offenceType = "CI",
            changedDate = LocalDateTime.now(),
            sdrsCache = OFFENCES_A,
          ),
          Offence(
            code = "XX99002",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            changedDate = LocalDateTime.now(),
            sdrsCache = OFFENCES_A,
          ),
        ),
      )

    assertThat(statusRecords.size).isEqualTo(28)
    statusRecords.forEach {
      assertThat(it.status).isEqualTo(SUCCESS)
      assertThat(it.loadType).isEqualTo(FULL_LOAD)
    }

    assertThat(statusHistoryRecords.size).isEqualTo(28)
    statusHistoryRecords.forEach {
      assertThat(it.status).isEqualTo(SUCCESS)
      assertThat(it.loadType).isEqualTo(FULL_LOAD)
    }

    assertThat(offenceScheduleParts).hasSize(1)

    assertThat(legacyRecords)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdDate", "lastUpdatedDate", "changedDate")
      .containsExactlyInAnyOrderElementsOf(
        listOf(
          LegacySdrsHoCodeMapping(
            offenceCode = "XX99001",
            category = 195,
            subCategory = 99,
          ),
          LegacySdrsHoCodeMapping(
            offenceCode = "XX99002",
            category = 195,
            subCategory = 99,
          ),
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/set-success-all-load-results.sql",
  )
  fun `Update any offences that have changed`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetChangedOffencesForA()
    sdrsApiMockServer.stubControlTableRequest()
    ('A'..'Z').forEach { alphaChar ->
      prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
    }
    prisonApiMockServer.stubCreateStatute()
    prisonApiMockServer.stubCreateOffence()

    sdrsService.deltaSynchroniseWithSdrs()

    val offences = offenceRepository.findAll()
    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()

    assertThat(offences)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdDate", "lastUpdatedDate", "changedDate")
      .isEqualTo(
        listOf(
          Offence(
            code = "XX99001",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 99991,
            startDate = LocalDate.of(2014, 1, 1),
            endDate = null,
            changedDate = LocalDateTime.now(),
            sdrsCache = OFFENCES_A,
          ),
        ),
      )

    statusRecords
      .filter { it.cache == OFFENCES_A || it.cache == OFFENCES_B }
      .forEach {
        assertThat(it.status).isEqualTo(SUCCESS)
        assertThat(it.loadType).isEqualTo(UPDATE)
      }

    statusHistoryRecords
      .filter { it.cache == OFFENCES_A || it.cache == OFFENCES_B }
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
    sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForQHasNoCache()
    sdrsApiMockServer.stubControlTableRequest()
    sdrsService.fullSynchroniseWithSdrs()
    val offences = offenceRepository.findAll()
    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()

    assertThat(offences).isEmpty()

    assertThat(statusRecords.size).isEqualTo(28)
    statusRecords.forEach {
      assertThat(it.status).isEqualTo(SUCCESS)
      assertThat(it.loadType).isEqualTo(FULL_LOAD)
    }

    assertThat(statusHistoryRecords.size).isEqualTo(28)
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
    sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetChangedOffencesForAHasBadJson()
    sdrsApiMockServer.stubControlTableRequest()
    sdrsService.deltaSynchroniseWithSdrs()

    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()

    assertThat(statusRecords.first { it.cache == OFFENCES_A }.status).isEqualTo(FAIL)
    assertThat(statusRecords.first { it.cache == OFFENCES_A }.lastSuccessfulLoadDate?.toLocalDate()).isEqualTo(LocalDate.of(2022, 4, 19))
    assertThat(statusRecords.first { it.cache == OFFENCES_A }.loadType).isEqualTo(UPDATE)
    assertThat(statusHistoryRecords.first { it.cache == OFFENCES_A }.status).isEqualTo(FAIL)
    assertThat(statusHistoryRecords.first { it.cache == OFFENCES_A }.loadType).isEqualTo(UPDATE)
  }

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
    "classpath:test_data/set-full-sdrs-load-toggle.sql",
    "classpath:test_data/insert-offence-data-with-children.sql",
  )
  fun `Perform a full load of offences when there are incohate offences`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesWithChildren()

    sdrsService.fullSynchroniseWithSdrs()

    val offences = offenceRepository.findAll()
    val parentOne = offences.first { it.code == "AX99001" }
    val parentTwo = offences.first { it.code == "AX99002" }
    val legacyRecords = legacySdrsHoCodeMappingRepository.findAll()

    assertThat(offences)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdDate", "lastUpdatedDate", "changedDate")
      .containsExactlyInAnyOrderElementsOf(
        listOf(
          Offence(
            code = "AX99001",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 354116,
            startDate = LocalDate.of(2005, 9, 2),
            endDate = LocalDate.of(2005, 9, 3),
            changedDate = LocalDateTime.now(),
            sdrsCache = OFFENCES_A,
            parentOffenceId = null,
          ),
          Offence(
            code = "AX99001A",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            changedDate = LocalDateTime.now(), sdrsCache = OFFENCES_A,
            parentOffenceId = parentOne.id,
          ),
          Offence(
            code = "AX99001B",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            changedDate = LocalDateTime.now(), sdrsCache = OFFENCES_A,
            parentOffenceId = parentOne.id,
          ),
          Offence(
            code = "AX99002",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            changedDate = LocalDateTime.now(), sdrsCache = OFFENCES_A,
            parentOffenceId = null,
          ),
          Offence(
            code = "AX99002B",
            description = "EMPTY TEMPLATE FOR USE WHERE A STANDARD OFFENCE WORDING IS NOT AVAILABLE",
            cjsTitle = null,
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            changedDate = LocalDateTime.now(), sdrsCache = OFFENCES_A,
            parentOffenceId = parentTwo.id,
          ),
        ),
      )

    assertThat(legacyRecords)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdDate", "lastUpdatedDate", "changedDate")
      .containsExactlyInAnyOrderElementsOf(
        listOf(
          LegacySdrsHoCodeMapping(
            offenceCode = "AX99001",
            category = 195,
            subCategory = 99,
          ),
          LegacySdrsHoCodeMapping(
            offenceCode = "AX99001A",
            category = 195,
            subCategory = 99,
          ),
          LegacySdrsHoCodeMapping(
            offenceCode = "AX99001B",
            category = 195,
            subCategory = 99,
          ),
          LegacySdrsHoCodeMapping(
            offenceCode = "AX99002",
            category = 195,
            subCategory = 99,
          ),
          LegacySdrsHoCodeMapping(
            offenceCode = "AX99002B",
            category = 195,
            subCategory = 99,
          ),
        ),
      )
  }
}
