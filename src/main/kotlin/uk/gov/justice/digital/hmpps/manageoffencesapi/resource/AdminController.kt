package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ScheduleStatusRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.AdminService

@RestController
@RequestMapping("/admin", produces = [MediaType.APPLICATION_JSON_VALUE])
class AdminController(
  private val adminService: AdminService,
) {
  @PutMapping(value = ["/schedule/{scheduleId}/status"])
  @PreAuthorize("hasRole('ROLE_MANAGE_OFFENCES_ADMIN')")
  @Operation(
    summary = "Set the publication status of a schedule",
    description = "Schedules are created as DRAFT and are withheld from callers without ROLE_MANAGE_OFFENCES_ADMIN until published",
  )
  fun setScheduleStatus(
    @PathVariable scheduleId: Long,
    @RequestBody request: ScheduleStatusRequest,
  ) {
    log.info("Request received to set status of schedule {} to {}", scheduleId, request.status)
    adminService.setScheduleStatus(scheduleId, request.status)
  }

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

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
