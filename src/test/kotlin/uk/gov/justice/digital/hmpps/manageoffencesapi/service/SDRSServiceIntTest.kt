package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.HomeOfficeCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.LegacySdrsHoCodeMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceToSyncWithNomis
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.CustodialIndicator.EITHER
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.CustodialIndicator.NO
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.CustodialIndicator.YES
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.FAIL
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.SUCCESS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.FULL_LOAD
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.UPDATE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisSyncType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache.GET_MOJ_OFFENCE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache.OFFENCES_A
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache.OFFENCES_B
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.LegacySdrsHoCodeMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceToSyncWithNomisRepository
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

  @Autowired
  lateinit var offenceToSyncWithNomisRepository: OffenceToSyncWithNomisRepository

  @Nested
  inner class FullSyncTests {
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
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
          "lastUpdatedDate",
          "changedDate",
        )
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
              custodialIndicator = YES,
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
              custodialIndicator = NO,
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
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
          "lastUpdatedDate",
          "changedDate",
        )
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
      "classpath:test_data/set-full-sdrs-load-toggle.sql",
      "classpath:test_data/insert-offence-data-with-children.sql",
    )
    fun `Perform a full load of offences when there are inchoate offences`() {
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
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
          "lastUpdatedDate",
          "changedDate",
        )
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
              custodialIndicator = NO,

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
              custodialIndicator = YES,
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
              custodialIndicator = YES,
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
              custodialIndicator = NO,
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
              custodialIndicator = EITHER,
            ),
          ),
        )

      assertThat(legacyRecords)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
          "lastUpdatedDate",
          "changedDate",
        )
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
      "classpath:test_data/set-full-sdrs-load-toggle.sql",
      "classpath:test_data/insert-offence-data-with-children.sql",
    )
    fun `Perform a full load of offences when there are future end dated offences in a primary cache`() {
      sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
      sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetAllOffencesWithFutureEndDated()

      sdrsService.fullSynchroniseWithSdrs()

      val offencesToSyncWithNomis = offenceToSyncWithNomisRepository.findAll()

      assertThat(offencesToSyncWithNomis)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
        )
        .isEqualTo(
          listOf(
            OffenceToSyncWithNomis(
              offenceCode = "AX99001",
              nomisSyncType = NomisSyncType.FUTURE_END_DATED,
            ),
            OffenceToSyncWithNomis(
              offenceCode = "AX99002",
              nomisSyncType = NomisSyncType.FUTURE_END_DATED,
            ),
          ),
        )
    }

    @Test
    @Sql(
      "classpath:test_data/reset-all-data.sql",
      "classpath:test_data/set-full-sdrs-load-toggle.sql",
      "classpath:test_data/insert-offence-data-with-children.sql",
    )
    fun `Perform a full load of offences when there are future end dated offences in a secondary cache`() {
      sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
      sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetMojSecondaryOffencesWithFutureEndDated()

      sdrsService.fullSynchroniseWithSdrs()

      val offencesToSyncWithNomis = offenceToSyncWithNomisRepository.findAll()

      assertThat(offencesToSyncWithNomis)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
        )
        .isEqualTo(
          listOf(
            OffenceToSyncWithNomis(
              offenceCode = "AX99001",
              nomisSyncType = NomisSyncType.FUTURE_END_DATED,
            ),
            OffenceToSyncWithNomis(
              offenceCode = "AX99002",
              nomisSyncType = NomisSyncType.FUTURE_END_DATED,
            ),
          ),
        )
    }
  }

  @Nested
  inner class DeltaSyncTests {
    @Test
    @Sql(
      "classpath:test_data/reset-all-data.sql",
      "classpath:test_data/set-success-all-load-results.sql",
    )
    fun `Update any offences that have changed during nightly delta sync`() {
      sdrsApiMockServer.resetAll()
      sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
      sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetChangedOffencesForA()
      sdrsApiMockServer.stubControlTableRequestForNightlySync()
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
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
          "lastUpdatedDate",
          "changedDate",
        )
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
              custodialIndicator = EITHER,
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
      "classpath:test_data/set-success-all-load-results.sql",
    )
    fun `Update any offences that have changed`() {
      sdrsApiMockServer.resetAll()
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
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
          "lastUpdatedDate",
          "changedDate",
        )
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
              custodialIndicator = EITHER,
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
      "classpath:test_data/set-success-all-load-results.sql",
    )
    fun `Handle unexpected exception from SDRS - Bad JSON is returned from SDRS thus causing a generic exception`() {
      sdrsApiMockServer.resetAll()
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
      "classpath:test_data/set-success-all-load-results.sql",
    )
    fun `Perform delta load with offences that end in the future`() {
      sdrsApiMockServer.resetAll()
      sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
      sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetChangedOffencesForAFutureEndDated()
      sdrsApiMockServer.stubControlTableRequest()
      ('A'..'Z').forEach { alphaChar ->
        prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
      }
      prisonApiMockServer.stubCreateStatute()
      prisonApiMockServer.stubCreateOffence()

      sdrsService.deltaSynchroniseWithSdrs()

      val offencesToSyncWithNomis = offenceToSyncWithNomisRepository.findAll()

      assertThat(offencesToSyncWithNomis)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
        )
        .isEqualTo(
          listOf(
            OffenceToSyncWithNomis(
              offenceCode = "XX99001",
              nomisSyncType = NomisSyncType.FUTURE_END_DATED,
            ),
          ),
        )
    }

    @Test
    @Sql(
      "classpath:test_data/reset-all-data.sql",
      "classpath:test_data/insert-future-end-dated-offence-to-sync-with-nomis.sql",
    )
    fun `Perform delta load with offences that end in the future - but the offence has already been marked to be future end dated`() {
      sdrsApiMockServer.resetAll()
      sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
      sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetChangedOffencesForAFutureEndDated()
      sdrsApiMockServer.stubControlTableRequest()
      ('A'..'Z').forEach { alphaChar ->
        prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
      }
      prisonApiMockServer.stubCreateStatute()
      prisonApiMockServer.stubCreateOffence()

      sdrsService.deltaSynchroniseWithSdrs()

      val offencesToSyncWithNomis = offenceToSyncWithNomisRepository.findAll()

      assertThat(offencesToSyncWithNomis)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
        )
        .isEqualTo(
          listOf(
            OffenceToSyncWithNomis(
              offenceCode = "XX99001",
              nomisSyncType = NomisSyncType.FUTURE_END_DATED,
            ),
          ),
        )
    }

    @Test
    @Sql(
      "classpath:test_data/reset-all-data.sql",
      "classpath:test_data/set-success-all-load-results.sql",
      "classpath:test_data/insert-offence-data-with-ho-code.sql",
    )
    fun `Perform delta load where the ho-code is different to the one stored - ho code should not get updated, everything else should`() {
      sdrsApiMockServer.resetAll()
      sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
      sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetMojRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetChangedOffencesForAWithDifferentHoCodes()
      sdrsApiMockServer.stubControlTableRequest()
      ('A'..'Z').forEach { alphaChar ->
        prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
      }
      prisonApiMockServer.stubCreateStatute()
      prisonApiMockServer.stubCreateOffence()

      sdrsService.deltaSynchroniseWithSdrs()

      val offences = offenceRepository.findAll()
      assertThat(offences)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
          "lastUpdatedDate",
          "changedDate",
        )
        .isEqualTo(
          listOf(
            Offence(
              code = "HO06999",
              description = "Brought before the court UPDATED",
              cjsTitle = "Brought before the court UPDATED",
              revisionId = 99990,
              startDate = LocalDate.of(2009, 11, 2),
              legislation = "NEW ACT UPDATED",
              category = 1,
              subCategory = 13,
              changedDate = LocalDateTime.now(),
              maxPeriodIsLife = null,
              sdrsCache = OFFENCES_A,
              homeOfficeCode = HomeOfficeCode(
                id = "00113",
                category = 1,
                subCategory = 13,
                description = "Random HO offence 1",
              ),
              custodialIndicator = EITHER,
            ),
          ),
        )
    }

    @Test
    @Sql(
      "classpath:test_data/reset-all-data.sql",
      "classpath:test_data/set-success-all-load-results.sql",
      "classpath:test_data/insert-single-offence.sql",
    )
    fun `Perform delta load where the offence exists in multiple caches (primary and secondary)- only the later 'start date' one  is used (primary)`() {
      sdrsApiMockServer.resetAll()
      sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
      sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetChangedOffencesForAWithSingleOffence()
      sdrsApiMockServer.stubGetMojSecondaryOffencesWithDuplicatedOlderOffence()
      sdrsApiMockServer.stubControlTableRequestWithSecondaryCache()
      ('A'..'Z').forEach { alphaChar ->
        prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
      }
      prisonApiMockServer.stubCreateStatute()
      prisonApiMockServer.stubCreateOffence()

      sdrsService.deltaSynchroniseWithSdrs()

      val offences = offenceRepository.findAll()
      assertThat(offences)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
          "lastUpdatedDate",
          "changedDate",
        )
        .isEqualTo(
          listOf(
            Offence(
              code = "AO06999",
              description = "Cache A UPDATED",
              cjsTitle = "Cache A UPDATED",
              revisionId = 99990,
              startDate = LocalDate.of(2009, 11, 3),
              legislation = "NEW ACT",
              changedDate = LocalDateTime.now(),
              sdrsCache = OFFENCES_A,
              maxPeriodIsLife = null,
              custodialIndicator = NO,
            ),
          ),
        )
    }

    @Test
    @Sql(
      "classpath:test_data/reset-all-data.sql",
      "classpath:test_data/set-success-all-load-results.sql",
      "classpath:test_data/insert-single-offence.sql",
    )
    fun `Perform delta load where the offence exists in multiple caches (primary and secondary)- only the later 'start date' one  is used (secondary)`() {
      sdrsApiMockServer.resetAll()
      sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
      sdrsApiMockServer.stubGetApplicationRequestReturnEmptyArray()
      sdrsApiMockServer.stubGetChangedOffencesForAWithSingleOffence()
      sdrsApiMockServer.stubGetMojSecondaryOffencesWithDuplicatedNewerOffence()
      sdrsApiMockServer.stubControlTableRequestWithSecondaryCache()
      ('A'..'Z').forEach { alphaChar ->
        prisonApiMockServer.stubFindByOffenceCodeStartsWithReturnsNothing(alphaChar)
      }
      prisonApiMockServer.stubCreateStatute()
      prisonApiMockServer.stubCreateOffence()

      sdrsService.deltaSynchroniseWithSdrs()

      val offences = offenceRepository.findAll()
      assertThat(offences)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
          "id",
          "createdDate",
          "lastUpdatedDate",
          "changedDate",
        )
        .isEqualTo(
          listOf(
            Offence(
              code = "AO06999",
              description = "Secondary offence UPDATED",
              cjsTitle = "Secondary offence UPDATED",
              revisionId = 99990,
              startDate = LocalDate.of(2009, 11, 4),
              legislation = "NEW ACT",
              changedDate = LocalDateTime.now(),
              sdrsCache = GET_MOJ_OFFENCE,
              maxPeriodIsLife = null,
              custodialIndicator = EITHER,
            ),
          ),
        )
    }
  }
}
