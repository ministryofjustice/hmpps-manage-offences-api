package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ImportCsvResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.resource.ScheduleController.Companion.log
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.ScheduleOffenceService
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.ScheduleService
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

@RestController
@RequestMapping("/schedule/{scheduleId}/part")
class SchedulePartController(
  private val scheduleService: ScheduleService,
  private val scheduleOffenceService: ScheduleOffenceService,
) {

  @PostMapping(value = ["/create"])
  @PreAuthorize("hasRole('ROLE_UPDATE_OFFENCE_SCHEDULES')")
  @Operation(
    summary = "Create a schedule part",
  )
  fun createSchedulePart(@PathVariable scheduleId: Long, @RequestBody schedulePart: SchedulePart) {
    log.info("Request received to create schedule part with number {}", schedulePart.partNumber)
    scheduleService.createSchedulePart(scheduleId, schedulePart)
  }

  @GetMapping("/{schedulePartId}/offences/import")
  @ResponseBody
  fun getImportCsv(): ResponseEntity<InputStreamResource> {
    val csvContent = ScheduleOffenceService.csvHeaders.joinToString(",") + "\n"

    val inputStream = ByteArrayInputStream(csvContent.toByteArray())

    return ResponseEntity.ok()
      .header("Content-Disposition", "attachment; filename=schedule_part_offences_template.csv")
      .contentType(MediaType.parseMediaType("text/csv"))
      .body(InputStreamResource(inputStream))
  }

  @PostMapping(value = ["/{schedulePartId}/offences/import"])
  @PreAuthorize("hasRole('ROLE_UPDATE_OFFENCE_SCHEDULES')")
  @ResponseBody
  @Operation(
    summary = "Upload multiple offences using CSV file",
    description = "Use formatted CSV file to upload multiple offences.",
  )
  fun importCSVFile(
    @RequestParam("file") file: MultipartFile,
    @PathVariable("schedulePartId") schedulePartId: Long,
  ): ImportCsvResult {
    if (file.isEmpty) {
      return ImportCsvResult(success = false, message = "Invalid file", errors = listOf("CSV file is empty"))
    }

    val schedulePart = scheduleOffenceService.getSchedulePart(schedulePartId) ?: return ImportCsvResult(
      success = false,
      message = "Invalid Schedule Part",
      errors = listOf("Schedule Part $schedulePartId does not exist"),
    )

    return scheduleOffenceService.import(BufferedReader(InputStreamReader(file.inputStream)), schedulePart)
  }
}
