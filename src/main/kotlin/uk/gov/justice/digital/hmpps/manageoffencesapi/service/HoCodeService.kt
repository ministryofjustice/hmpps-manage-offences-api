package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CompressionType
import software.amazon.awssdk.services.s3.model.ExpressionType
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest
import software.amazon.awssdk.services.s3.model.InputSerialization
import software.amazon.awssdk.services.s3.model.JSONOutput
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.OutputSerialization
import software.amazon.awssdk.services.s3.model.ParquetInput
import software.amazon.awssdk.services.s3.model.RecordsEvent
import software.amazon.awssdk.services.s3.model.SelectObjectContentEventStream
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponse
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler
import java.util.concurrent.CompletableFuture

@Service
class HoCodeService(
  private val s3Client: S3Client,
  private val s3AsyncClient: S3AsyncClient,
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

  fun testSelectS3ObjectContent(bucket: String, key: String): List<HomeOfficeCode> {
    log.info("testSelectS3ObjectContent start for bucket: $bucket and key: $key")
    val handler = Handler()
    log.info("Making query to S3")
    selectQueryWithHandler(handler, bucket, key).join()
    log.info("Query finished")
    val events = handler.receivedEvents
    log.info("Size of events:  ${events.size}")
    val recordsEvents = events
      .filter { it.sdkEventType() === SelectObjectContentEventStream.EventType.RECORDS }
      .map { it as RecordsEvent }
    val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val rowsAsStrings = recordsEvents.map {
      it.payload().asUtf8String().split("\r?\n|\r".toRegex()).toTypedArray().toList()
    }.flatten().filter { it.isNotBlank() }

    val homeOfficeCodes = rowsAsStrings.map { mapper.readValue(it, HomeOfficeCode::class.java) }

    log.info("All rows received from S3 select on parquet file:")
    homeOfficeCodes.forEach { log.info(it.toString()) }
    log.info("Number of records from parquet file: ${homeOfficeCodes.size}")
    return homeOfficeCodes
  }

  private fun selectQueryWithHandler(
    handler: SelectObjectContentResponseHandler,
    bucket: String,
    key: String,
  ): CompletableFuture<Void> {
    val inputSerialization = InputSerialization.builder()
      .parquet(ParquetInput.builder().build())
      .compressionType(CompressionType.NONE)
      .build()
    val outputSerialization = OutputSerialization.builder()
      .json(JSONOutput.builder().build())
      .build()
    val select = SelectObjectContentRequest.builder()
      .bucket(bucket)
      .key(key)
      .expression("select * from S3Object s")
      .expressionType(ExpressionType.SQL)
      .inputSerialization(inputSerialization)
      .outputSerialization(outputSerialization)
      .build()

    return s3AsyncClient.selectObjectContent(select, handler)
  }

  private class Handler : SelectObjectContentResponseHandler {
    private var response: SelectObjectContentResponse? = null
    val receivedEvents: MutableList<SelectObjectContentEventStream> = ArrayList()
    private var exception: Throwable? = null
    override fun responseReceived(response: SelectObjectContentResponse) {
      this.response = response
    }

    override fun onEventStream(publisher: SdkPublisher<SelectObjectContentEventStream>) {
      publisher.subscribe { e: SelectObjectContentEventStream ->
        receivedEvents.add(e)
      }
    }

    override fun exceptionOccurred(throwable: Throwable) {
      exception = throwable
    }

    override fun complete() {}
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}

data class HomeOfficeCode(
  @JsonProperty("ho_offence_code")
  val hoCode: String = "",
  @JsonProperty("ho_offence_desc")
  val hoDescription: String = "",
)
