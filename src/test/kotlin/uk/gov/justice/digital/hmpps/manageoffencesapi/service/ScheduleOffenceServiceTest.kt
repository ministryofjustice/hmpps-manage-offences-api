package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ImportCsvResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import java.io.BufferedReader
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleOffenceServiceTest {
  private val offenceRepository = mock<OffenceRepository>()
  private val offenceScheduleMappingRepository = mock<OffenceScheduleMappingRepository>()
  private val schedulePartRepository = mock<SchedulePartRepository>()
  private val scheduleOffenceUpdateService = ScheduleOffenceUpdateService(offenceScheduleMappingRepository)
  private val scheduleOffenceService = ScheduleOffenceService(
    offenceRepository,
    schedulePartRepository,
    scheduleOffenceUpdateService,
  )

  @Test
  fun `Persist should return error where no offences are found`() {
    val bufferedReader = Mockito.mock<BufferedReader>()

    val csvLines = listOf(
      "code, lineReference, legislationText, paragraphNumber, paragraphTitle",
      "CT1234, line1, leg1, para1, title1",
      "CT1235, line2, leg2, para2, title2",
    )

    whenever(bufferedReader.readLine()).thenReturn(
      csvLines[0],
      *csvLines.drop(1).toTypedArray(),
      null,
    )

    whenever(offenceRepository.findByCodeIn(any())).thenReturn(emptyList())

    val schedulePart = SchedulePart(
      id = -1,
      schedule = Schedule(
        id = 1,
        code = "ABC",
        act = "act1",
        url = null,
      ),
      partNumber = 1,
    )

    val result = scheduleOffenceService.import(bufferedReader, schedulePart)

    assertThat(result).isEqualTo(
      ImportCsvResult(
        success = false,
        message = "No offences valid offences",
        errors = listOf(
          "No offences codes found within the CSV match any actual offences",
        ),
      ),
    )
  }

  @Test
  fun `Persist should update DB where no errors are found`() {
    val bufferedReader = Mockito.mock<BufferedReader>()

    val csvLines = listOf(
      "code, lineReference, legislationText, paragraphNumber, paragraphTitle",
      "CT1234, line1, leg1, para1, title1",
      "CT1235, line2, leg2, para2, title2",
    )

    whenever(bufferedReader.readLine()).thenReturn(
      csvLines[0],
      *csvLines.drop(1).toTypedArray(),
      null,
    )

    val offence1 = Offence(
      id = 1,
      code = "CT1234",
      revisionId = 111,
      startDate = LocalDate.of(2021, 1, 1),
      sdrsCache = SdrsCache.OFFENCES_C,
      changedDate = LocalDateTime.now(),
    )

    val offence2 = Offence(
      id = 2,
      code = "CT1235",
      revisionId = 222,
      startDate = LocalDate.of(2021, 1, 1),
      sdrsCache = SdrsCache.OFFENCES_C,
      changedDate = LocalDateTime.now(),
    )

    whenever(offenceRepository.findByCodeIn(any())).thenReturn(
      listOf(offence1, offence2),
    )

    whenever(offenceScheduleMappingRepository.saveAll<OffenceScheduleMapping>(any())).thenReturn(emptyList())

    val schedulePart = SchedulePart(
      id = -1,
      schedule = Schedule(
        id = 1,
        code = "ABC",
        act = "Act1",
        url = null,
      ),
      partNumber = 1,
    )

    val result = scheduleOffenceService.import(bufferedReader, schedulePart)

    verify(offenceScheduleMappingRepository, times(1)).saveAll<OffenceScheduleMapping>(any())
    verify(offenceScheduleMappingRepository).saveAll(
      listOf(
        OffenceScheduleMapping(
          schedulePart = schedulePart,
          offence = offence1,
          lineReference = "line1",
          legislationText = "leg1",
          paragraphNumber = "para1",
          paragraphTitle = "title1",
        ),
        OffenceScheduleMapping(
          schedulePart = schedulePart,
          offence = offence2,
          lineReference = "line2",
          legislationText = "leg2",
          paragraphNumber = "para2",
          paragraphTitle = "title2",
        ),
      ),
    )

    assertThat(result).isEqualTo(
      ImportCsvResult(
        success = true,
        message = "Imported 2 offences to Schedule Act1 part 1",
        errors = emptyList(),
      ),
    )
  }

  @Test
  fun `Persist should update DB where optional values are null`() {
    val bufferedReader = Mockito.mock<BufferedReader>()

    val csvLines = listOf(
      "code, lineReference, legislationText, paragraphNumber, paragraphTitle",
      "CT1234,,,,",
    )

    whenever(bufferedReader.readLine()).thenReturn(
      csvLines[0],
      *csvLines.drop(1).toTypedArray(),
      null,
    )

    val offence = Offence(
      id = 1,
      code = "CT1234",
      revisionId = 111,
      startDate = LocalDate.of(2021, 1, 1),
      sdrsCache = SdrsCache.OFFENCES_C,
      changedDate = LocalDateTime.now(),
    )

    whenever(offenceRepository.findByCodeIn(any())).thenReturn(
      listOf(offence),
    )

    whenever(offenceScheduleMappingRepository.saveAll<OffenceScheduleMapping>(any())).thenReturn(emptyList())

    val schedulePart = SchedulePart(
      id = -1,
      schedule = Schedule(
        id = 1,
        code = "ABC",
        act = "Act1",
        url = null,
      ),
      partNumber = 1,
    )

    val result = scheduleOffenceService.import(bufferedReader, schedulePart)

    verify(offenceScheduleMappingRepository, times(1)).saveAll<OffenceScheduleMapping>(any())
    verify(offenceScheduleMappingRepository).saveAll(
      listOf(
        OffenceScheduleMapping(
          schedulePart = schedulePart,
          offence = offence,
        ),
      ),
    )

    assertThat(result).isEqualTo(
      ImportCsvResult(
        success = true,
        message = "Imported 1 offences to Schedule Act1 part 1",
        errors = emptyList(),
      ),
    )
  }

  @Test
  fun `Persist should update DB where valid offence exists`() {
    val bufferedReader = Mockito.mock<BufferedReader>()

    val csvLines = listOf(
      "code, lineReference, legislationText, paragraphNumber, paragraphTitle",
      "CT1234, line1, leg1, para1, title1",
      "CT1235, line2, leg2, para2, title2",
    )

    whenever(bufferedReader.readLine()).thenReturn(
      csvLines[0],
      *csvLines.drop(1).toTypedArray(),
      null,
    )

    whenever(offenceRepository.findByCodeIn(any())).thenReturn(
      listOf(
        Offence(
          id = 2,
          code = "CT1235",
          revisionId = 222,
          startDate = LocalDate.of(2021, 1, 1),
          sdrsCache = SdrsCache.OFFENCES_C,
          changedDate = LocalDateTime.now(),
        ),
      ),
    )

    whenever(offenceScheduleMappingRepository.saveAll<OffenceScheduleMapping>(any())).thenReturn(emptyList())

    val schedulePart = SchedulePart(
      id = -1,
      schedule = Schedule(
        id = 1,
        code = "ABC",
        act = "Act1",
        url = null,
      ),
      partNumber = 1,
    )

    val result = scheduleOffenceService.import(bufferedReader, schedulePart)

    assertThat(result).isEqualTo(
      ImportCsvResult(
        success = true,
        message = "Imported 1 offences to Schedule Act1 part 1",
        errors = emptyList(),
      ),
    )
  }
}
