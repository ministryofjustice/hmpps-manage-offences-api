package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

@Service
class HoCodeService(
  private val s3Client: S3Client,
) {
  fun testS3Integration(): String {
    log.info("Fetching from S3 start")
    val listOfbuckets = s3Client.listBuckets()
    log.info("listOfbuckets: $listOfbuckets")

    val listObjectsInFolder = s3Client.listObjectsV2(
      ListObjectsV2Request
        .builder()
        .bucket("mojap-manage-offences")
        .build(),
    )

    log.info("List of objects in folder $listObjectsInFolder")
    log.info("Fetching from S3 finish")
    return listOfbuckets.toString()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
