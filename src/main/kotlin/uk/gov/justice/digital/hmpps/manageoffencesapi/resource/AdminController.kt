package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ImportCsvResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.AdminService
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.OffenceImportService
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

@RestController
@RequestMapping("/admin", produces = [MediaType.APPLICATION_JSON_VALUE])
class AdminController(
  private val adminService: AdminService,
  private val offenceImportService: OffenceImportService,
) {
  @PutMapping(value = ["/toggle-feature"])
  @PreAuthorize("hasRole('ROLE_MANAGE_OFFENCES_ADMIN')")
  @Operation(
    summary = "Enable / disable a feature",
  )
  fun toggleFeature(@RequestBody featureToggles: List<FeatureToggle>) {
    log.info("Request received to toggle features")
    return adminService.toggleFeature(featureToggles)
  }

  @GetMapping(value = ["/feature-toggles"])
  @Operation(
    summary = "Get values of all feature toggles",
  )
  fun getAllToggles(): List<FeatureToggle> {
    log.info("Request received to get values of all feature toggles")
    return adminService.getAllToggles()
  }

  @PostMapping(value = ["/nomis/offences/reactivate"])
  @PreAuthorize("hasRole('ROLE_NOMIS_OFFENCE_ACTIVATOR')")
  @Operation(
    summary = "Reactivate offences in NOMIS",
    description = "Reactivate offences in NOMIS, only to be used for offences that are end dated but NOMIS need them to be reactivated",
  )
  fun reactivateNomisOffence(@RequestBody offenceIds: List<Long>) {
    log.info("Request received to reactivate offences in nomis")
    return adminService.reactivateNomisOffence(offenceIds)
  }

  @PostMapping(value = ["/nomis/offences/deactivate"])
  @PreAuthorize("hasRole('ROLE_NOMIS_OFFENCE_ACTIVATOR')")
  @Operation(
    summary = "Deactivate offences in NOMIS",
    description = "Deactivate offences in NOMIS, only to be used for offences that are end dated but are active in NOMIS",
  )
  fun deactivateNomisOffence(@RequestBody offenceIds: List<Long>) {
    log.info("Request received to deactivate offences in nomis")
    return adminService.deactivateNomisOffence(offenceIds)
  }

  @PostMapping(value = ["/nomis/offences/encouragement/{parentOffenceId}"])
  @PreAuthorize("hasRole('ROLE_NOMIS_OFFENCE_ACTIVATOR')")
  @Operation(
    summary = "Create encouragement offence for parent offence",
    description = "Encouragement offence creates a new record with existing parent offence value, but with 'E' suffix to the offence code",
  )
  fun createEncouragementOffence(@PathVariable parentOffenceId: Long): Offence {
    log.info("Create encouragement offence for parent offence")
    return adminService.createEncouragementOffence(parentOffenceId)
  }

  @GetMapping("import")
  @PreAuthorize("hasRole('ROLE_MANAGE_OFFENCES_ADMIN')")
  @ResponseBody
  fun getImportCsv(): ResponseEntity<InputStreamResource> {
    val csvContent = OffenceImportService.csvHeaders.joinToString(",") + "\n"

    val inputStream = ByteArrayInputStream(csvContent.toByteArray())

    return ResponseEntity.ok()
      .header("Content-Disposition", "attachment; filename=offence_template.csv")
      .contentType(MediaType.parseMediaType("text/csv"))
      .body(InputStreamResource(inputStream))
  }

  @PostMapping(value = ["/import"])
  @PreAuthorize("hasRole('ROLE_MANAGE_OFFENCES_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Upload multiple offences using CSV file",
    description = "Use formatted CSV file to upload multiple offences. Offence code must not already be in use.",
  )
  fun importCSVFile(
    @RequestParam("file") file: MultipartFile,
    @RequestParam("schedulePartId", required = false) schedulePartId: Long?,
  ): ImportCsvResult {
    if (file.isEmpty) {
      return ImportCsvResult(success = false, message = "Invalid file", errors = listOf("CSV file is empty"))
    }

    if (schedulePartId !== null && !offenceImportService.validateSchedulePartExists(schedulePartId)) {
      return ImportCsvResult(success = false, message = "Invalid schedule part", errors = listOf("No schedule part found for ID $schedulePartId"))
    }

    offenceImportService.validateCsv(BufferedReader(InputStreamReader(file.inputStream)))?.let { return it }
    return offenceImportService.persist(BufferedReader(InputStreamReader(file.inputStream)), schedulePartId)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
