package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
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

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
