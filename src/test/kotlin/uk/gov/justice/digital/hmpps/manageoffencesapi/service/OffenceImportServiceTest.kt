package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.CustodialIndicator
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ImportCsvResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import java.io.BufferedReader
import java.time.LocalDate

class OffenceImportServiceTest {
  private val offenceRepository = mock<OffenceRepository>()
  private val offenceScheduleMappingRepository = mock<OffenceScheduleMappingRepository>()
  private val schedulePartRepository = mock<SchedulePartRepository>()
  private val offenceImportService = OffenceImportService(
    offenceRepository,
    offenceScheduleMappingRepository,
    schedulePartRepository,
  )

  @Test
  fun `Validate CSV should return correct list of errors`() {
    val bufferedReader = Mockito.mock<BufferedReader>()

    val csvLines = listOf(
      "code,description,offenceType,revisionId,startDate,endDate,legislation,maxPeriodIsLife,maxPeriodOfIndictmentYears,maxPeriodOfIndictmentMonths,maxPeriodOfIndictmentWeeks,maxPeriodOfIndictmentDays,custodialIndicator",
      "CT12345,Test offence One,MO,123,2020-01-01,2030-01-01,leg,false,5,6,7,8,NO", // valid line
      "CT12346,Test offence One,MO,123,20-01-01,23-01-01,leg,false,5,6,7,8,NO", // invalid dates
      ",Test offence One,MO,123,2020-01-01,2030-01-01,leg,false,5,6,7,8,NO", // missing offence code
    )

    whenever(bufferedReader.readLine()).thenReturn(
      csvLines[0],
      *csvLines.drop(1).toTypedArray(),
      null,
    )

    whenever(offenceRepository.returnCodesThatExist(any())).thenReturn(emptyList())

    val validationResults = offenceImportService.validateCsv(bufferedReader)

    assertThat(validationResults).isEqualTo(
      ImportCsvResult(
        success = false,
        message = "Invalid offence data",
        errors = listOf(
          "Line 3 [startDate] Expected date (yyyy-MM-dd)",
          "Line 3 [endDate] Expected date (yyyy-MM-dd)",
          "Line 4 [code] must be provided",
        ),
      ),
    )
  }

  @Test
  fun `Should return Offence entities`() {
    val bufferedReader = Mockito.mock<BufferedReader>()

    val csvLines = listOf(
      "code,description,offenceType,revisionId,startDate,endDate,legislation,maxPeriodIsLife,maxPeriodOfIndictmentYears,maxPeriodOfIndictmentMonths,maxPeriodOfIndictmentWeeks,maxPeriodOfIndictmentDays,custodialIndicator",
      "CT12345,Test Offence One,DO,123,2020-01-01,2030-01-01,leg,false,5,6,7,8,NO",
      "DT12345,Test Offence Two,DO,124,2020-02-01,2030-02-01,leg2,true,3,4,5,6,YES",
      "ET12345,Test Offence Three,,125,2020-03-01,,,,,,,,,",
    )

    whenever(bufferedReader.readLine()).thenReturn(
      csvLines[0],
      *csvLines.drop(1).toTypedArray(),
      null,
    )

    val offences = offenceImportService.parseCsv(bufferedReader)
    assertThat(offences.size == 3)
    assertThat(offences).isEqualTo(
      listOf(
        Offence(
          code = "CT12345",
          description = "Test Offence One",
          cjsTitle = "Test Offence One",
          offenceType = "DO",
          revisionId = 123,
          startDate = LocalDate.of(2020, 1, 1),
          endDate = LocalDate.of(2030, 1, 1),
          legislation = "leg",
          maxPeriodIsLife = false,
          maxPeriodOfIndictmentYears = 5,
          maxPeriodOfIndictmentMonths = 6,
          maxPeriodOfIndictmentWeeks = 7,
          maxPeriodOfIndictmentDays = 8,
          custodialIndicator = CustodialIndicator.NO,
          sdrsCache = SdrsCache.OFFENCES_C,
          createdDate = offences[0].createdDate,
          changedDate = offences[0].changedDate,
          lastUpdatedDate = offences[0].lastUpdatedDate,
        ),
        Offence(
          code = "DT12345",
          description = "Test Offence Two",
          cjsTitle = "Test Offence Two",
          offenceType = "DO",
          revisionId = 124,
          startDate = LocalDate.of(2020, 2, 1),
          endDate = LocalDate.of(2030, 2, 1),
          legislation = "leg2",
          maxPeriodIsLife = true,
          maxPeriodOfIndictmentYears = 3,
          maxPeriodOfIndictmentMonths = 4,
          maxPeriodOfIndictmentWeeks = 5,
          maxPeriodOfIndictmentDays = 6,
          custodialIndicator = CustodialIndicator.YES,
          sdrsCache = SdrsCache.OFFENCES_D,
          createdDate = offences[1].createdDate,
          changedDate = offences[1].changedDate,
          lastUpdatedDate = offences[1].lastUpdatedDate,
        ),
        Offence(
          code = "ET12345",
          description = "Test Offence Three",
          cjsTitle = "Test Offence Three",
          revisionId = 125,
          startDate = LocalDate.of(2020, 3, 1),
          sdrsCache = SdrsCache.OFFENCES_E,
          createdDate = offences[2].createdDate,
          changedDate = offences[2].changedDate,
          lastUpdatedDate = offences[2].lastUpdatedDate,
        ),
      ),
    )
  }
}
