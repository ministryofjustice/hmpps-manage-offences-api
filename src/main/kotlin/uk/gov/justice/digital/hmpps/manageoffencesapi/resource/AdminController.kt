package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
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
  fun toggleFeature(@RequestBody featureToggle: FeatureToggle) {
    log.info("Request received to toggle Feature to {}", featureToggle.enabled)
    return adminService.toggleFeature(featureToggle)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
