package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.AdminService

@RestController
@RequestMapping("/admin", produces = [MediaType.APPLICATION_JSON_VALUE])
class AdminController(
  private val adminService: AdminService,
) {
  @PutMapping(value = ["/toggle-feature"])
  @PreAuthorize("hasRole('ROLE_MANAGE_OFFENCES_ADMIN')")
  @Operation(
    summary = "Enable / disable a feature"
  )
  fun toggleFeature(@RequestBody featureToggles: List<FeatureToggle>) {
    log.info("Request received to toggle features")
    return adminService.toggleFeature(featureToggles)
  }

  @GetMapping(value = ["/feature-toggles"])
  @Operation(
    summary = "Get values of all feature toggles"
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

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
