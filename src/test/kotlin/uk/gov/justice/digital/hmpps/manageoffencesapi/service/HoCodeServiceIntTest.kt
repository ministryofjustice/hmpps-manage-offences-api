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
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.AnalyticalPlatformTableName.HO_CODES
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.AnalyticalPlatformTableName.HO_CODES_TO_OFFENCE_MAPPING
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
    whenever(s3Client.listObjectsV2(getSubDirectoriesRequest(HO_CODES))).thenReturn(SUB_DIRECTORIES_HO_CODE)
    whenever(s3Client.listObjectsV2(getSubDirectoriesRequest(HO_CODES_TO_OFFENCE_MAPPING))).thenReturn(
      SUB_DIRECTORIES_MAPPINGS,
    )
    whenever(s3Client.listObjectsV2(getKeysInPathRequest(LATEST_FOLDER_PATH_HO_CODE))).thenReturn(
      HO_CODE_FILE1_TO_PROCESS,
    )
    whenever(s3Client.listObjectsV2(getKeysInPathRequest(LATEST_FOLDER_PATH_MAPPINGS))).thenReturn(
      MAPPING_FILE1_TO_PROCESS,
    )
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
            loadedFile = LATEST_FOLDER_PATH_HO_CODE + "ho-code-file1",
          ),
          HoCodesLoadHistory(
            loadedFile = LATEST_FOLDER_PATH_MAPPINGS + "mapping-file1",
          ),
        ),
      )
  }

  private fun getSubDirectoriesRequest(apTable: AnalyticalPlatformTableName): ListObjectsV2Request? = ListObjectsV2Request.builder()
    .bucket(BUCKET)
    .prefix(apTable.s3BasePath)
    .delimiter("/")
    .build()

  fun getKeysInPathRequest(path: String): ListObjectsV2Request = ListObjectsV2Request.builder()
    .bucket(AwsS3Service.BUCKET)
    .prefix(path)
    .delimiter("/")
    .startAfter(path)
    .build()

  companion object {
    private const val BUCKET = "mojap-manage-offences"
    private val LATEST_FOLDER_PATH_HO_CODE = HO_CODES.s3BasePath + "extraction_timestamp=" + "2023-05-07T03:04:11.984/"
    private val LATEST_FOLDER_PATH_MAPPINGS =
      HO_CODES_TO_OFFENCE_MAPPING.s3BasePath + "extraction_timestamp=" + "2023-05-07T03:04:11.984/"
    val SUB_DIRECTORIES_HO_CODE: ListObjectsV2Response = ListObjectsV2Response
      .builder()
      .commonPrefixes(
        CommonPrefix.builder()
          .prefix(LATEST_FOLDER_PATH_HO_CODE)
          .build(),
      )
      .build()

    val SUB_DIRECTORIES_MAPPINGS: ListObjectsV2Response = ListObjectsV2Response
      .builder()
      .commonPrefixes(
        CommonPrefix.builder()
          .prefix(LATEST_FOLDER_PATH_MAPPINGS)
          .build(),
      )
      .build()

    val HO_CODE_FILE1_TO_PROCESS: ListObjectsV2Response = ListObjectsV2Response
      .builder()
      .contents(
        listOf(S3Object.builder().key(LATEST_FOLDER_PATH_HO_CODE + "ho-code-file1").build()),
      )
      .build()

    val MAPPING_FILE1_TO_PROCESS: ListObjectsV2Response = ListObjectsV2Response
      .builder()
      .contents(
        listOf(S3Object.builder().key(LATEST_FOLDER_PATH_MAPPINGS + "mapping-file1").build()),
      )
      .build()
  }
}
