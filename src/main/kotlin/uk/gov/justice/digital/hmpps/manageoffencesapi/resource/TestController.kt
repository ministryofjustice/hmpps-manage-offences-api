package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.HoCodeService

@RestController
@RequestMapping("/test")
class TestController(
  private val hoCodeService: HoCodeService,
) {
  @GetMapping(value = ["/s3-integration"])
  @Operation(
    summary = "Test AP S3 integration",
  )
  fun testS3Integration(): String {
    log.info("Request received to test s3 integration")
    return hoCodeService.testS3Integration()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
