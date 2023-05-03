package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.HoCodeService

@RestController
@RequestMapping("/test")
class TestController(
  private val hoCodeService: HoCodeService,
) {
  @GetMapping(value = ["/s3-list-buckets"])
  @Operation(
    summary = "Test AP S3 integration - list buckets",
  )
  fun testS3ListBuckets(): String {
    log.info("Request received to test s3 list buckets")
    return hoCodeService.testS3ListBuckets()
  }

  @GetMapping(value = ["/s3-list-objects/{bucket}"])
  @Operation(
    summary = "Test AP S3 integration - list objects",
  )
  fun testS3ListObjects(
    @Parameter(required = true, example = "bucket1", description = "The bucket name")
    @PathVariable("bucket")
    bucket: String,
  ): String {
    log.info("Request received to test s3 list objects")
    return hoCodeService.testS3ListObjects(bucket)
  }

  @GetMapping(value = ["/s3-bucket-location/{bucket}"])
  @Operation(
    summary = "Test AP S3 integration - bucket location",
  )
  fun testS3GetBucketLocation(
    @Parameter(required = true, example = "bucket1", description = "The bucket name")
    @PathVariable("bucket")
    bucket: String,
  ): String {
    log.info("Request received to test s3 list objects")
    return hoCodeService.testS3GetBucketLocation(bucket)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
