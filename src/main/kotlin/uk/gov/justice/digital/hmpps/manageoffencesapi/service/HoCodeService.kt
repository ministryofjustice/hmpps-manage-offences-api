package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.fasterxml.jackson.annotation.JsonProperty
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.AnalyticalPlatformTableName
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.HomeOfficeCodeRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.HomeOfficeCode as HomeOfficeCodeEntity

@Service
class HoCodeService(
  private val awsS3Service: AwsS3Service,
  private val homeOfficeCodeRepository: HomeOfficeCodeRepository,
  private val adminService: AdminService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Scheduled(cron = "0 */15 * * * *") // TODO setting to 15 minutes for test purposes. change schedule after testing
  @Transactional
  @SchedulerLock(name = "fullLoadOfHomeOfficeCodes")
  fun fullLoadOfHomeOfficeCodes() {
    if (!adminService.isFeatureEnabled(Feature.SYNC_HOME_OFFICE_CODES)) {
      log.info("Sync Home Office Codes not running - disabled")
      return
    }

    log.info("Start a full of Home Office Code data from Analytical Platform (S3)")
    val hoCodeFileKeys = awsS3Service.getKeysInPath(AnalyticalPlatformTableName.HO_CODES.s3Path)
    hoCodeFileKeys.forEach { fileKey ->
      val hoCodesToLoad = awsS3Service.loadParquetFileContents(fileKey, AnalyticalPlatformTableName.HO_CODES.clazz).map { it as HomeOfficeCode }
      val hoCodesToSave = hoCodesToLoad.map {
        HomeOfficeCodeEntity(
          id = it.code,
          category = it.category,
          subCategory = it.subCategory,
          description = it.description,
        )
      }
      homeOfficeCodeRepository.saveAll(hoCodesToSave)
    }

    val mappingFileKeys = awsS3Service.getKeysInPath(AnalyticalPlatformTableName.HO_CODES_TO_OFFENCE_MAPPING.s3Path)
    mappingFileKeys.forEach { fileKey ->
      val mappingsToLoad =
        awsS3Service.loadParquetFileContents(fileKey, AnalyticalPlatformTableName.HO_CODES_TO_OFFENCE_MAPPING.clazz).map { it as HomeOfficeCodeToOffenceMapping }
      // TODO decide what to do with this mapping data. overwrite sdrs mappings? create mapping table? delete and full load or increment?
      log.info("file $fileKey has ${mappingsToLoad.size} mappings to load")
    }

    log.info("Finished a full load of Home Office Code data from Analytical Platform (S3)")
  }
}

data class HomeOfficeCode(
  @JsonProperty("ho_offence_code")
  val code: String = "",
  @JsonProperty("ho_offence_desc")
  val description: String = "",
) {
  val category: Int
    get() = code.substring(0, 3).toInt()
  val subCategory: Int
    get() = code.substring(3).toInt()
}

data class HomeOfficeCodeToOffenceMapping(
  @JsonProperty("ho_offence_code")
  val hoCode: String = "",
  @JsonProperty("cjs_offence_code")
  val offenceCode: String = "",
)
