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

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql"
  )
  fun `Perform a full load of offences retrieved from SDRS`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForA()
    sdrsService.synchroniseWithSdrs()
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
            revisionId = 410082,
            startDate = LocalDate.of(2013, 3, 1),
            endDate = LocalDate.of(2013, 3, 2),
            homeOfficeStatsCode = "195/99",
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
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql",
    "classpath:test_data/insert-sdrs-load-result.sql",
  )
  fun `Update any offences that have changed`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetChangedOffencesForA()
    sdrsApiMockServer.stubControlTableRequest()
    sdrsService.synchroniseWithSdrs()
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
    "classpath:test_data/clear-all-data.sql",
  )
  fun `Handle SDRS-99918 as a success ie no offences exist for that cache (cache doesnt exit)`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetAllOffencesForQHasNoCache()
    sdrsApiMockServer.stubControlTableRequest()
    sdrsService.synchroniseWithSdrs()
    val offences = offenceRepository.findAll()
    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()

    assertThat(offences).isEmpty()

    assertThat(statusRecords.size).isEqualTo(26)
    statusRecords.forEach {
      println("Alpha char = " + it.alphaChar)
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
    "classpath:test_data/clear-all-data.sql",
    "classpath:test_data/insert-sdrs-load-result.sql",
  )
  fun `Handle unexpected exception from SDRS - Bad JSON is returned from SDRS thus causing a generic exception`() {
    sdrsApiMockServer.stubGetAllOffencesReturnEmptyArray()
    sdrsApiMockServer.stubGetChangedOffencesForAHasBadJson()
    sdrsApiMockServer.stubControlTableRequest()
    sdrsService.synchroniseWithSdrs()

    val statusRecords = sdrsLoadResultRepository.findAll()
    val statusHistoryRecords = sdrsLoadResultHistoryRepository.findAll()

    assertThat(statusRecords.first { it.alphaChar == "A" }.status).isEqualTo(FAIL)
    assertThat(statusRecords.first { it.alphaChar == "A" }.loadType).isEqualTo(UPDATE)
    assertThat(statusHistoryRecords.first { it.alphaChar == "A" }.status).isEqualTo(FAIL)
    assertThat(statusHistoryRecords.first { it.alphaChar == "A" }.loadType).isEqualTo(UPDATE)
  }
}
