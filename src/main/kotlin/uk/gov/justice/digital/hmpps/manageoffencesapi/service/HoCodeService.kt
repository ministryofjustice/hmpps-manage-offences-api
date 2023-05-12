package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import com.fasterxml.jackson.annotation.JsonProperty
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.HoCodesLoadHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.AnalyticalPlatformTableName.HO_CODES
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.AnalyticalPlatformTableName.HO_CODES_TO_OFFENCE_MAPPING
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.HoCodesLoadHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.HomeOfficeCodeRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.HomeOfficeCode as HomeOfficeCodeEntity

@Service
class HoCodeService(
  private val awsS3Service: AwsS3Service,
  private val homeOfficeCodeRepository: HomeOfficeCodeRepository,
  private val hoCodesLoadHistoryRepository: HoCodesLoadHistoryRepository,
  private val offenceRepository: OffenceRepository,
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

    log.info("Start a full load of Home Office Code data from Analytical Platform (S3)")
    loadHoCodes()
    loadMappingData()
    log.info("Finished full load of Home Office Code data from Analytical Platform (S3)")
  }

  private fun loadHoCodes() {
    val pathToReadFrom = getLatestLoadDirectory(HO_CODES.s3BasePath)
    val hoCodeFileKeys = awsS3Service.getKeysInPath(pathToReadFrom)
    val alreadyLoadedFiles = hoCodesLoadHistoryRepository.findByLoadedFileIn(hoCodeFileKeys)
    log.info("${alreadyLoadedFiles.size} ho-code files have previously been loaded")

    val filesToProcess = hoCodeFileKeys.minus(alreadyLoadedFiles.map { it.loadedFile }.toSet())
    log.info("${filesToProcess.size} ho-code files to process")

    filesToProcess.forEach { fileKey ->
      log.info("Processing $fileKey")
      val hoCodesToLoad =
        awsS3Service.loadParquetFileContents(fileKey, HO_CODES.mappingClass).map { it as HomeOfficeCode }
      val hoCodesToSave = hoCodesToLoad.map {
        HomeOfficeCodeEntity(
          id = it.code,
          category = it.category,
          subCategory = it.subCategory,
          description = it.description,
        )
      }
      homeOfficeCodeRepository.saveAll(hoCodesToSave)
      hoCodesLoadHistoryRepository.save(HoCodesLoadHistory(loadedFile = fileKey))
    }
  }

  private fun loadMappingData() {
    val pathToReadFrom = getLatestLoadDirectory(HO_CODES_TO_OFFENCE_MAPPING.s3BasePath)
    val mappingFileKeys = awsS3Service.getKeysInPath(pathToReadFrom)
    val alreadyLoadedFiles = hoCodesLoadHistoryRepository.findByLoadedFileIn(mappingFileKeys)
    log.info("${alreadyLoadedFiles.size} mapping files have previously been loaded")
    val filesToProcess = mappingFileKeys.minus(alreadyLoadedFiles.map { it.loadedFile }.toSet())
    log.info("${filesToProcess.size} mapping files to process")

    filesToProcess.forEach { fileKey ->
      log.info("Processing $fileKey")
      val mappingsToLoad =
        awsS3Service.loadParquetFileContents(fileKey, HO_CODES_TO_OFFENCE_MAPPING.mappingClass)
          .map { it as HomeOfficeCodeToOffenceMapping }
      val mappingsByCode = mappingsToLoad.associateBy { it.offenceCode }
      val offencesToUpdate = offenceRepository.findByCodeIn(mappingsToLoad.map { it.offenceCode }.toSet())
      offenceRepository.saveAll(
        offencesToUpdate.map {
          it.copy(
            category = mappingsByCode[it.code]!!.category,
            subCategory = mappingsByCode[it.code]!!.subCategory,
          )
        },
      )
      hoCodesLoadHistoryRepository.save(HoCodesLoadHistory(loadedFile = fileKey))
    }
  }

  private fun getLatestLoadDirectory(s3BasePath: String): String {
    val subdirectories = awsS3Service.getSubDirectories(s3BasePath)
    val pathByTimestamp = subdirectories
      .associateBy {
        LocalDateTime.parse(
          it.prefix().substring(s3BasePath.length + FOLDER_NAME_PREFIX.length, it.prefix().lastIndex),
        )
      }
    val maxExtractionTimestamp = pathByTimestamp.keys.max()
    log.info("latest extraction timestamp is: $maxExtractionTimestamp")
    return pathByTimestamp[maxExtractionTimestamp]!!.prefix()
  }

  companion object {
    const val FOLDER_NAME_PREFIX = "extraction_timestamp="
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
) {
  val category: Int
    get() = hoCode.substring(0, 3).toInt()
  val subCategory: Int
    get() = hoCode.substring(3).toInt()
}
