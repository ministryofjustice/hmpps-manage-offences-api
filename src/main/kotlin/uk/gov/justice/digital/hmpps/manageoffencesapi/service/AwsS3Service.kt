package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CommonPrefix
import software.amazon.awssdk.services.s3.model.CompressionType.NONE
import software.amazon.awssdk.services.s3.model.ExpressionType.SQL
import software.amazon.awssdk.services.s3.model.InputSerialization
import software.amazon.awssdk.services.s3.model.JSONOutput
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.OutputSerialization
import software.amazon.awssdk.services.s3.model.ParquetInput
import software.amazon.awssdk.services.s3.model.SelectObjectContentEventStream
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponse
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler
import software.amazon.awssdk.services.s3.model.selectobjectcontenteventstream.DefaultRecords
import java.util.concurrent.CompletableFuture

@Service
class AwsS3Service(
  private val s3AsyncClient: S3AsyncClient,
  private val s3Client: S3Client,
) {
  private val log = LoggerFactory.getLogger(AwsS3Service::class.java)

  fun <T> loadParquetFileContents(fileKey: String, clazz: Class<T>): List<T> {
    val res = getParquetFileData(fileKey)
    val rowsAsStrings = res.split("\r?\n|\r".toRegex()).toTypedArray().toList().filter { it.isNotBlank() }
    val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    return rowsAsStrings.map { mapper.readValue(it, clazz) }
  }
  fun getParquetFileData(fileKey: String): String {
    val data = StringBuilder()
    try {
      val handler = SelectObjectHandler()
      val query = queryS3(fileKey, handler)
      query.join()
      for (events in handler.receivedEvents) {
        if (events is DefaultRecords) {
          val payload = events.payload().asUtf8String()
          data.append(payload)
        }
      }
    } catch (e: Exception) {
      log.error("Failed when downloading parquet data: ${e.message}", e)
      throw RuntimeException("Failed s3 download for key $fileKey, error: ${e.message}", e)
    }
    return data.toString()
  }

  fun getKeysInPath(path: String): Set<String> {
    return s3Client.listObjectsV2(
      ListObjectsV2Request.builder()
        .bucket(BUCKET)
        .prefix(path)
        .delimiter("/")
        .startAfter(path)
        .build(),
    ).contents()
      .map { it.key() }
      .toSet()
  }

  private fun queryS3(
    key: String,
    handler: SelectObjectContentResponseHandler,
  ): CompletableFuture<Void> {
    val inputSerialization = InputSerialization.builder()
      .parquet(ParquetInput.builder().build())
      .compressionType(NONE)
      .build()
    val outputSerialization = OutputSerialization.builder().json(JSONOutput.builder().build()).build()

    val select = SelectObjectContentRequest.builder()
      .bucket(BUCKET)
      .key(key)
      .expression("SELECT * FROM s3object")
      .expressionType(SQL)
      .inputSerialization(inputSerialization)
      .outputSerialization(outputSerialization)
      .build()
    return s3AsyncClient.selectObjectContent(select, handler)
  }

  fun getSubDirectories(path: String): List<CommonPrefix> {
    return s3Client.listObjectsV2(
      ListObjectsV2Request.builder()
        .bucket(BUCKET)
        .prefix(path)
        .delimiter("/")
        .build(),
    ).commonPrefixes()
  }

  private class SelectObjectHandler : SelectObjectContentResponseHandler {
    val receivedEvents: MutableList<SelectObjectContentEventStream> = ArrayList()
    override fun responseReceived(response: SelectObjectContentResponse) {}
    override fun onEventStream(publisher: SdkPublisher<SelectObjectContentEventStream>) {
      publisher.subscribe { e: SelectObjectContentEventStream -> receivedEvents.add(e) }
    }

    override fun exceptionOccurred(throwable: Throwable) {}
    override fun complete() {}
  }

  companion object {
    const val BUCKET = "mojap-manage-offences"
  }
}
