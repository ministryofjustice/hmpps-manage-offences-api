package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import software.amazon.awssdk.services.s3.model.CommonPrefix
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.HoCodesLoadHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.AnalyticalPlatformTableName
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.HoCodesLoadHistoryRepository
import java.util.concurrent.CompletableFuture

class HoCodeServiceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var hoCodeService: HoCodeService

  @Autowired
  lateinit var hoCodesLoadHistoryRepository: HoCodesLoadHistoryRepository

  @Test
  @Sql(
    "classpath:test_data/reset-all-data.sql",
  )
  fun `Ensure record is created in ho_code_load_history after successful run`() {
    whenever(s3Client.listObjectsV2(any(ListObjectsV2Request::class.java))).thenReturn(SUB_DIRECTORIES)
    whenever(s3Client.listObjectsV2(GET_FILES_TO_PROCESS_REQUEST)).thenReturn(FILE1_TO_PROCESS)
    whenever(s3AsyncClient.selectObjectContent(any(SelectObjectContentRequest::class.java), any())).thenReturn(
      CompletableFuture.completedFuture(null),
    )
    val countBefore = hoCodesLoadHistoryRepository.count()
    assertThat(countBefore).isEqualTo(0)

    hoCodeService.fullLoadOfHomeOfficeCodes()

    val loadHistories = hoCodesLoadHistoryRepository.findAll()
    assertThat(loadHistories)
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes("loadDate")
      .ignoringFieldsMatchingRegexes("id")
      .isEqualTo(
        listOf(
          HoCodesLoadHistory(
            loadedFile = LATEST_FOLDER_PATH + "file1",
          ),
        ),
      )
  }

  companion object {
    private const val BUCKET = "mojap-manage-offences"

    private val LATEST_FOLDER_PATH =
      AnalyticalPlatformTableName.HO_CODES.s3BasePath + "extraction_timestamp=" + "2023-05-07T03:04:11.984/"
    val SUB_DIRECTORIES: ListObjectsV2Response = ListObjectsV2Response
      .builder()
      .commonPrefixes(
        CommonPrefix.builder()
          .prefix(LATEST_FOLDER_PATH)
          .build(),
      )
      .build()

    val FILE1_TO_PROCESS: ListObjectsV2Response = ListObjectsV2Response
      .builder()
      .contents(
        listOf(S3Object.builder().key(LATEST_FOLDER_PATH + "file1").build()),
      )
      .build()

    val GET_FILES_TO_PROCESS_REQUEST: ListObjectsV2Request =
      ListObjectsV2Request.builder()
        .bucket(BUCKET)
        .prefix(LATEST_FOLDER_PATH)
        .delimiter("/")
        .startAfter(LATEST_FOLDER_PATH)
        .build()
  }
}
