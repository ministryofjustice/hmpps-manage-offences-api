package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

@Service
class HoCodeService(
  private val s3Client: S3Client,
) {
  fun testS3ListBuckets(): String {
    log.info("testS3ListBuckets start")
    val listOfbuckets = s3Client.listBuckets()
    log.info("listOfbuckets: $listOfbuckets")
    return listOfbuckets.toString()
  }
  fun testS3ListObjects(bucket: String): String {
    log.info("testS3ListObjects for $bucket start")

    val listObjectsInBucket = s3Client.listObjectsV2(
      ListObjectsV2Request
        .builder()
        .bucket(bucket)
        .build(),
    )

    log.info("List of objects in bucket $listObjectsInBucket")
    log.info("testS3ListObjects finish")
    return listObjectsInBucket.toString()
  }

  fun testS3GetBucketLocation(bucket: String): String {
    log.info("testS3ListObjectsInBucket start for bucket $bucket")

    val bucketLocation = s3Client.getBucketLocation(
      GetBucketLocationRequest.builder()
        .bucket(bucket)
        .build(),
    )

    log.info("Bucket location $bucketLocation")
    log.info("testS3GetBucketLocation finish")
    return bucketLocation.toString()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
