package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import jakarta.persistence.EntityNotFoundException
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResultHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.INSERT
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType.UPDATE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisSyncType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.HoCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Statute
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisChangeHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceReactivatedInNomisRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceToSyncWithNomisRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence as EntityOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence as PrisonApiOffence

@Service
class OffenceService(
  private val offenceRepository: OffenceRepository,
  private val offenceScheduleMappingRepository: OffenceScheduleMappingRepository,
  private val sdrsLoadResultRepository: SdrsLoadResultRepository,
  private val sdrsLoadResultHistoryRepository: SdrsLoadResultHistoryRepository,
  private val nomisChangeHistoryRepository: NomisChangeHistoryRepository,
  private val reactivatedInNomisRepository: OffenceReactivatedInNomisRepository,
  private val offenceToSyncWithNomisRepository: OffenceToSyncWithNomisRepository,
  private val prisonApiClient: PrisonApiClient,
  private val adminService: AdminService,
) {
  fun findOffencesByCode(code: String): List<Offence> {
    log.info("Fetching offences by offenceCode")
    val offences = offenceRepository.findByCodeStartsWithIgnoreCase(code)
    return populateOffenceModel(offences)
  }

  private fun populateOffenceModel(offences: List<uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence>): List<Offence> {
    val offenceIds = offences.map { it.id }.toSet()
    val childrenByParentId = offenceRepository.findByParentOffenceIdIn(offenceIds).groupBy { it.parentOffenceId }
    val matchingOffences = offences.map {
      val children = childrenByParentId[it.id]
      transform(it, children?.map { child -> child.id })
    }

    val matchingOffenceIds = matchingOffences.map { it.id }
    val offenceMappingsByOffenceId =
      offenceScheduleMappingRepository.findByOffenceIdIn(matchingOffenceIds).groupBy { it.offence.id }
    return matchingOffences.map {
      it.copy(schedules = transform(offenceMappingsByOffenceId[it.id]))
    }.sortedBy { it.code }
  }

  fun findLoadResults(): List<MostRecentLoadResult> {
    log.info("Fetching offences by offenceCode")
    return sdrsLoadResultRepository.findAllByOrderByCacheAsc().map { transform(it) }
  }

  @Scheduled(cron = "0 0 */1 * * *")
  @SchedulerLock(name = "fullSyncWithNomisLock")
  @Transactional
  fun fullSyncWithNomis() {
    if (!adminService.isFeatureEnabled(FULL_SYNC_NOMIS)) {
      log.info("Full sync with NOMIS not running - disabled")
      return
    }
    // When we do a full sync to nomis, we split it into 26 chunks (A to Z)
    ('A'..'Z').forEach { alphaChar ->
      log.info("Starting full sync with NOMIS for offence group {} ", alphaChar)
      fullySyncWithNomisWhereOffenceStartsWith(alphaChar)
    }
  }

  @Scheduled(cron = "0 5-55/10 * * * *")
  @SchedulerLock(name = "deltaSyncWithNomisLock")
  @Transactional
  fun deltaSyncWithNomis() {
    if (!adminService.isFeatureEnabled(DELTA_SYNC_NOMIS) || adminService.isFeatureEnabled(FULL_SYNC_NOMIS)) {
      log.info("Delta sync with NOMIS not running - delta sync disabled or full sync is enabled ")
      return
    }
    val cachesToSyncWithNomis = sdrsLoadResultRepository.findByNomisSyncRequiredIsTrue()

    cachesToSyncWithNomis.forEach {
      fullySyncWithNomis(it.cache)
      sdrsLoadResultRepository.save(it.copy(nomisSyncRequired = false))
      createLoadHistoryRecord(it)
    }
    syncOtherOffencesWithNomis()
  }

  // This is used to synchronise offences to NOMIS that are outside the main SDRS route; i.e. ho-code updates and future end dated offences
  private fun syncOtherOffencesWithNomis() {
    log.info("Starting sync for other offence types with nomis (not from SDRS cache)")
    val (hoCodeUpdatedOffences, futureEndDatedOffences) = offenceToSyncWithNomisRepository.findAll().partition { it.nomisSyncType == NomisSyncType.HO_CODE_UPDATE }
    val offencesNeedDeactivating = offenceRepository.findByCodeIn(futureEndDatedOffences.map { it.offenceCode }.toSet()).filter { it.activeFlag == "N" }
    val offenceswithHoCodeUpdates = offenceRepository.findByCodeIn(hoCodeUpdatedOffences.map { it.offenceCode }.toSet())
    val allOffencesToSync = offencesNeedDeactivating.plus(offenceswithHoCodeUpdates)

    val offencesStartWithList = allOffencesToSync.map { it.code.substring(0, 3) }.toSet()
    val nomisOffences: MutableList<PrisonApiOffence> = mutableListOf()
    val nomisOffencesById: MutableMap<Pair<String, String>, PrisonApiOffence> = mutableMapOf()
    offencesStartWithList.forEach {
      val (offencesById, offences) = getAllNomisOffencesThatStartWith(it)
      nomisOffencesById.putAll(offencesById)
      nomisOffences.addAll(offences)
    }

    fullySyncWithNomis(allOffencesToSync, nomisOffencesById, nomisOffences)
    offenceToSyncWithNomisRepository.flush()
    offenceToSyncWithNomisRepository.deleteAllByIdInBatch(
      futureEndDatedOffences
        .filter { f -> offencesNeedDeactivating.any { it.code == f.offenceCode } }
        .plus(hoCodeUpdatedOffences)
        .map { it.id },
    )
  }

  private fun createLoadHistoryRecord(it: SdrsLoadResult) {
    sdrsLoadResultHistoryRepository.save(
      SdrsLoadResultHistory(
        cache = it.cache,
        status = it.status,
        loadType = it.loadType,
        loadDate = it.loadDate,
        nomisSyncRequired = false,
      ),
    )
  }

  //  This syncs all offences that start with the passed character with NOMIS - independent of caches
  fun fullySyncWithNomisWhereOffenceStartsWith(alphaChar: Char) {
    val allOffences = offenceRepository.findByCodeStartsWithIgnoreCase(alphaChar.toString())
    val (nomisOffencesById, nomisOffences) = getAllNomisOffencesThatStartWith(alphaChar.toString())

    fullySyncWithNomis(allOffences, nomisOffencesById, nomisOffences)
  }

  @Transactional(readOnly = true)
  fun findHoCodeByOffenceCode(code: String): String? {
    val offence = offenceRepository.findByCodeIgnoreCase(code)
      ?: throw EntityNotFoundException("No offence exists for the passed in offence code")
    return offence.homeOfficeStatsCode
  }

  fun fullySyncWithNomis(cache: SdrsCache) {
    log.info("Starting full sync with NOMIS for cache: {}", cache)
    val allOffences = offenceRepository.findBySdrsCache(cache)
    val (nomisOffencesById, nomisOffences) = if (cache.alphaChar != null) {
      getAllNomisOffencesThatStartWith(cache.alphaChar.toString())
    } else {
      getAllNomisOffencecsForNonAlphaCache(allOffences)
    }

    fullySyncWithNomis(allOffences, nomisOffencesById, nomisOffences)
  }

  private fun fullySyncWithNomis(
    allOffences: List<EntityOffence>,
    nomisOffencesById: Map<Pair<String, String>, PrisonApiOffence>,
    nomisOffences: List<PrisonApiOffence>,
  ) {
    val offencesByCode = allOffences.associateBy { it.code }

    // the keys here represent the NOMIS keys, each key is a pair of the offenceCode and the statuteCode
    val (existingOffenceKeys, newOffenceKeys) = offencesByCode.keys.map { Pair(it, offencesByCode[it]!!.statuteCode) }
      .partition { nomisOffencesById.containsKey(it) }

    val newNomisStatutes = determineNewStatutesToCreate(allOffences, nomisOffences)
    val newNomisHoCodes = determineNewHoCodesToCreate(allOffences, nomisOffences)
    val newNomisOffences = mutableSetOf<PrisonApiOffence>()
    val updatedNomisOffences = mutableSetOf<PrisonApiOffence>()

    newOffenceKeys.forEach {
      val offence = offencesByCode[it.first]!!
      val statute = findAssociatedStatute(offence, nomisOffences, newNomisStatutes)
      val homeOfficeCode = findAssociatedHomeOfficeCodeInNomis(offence, nomisOffences, newNomisHoCodes)
      newNomisOffences.add(copyOffenceToCreate(offence, statute, homeOfficeCode))
    }

    // Reactivated offences are offences that have been explicitly made active in NOMIS (using the manage-offences UI)
    // So they will remain active in NOMIS (although they have been end dated). This list will be small
    val reactivatedOffences = reactivatedInNomisRepository.findAllById(
      existingOffenceKeys
        .map { it.first },
    )
      .map { it.offenceCode }.toSet()
    existingOffenceKeys.forEach {
      val offence = offencesByCode[it.first]!!
      val nomisOffence = nomisOffencesById[it]!!
      if (!offenceDetailsSame(offence, nomisOffence)) {
        val homeOfficeCode = findAssociatedHomeOfficeCodeInNomis(offence, nomisOffences, newNomisHoCodes)
        updatedNomisOffences.add(copyOffenceToUpdate(offence, nomisOffence, homeOfficeCode, reactivatedOffences))
      }
    }

    createNomisStatutes(newNomisStatutes)
    createNomisHomeOfficeCodes(newNomisHoCodes)
    createNomisOffences(newNomisOffences)
    updateNomisOffences(updatedNomisOffences)
  }

  private fun determineNewStatutesToCreate(
    allOffences: List<EntityOffence>,
    nomisOffences: List<uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence>,
  ): Set<Statute> {
    val offencesByNewStatuteCode = allOffences
      .filter { nomisOffences.none { o -> o.statuteCode.code == it.statuteCode } }
      .groupBy { it.statuteCode }

    return offencesByNewStatuteCode.map {
      val statuteDescription = it.value
        .sortedWith(
          compareByDescending(EntityOffence::activeFlag)
            .thenByDescending(EntityOffence::startDate),
        )
        .firstOrNull { offence -> offence.statuteDescription != offence.statuteCode }?.statuteDescription
      Statute(
        code = it.key,
        description = statuteDescription ?: it.key,
        legislatingBodyCode = "UK",
        activeFlag = "Y",
      )
    }.toSet()
  }

  private fun determineNewHoCodesToCreate(
    allOffences: List<EntityOffence>,
    nomisOffences: List<uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence>,
  ): Set<HoCode> =
    allOffences
      .filter { !it.homeOfficeStatsCode.isNullOrBlank() && nomisOffences.none { o -> o.hoCode?.code == it.homeOfficeStatsCode } }
      .map {
        HoCode(
          code = it.homeOfficeStatsCode!!,
          description = it.homeOfficeStatsCode!!,
          activeFlag = "Y",
        )
      }.toSet()

  private fun createNomisStatutes(nomisStatutesToCreate: Set<Statute>) {
    nomisStatutesToCreate.chunked(MAX_RECORDS_IN_POST_PAYLOAD).forEach {
      prisonApiClient.createStatutes(it)
      nomisChangeHistoryRepository.saveAll(it.map { o -> transform(o, INSERT) })
    }
  }

  private fun createNomisHomeOfficeCodes(homeOfficeCodesToCreate: Set<HoCode>) {
    if (homeOfficeCodesToCreate.isNotEmpty()) {
      homeOfficeCodesToCreate.chunked(MAX_RECORDS_IN_POST_PAYLOAD).forEach {
        prisonApiClient.createHomeOfficeCodes(it)
      }
    }
  }

  private fun createNomisOffences(nomisOffencesToCreate: Set<PrisonApiOffence>) {
    if (nomisOffencesToCreate.isNotEmpty()) {
      nomisOffencesToCreate.chunked(MAX_RECORDS_IN_POST_PAYLOAD).forEach {
        prisonApiClient.createOffences(it)
        nomisChangeHistoryRepository.saveAll(it.map { o -> transform(o, INSERT) })
      }
    }
  }

  private fun updateNomisOffences(nomisOffencedToUpdate: Set<PrisonApiOffence>) {
    if (nomisOffencedToUpdate.isNotEmpty()) {
      nomisOffencedToUpdate.chunked(MAX_RECORDS_IN_POST_PAYLOAD).forEach {
        prisonApiClient.updateOffences(it)
        nomisChangeHistoryRepository.saveAll(it.map { o -> transform(o, UPDATE) })
      }
    }
  }

  private fun statuteExists(
    nomisOffences: List<PrisonApiOffence>,
    offence: EntityOffence,
  ) = nomisOffences.any { o -> o.statuteCode.code == offence.statuteCode }

  private fun offenceDetailsSame(offence: EntityOffence, nomisOffence: PrisonApiOffence): Boolean =
    nomisOffence.hoCode?.code == offence.homeOfficeStatsCode &&
      nomisOffence.description == offence.derivedDescription &&
      nomisOffence.activeFlag == offence.activeFlag &&
      nomisOffence.expiryDate == offence.expiryDate

  private fun copyOffenceToUpdate(
    offence: uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence,
    nomisOffence: uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence,
    homeOfficeCode: HoCode?,
    reactivatedOffences: Set<String>,
  ): PrisonApiOffence {
    log.info("Offence code {} to be updated in NOMIS", offence.code)
    return nomisOffence.copy(
      description = offence.derivedDescription.trim(),
      hoCode = homeOfficeCode,
      activeFlag = if (reactivatedOffences.contains(offence.code)) nomisOffence.activeFlag else offence.activeFlag,
      expiryDate = offence.expiryDate,
    )
  }

  private fun copyOffenceToCreate(
    offence: EntityOffence,
    statute: Statute,
    homeOfficeCode: HoCode?,
  ): PrisonApiOffence {
    log.info("Offence code {} to be created in NOMIS", offence.code)
    return PrisonApiOffence(
      code = offence.code,
      description = offence.derivedDescription.trim(),
      statuteCode = statute,
      hoCode = homeOfficeCode,
      severityRanking = offence.category?.toString() ?: "99",
      activeFlag = offence.activeFlag,
      expiryDate = offence.expiryDate,
    )
  }

  private fun findAssociatedHomeOfficeCodeInNomis(
    offence: EntityOffence,
    nomisOffences: List<PrisonApiOffence>,
    newNomisHoCodes: Set<HoCode>,
  ): HoCode? {
    if (offence.homeOfficeStatsCode == null) return null
    if (homeOfficeCodeExists(nomisOffences, offence)) {
      return nomisOffences.first { it.hoCode?.code == offence.homeOfficeStatsCode }.hoCode
    }
    return newNomisHoCodes.first { it.code == offence.homeOfficeStatsCode }
  }

  private fun homeOfficeCodeExists(
    nomisOffences: List<PrisonApiOffence>,
    offence: EntityOffence,
  ) = nomisOffences.any { it.hoCode?.code == offence.homeOfficeStatsCode }

  private fun findAssociatedStatute(
    offence: EntityOffence,
    nomisOffences: List<PrisonApiOffence>,
    newStatutes: Set<Statute>,
  ): Statute {
    if (statuteExists(nomisOffences, offence)) {
      return nomisOffences.first { it.statuteCode.code == offence.statuteCode }.statuteCode
    }
    return newStatutes.first { it.code == offence.statuteCode }
  }

  private fun getAllNomisOffencesThatStartWith(alphaChar: String): Pair<Map<Pair<String, String>, PrisonApiOffence>, List<PrisonApiOffence>> {
    val nomisOffences: List<PrisonApiOffence> = findNomisOffencesThatStartWith(alphaChar)
    return nomisOffences.associateBy { it.code to it.statuteCode.code } to nomisOffences
  }

  private fun findNomisOffencesThatStartWith(alphaChar: String): List<PrisonApiOffence> {
    var pageNumber = 0
    var totalPages = 1
    val nomisOffences: MutableList<PrisonApiOffence> = mutableListOf()
    while (pageNumber < totalPages) {
      val response = prisonApiClient.findByOffenceCodeStartsWith(alphaChar, pageNumber)
      totalPages = response.totalPages
      pageNumber++
      nomisOffences.addAll(response.content)
    }
    return nomisOffences
  }

  private fun getAllNomisOffencecsForNonAlphaCache(offences: List<EntityOffence>): Pair<Map<Pair<String, String>, PrisonApiOffence>, List<PrisonApiOffence>> {
    val offencesStartWith = offences.map { it.code.first() }.toSet()
    val nomisOffences: MutableList<PrisonApiOffence> = mutableListOf()
    offencesStartWith.forEach {
      nomisOffences.addAll(findNomisOffencesThatStartWith(it.toString()))
    }
    return nomisOffences.associateBy { it.code to it.statuteCode.code } to nomisOffences
  }

  fun findOffenceById(offenceId: Long): Offence {
    val offence = offenceRepository.findById(offenceId)
      .orElseThrow { EntityNotFoundException("Offence not found with ID $offenceId") }
    val children = offenceRepository.findByParentOffenceId(offenceId)
    val offenceMappings = offenceScheduleMappingRepository.findByOffenceId(offenceId)
    val populatedOffence = transform(offence, children.map { it.id })
    return populatedOffence.copy(schedules = transform(offenceMappings))
  }

  fun searchOffences(searchString: String): List<Offence> {
    val hoCodeMatch = Regex("[0-9][0-9]?[0-9]?/[0-9][0-9]?")
    if (searchString.matches(hoCodeMatch)) {
      return searchByHoCode(searchString)
    }
    return populateOffenceModel(
      offenceRepository.findByCodeStartsWithIgnoreCaseOrCjsTitleContainsIgnoreCaseOrLegislationContainsIgnoreCase(
        searchString,
        searchString,
        searchString,
      ),
    )
  }

  private fun searchByHoCode(searchString: String): List<Offence> {
    val (category, subCategory) = searchString.split("/").map { it.toInt() }.let { Pair(it[0], it[1]) }
    return populateOffenceModel(offenceRepository.findByCategoryAndSubCategory(category, subCategory))
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private const val MAX_RECORDS_IN_POST_PAYLOAD = 100
  }
}
