package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.s3.model.CommonPrefix
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.AnalyticalPlatformTableName.HO_CODES
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.AnalyticalPlatformTableName.HO_CODES_TO_OFFENCE_MAPPING
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.HoCodesLoadHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.HomeOfficeCodeRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.PreviousOffenceToHoCodeMappingRepository
import java.time.LocalDate
import java.time.LocalDateTime

class HoCodeServiceTest {

  private val awsS3Service = mock<AwsS3Service>()
  private val homeOfficeCodeRepository = mock<HomeOfficeCodeRepository>()
  private val hoCodesLoadHistoryRepository = mock<HoCodesLoadHistoryRepository>()
  private val offenceRepository = mock<OffenceRepository>()
  private val previousOffenceToHoCodeMappingRepository = mock<PreviousOffenceToHoCodeMappingRepository>()
  private val adminService = mock<AdminService>()
  private val hoCodeService =
    HoCodeService(awsS3Service, homeOfficeCodeRepository, hoCodesLoadHistoryRepository, offenceRepository, adminService, previousOffenceToHoCodeMappingRepository)

  @Test
  fun `Do not run load if feature toggle is disabled`() {
    whenever(adminService.isFeatureEnabled(Feature.SYNC_HOME_OFFICE_CODES)).thenReturn(false)
    hoCodeService.fullLoadOfHomeOfficeCodes()
    verifyNoInteractions(awsS3Service)
  }

  @Test
  fun `Test that a full load runs successfully - loads ho-codes and mappings`() {
    whenever(adminService.isFeatureEnabled(Feature.SYNC_HOME_OFFICE_CODES)).thenReturn(true)
    whenever(awsS3Service.getSubDirectories(HO_CODES.s3BasePath)).thenReturn(SUB_DIRECTORIES_HO_CODE)
    whenever(awsS3Service.getSubDirectories(HO_CODES_TO_OFFENCE_MAPPING.s3BasePath)).thenReturn(SUB_DIRECTORIES_MAPPINGS)

    whenever(awsS3Service.getKeysInPath(LATEST_FOLDER_PATH_HO_CODE)).thenReturn(setOf(HO_FILE_1_KEY))
    whenever(awsS3Service.getKeysInPath(LATEST_FOLDER_PATH_MAPPINGS)).thenReturn(setOf(MAPPING_FILE_1_KEY))

    whenever(hoCodesLoadHistoryRepository.findByLoadedFileIn(any())).thenReturn(emptySet())

    whenever(awsS3Service.loadParquetFileContents(HO_FILE_1_KEY, HO_CODES.mappingClass)).thenReturn(
      listOf(
        HOME_OFFICE_CODE_1,
      ),
    )
    whenever(
      awsS3Service.loadParquetFileContents(
        MAPPING_FILE_1_KEY,
        HO_CODES_TO_OFFENCE_MAPPING.mappingClass,
      ),
    ).thenReturn(listOf(MAPPING_1))

    whenever(offenceRepository.findByCodeIn(setOf(MAPPING_1.offenceCode))).thenReturn(listOf(OFFENCE_1))

    hoCodeService.fullLoadOfHomeOfficeCodes()

    verify(homeOfficeCodeRepository).saveAll(
      listOf(
        uk.gov.justice.digital.hmpps.manageoffencesapi.entity.HomeOfficeCode(
          id = HOME_OFFICE_CODE_1.code,
          category = HOME_OFFICE_CODE_1.category,
          subCategory = HOME_OFFICE_CODE_1.subCategory,
          description = HOME_OFFICE_CODE_1.description,
        ),
      ),
    )
    verify(offenceRepository).saveAll(
      listOf(
        OFFENCE_1.copy(category = 12, subCategory = 34),
      ),
    )
    verify(hoCodesLoadHistoryRepository).save(
      argThat { hoCodesLoadHistory -> hoCodesLoadHistory.loadedFile == HO_FILE_1_KEY },
    )
    verify(hoCodesLoadHistoryRepository).save(
      argThat { hoCodesLoadHistory -> hoCodesLoadHistory.loadedFile == MAPPING_FILE_1_KEY },
    )
  }

  companion object {
    private val LATEST_FOLDER_PATH_HO_CODE = HO_CODES.s3BasePath + "extraction_timestamp=" + "2023-05-07T03:04:11.984/"
    private val LATEST_FOLDER_PATH_MAPPINGS =
      HO_CODES_TO_OFFENCE_MAPPING.s3BasePath + "extraction_timestamp=" + "2023-05-07T03:04:11.984/"
    val SUB_DIRECTORIES_HO_CODE = listOf(
      CommonPrefix.builder()
        .prefix(LATEST_FOLDER_PATH_HO_CODE)
        .build(),
    )

    val SUB_DIRECTORIES_MAPPINGS = listOf(
      CommonPrefix.builder()
        .prefix(LATEST_FOLDER_PATH_MAPPINGS)
        .build(),
    )

    val HO_FILE_1_KEY = LATEST_FOLDER_PATH_HO_CODE + "ho-code-file1"
    val MAPPING_FILE_1_KEY = LATEST_FOLDER_PATH_MAPPINGS + "mapping-file1"

    val HOME_OFFICE_CODE_1 = HomeOfficeCode(code = "01234", description = "ho description 1")
    val MAPPING_1 = HomeOfficeCodeToOffenceMapping(hoCode = "01234", offenceCode = "OFF1")

    private val BASE_OFFENCE = Offence(
      code = "AABB011",
      changedDate = LocalDateTime.now(),
      revisionId = 1,
      startDate = LocalDate.now(),
      sdrsCache = SdrsCache.OFFENCES_A,
    )
    val OFFENCE_1 = BASE_OFFENCE.copy(code = "OFF1")
  }
}
