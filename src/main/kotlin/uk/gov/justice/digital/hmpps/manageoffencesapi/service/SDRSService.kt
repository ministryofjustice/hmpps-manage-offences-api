package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResultHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.DELTA_SYNC_SDRS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_SDRS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.FAIL
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus.SUCCESS
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.FULL_LOAD
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType.UPDATE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetApplications
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetControlTable
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetMojOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsErrorCodes.SDRS_99918
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GatewayOperationTypeRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetApplicationRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetControlTableRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetMojOffenceRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.GetOffenceRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageBodyRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageHeader
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.MessageID
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSRequest
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultHistoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence as EntityOffence

@Service
class SDRSService(
  private val sdrsApiClient: SDRSApiClient,
  private val offenceRepository: OffenceRepository,
  private val sdrsLoadResultRepository: SdrsLoadResultRepository,
  private val sdrsLoadResultHistoryRepository: SdrsLoadResultHistoryRepository,
  private val offenceScheduleMappingRepository: OffenceScheduleMappingRepository,
  private val offenceService: OffenceService,
  private val adminService: AdminService,
  private val eventService: EventService,
) {
  @Scheduled(cron = "0 0 */1 * * *")
  @SchedulerLock(name = "fullSynchroniseWithSdrsLock")
  @Transactional
  fun fullSynchroniseWithSdrs() {
    if (!adminService.isFeatureEnabled(FULL_SYNC_SDRS)) {
      log.info("FULL Sync with SDRS not running - disabled")
      return
    }
    log.info("The 'Synchronise with SDRS' job is performing a full load")
    loadAllOffences()
  }

  @Scheduled(cron = "0 */10 * * * *")
  @SchedulerLock(name = "deltaSynchroniseWithSdrsLock")
  @Transactional
  fun deltaSynchroniseWithSdrs() {
    if (!adminService.isFeatureEnabled(DELTA_SYNC_SDRS) || adminService.isFeatureEnabled(FULL_SYNC_SDRS)) {
      log.info("DELTA Sync with SDRS not running - delta sync disabled or full sync is enabled")
      return
    }

    val sdrsLoadResults = sdrsLoadResultRepository.findAll()

    log.info("The 'Synchronise with SDRS' job is checking for any updates since the last load")
    loadOffenceUpdates(sdrsLoadResults)
  }

  private fun loadAllOffences() {
    // These offenceToScheduleParts mappings will be re-inserted after all offences have been re-loaded (from SDRS)
    val offenceToScheduleMappings = offenceScheduleMappingRepository.findAll()
    resetLoadResultAndDeleteOffences()

    val loadDate = LocalDateTime.now()
    (SdrsCache.values()).forEach { cache ->
      log.info("Starting full load for cache {} ", cache)
      if (cache.isPrimaryCache) fullLoadPrimaryCache(cache, loadDate)
      else fullLoadSecondaryCache(cache, loadDate)
    }

    offenceToScheduleMappings.forEach {
      val offence = offenceRepository.findOneByCode(it.offence.code)
        .orElseThrow { EntityNotFoundException("Offence code ${it.offence.code} missing that was previously assigned to a schedule") }
      offenceScheduleMappingRepository.save(transform(offence, it))
    }
  }

  private fun setParentOffences(sdrsCache: SdrsCache) {
    val offences = offenceRepository.findChildOffencesWithNoParent(sdrsCache)
    offences
      .filter { it.parentCode != null }
      .forEach { child ->
        offenceRepository.findOneByCode(child.parentCode!!).ifPresent { parent ->
          offenceRepository.save(child.copy(parentOffenceId = parent.id))
        }
      }
  }

  private fun resetLoadResultAndDeleteOffences() {
    SdrsCache.values().forEach { cache ->
      sdrsLoadResultRepository.save(SdrsLoadResult(cache = cache))
    }
    offenceScheduleMappingRepository.deleteAll()
    offenceRepository.deleteByParentOffenceIdIsNotNull()
    offenceRepository.deleteAll()
    offenceRepository.flush()
    offenceScheduleMappingRepository.flush()
  }

  private fun makeControlTableRequest(changedDateTime: LocalDateTime): SDRSResponse {
    log.info("Making a control table request from SDRS")
    val sdrsRequest = createControlTableRequest(changedDateTime)
    return sdrsApiClient.callSDRS(sdrsRequest)
  }

  private fun fullLoadPrimaryCache(cache: SdrsCache, loadDate: LocalDateTime?) {
    try {
      val sdrsResponse = getSdrsResponse(cache)
      if (sdrsResponse.messageStatus.status == "ERRORED") {
        handleSdrsError(sdrsResponse, cache, loadDate, FULL_LOAD)
      } else {
        val latestOfEachOffence = getLatestOfEachOffence(sdrsResponse, cache)
        offenceRepository.saveAll(latestOfEachOffence.map { transform(it, cache) })
        saveLoad(cache, loadDate, SUCCESS, FULL_LOAD)
        setParentOffences(cache)
      }
    } catch (e: Exception) {
      log.error("Failed to do a full load from SDRS for cache {} - error message = {}", cache, e.message)
      handleSdrsError(cache = cache, loadDate = loadDate, loadType = FULL_LOAD)
    }
  }

  private fun fullLoadSecondaryCache(cache: SdrsCache, loadDate: LocalDateTime?) {
    try {
      val sdrsResponse = getSdrsResponse(cache)
      if (sdrsResponse.messageStatus.status == "ERRORED") {
        handleSdrsError(sdrsResponse, cache, loadDate, FULL_LOAD)
      } else {
        val latestOfEachOffence = getLatestOfEachOffence(sdrsResponse, cache)
        val duplicateOffences = offenceRepository.findByCodeIn(latestOfEachOffence.map { it.code }.toSet())
        val (inserts, updates) = extractInsertsAndUpdates(latestOfEachOffence, duplicateOffences)
        offenceRepository.saveAll(inserts.map { transform(it, cache) })
        processUpdatesForOffencesThatExistInAnotherCache(updates, duplicateOffences, cache)
        saveLoad(cache, loadDate, SUCCESS, FULL_LOAD)
        setParentOffences(cache)
      }
    } catch (e: Exception) {
      log.error("Failed to do a full load from SDRS for cache {} - error message = {}", cache, e.message)
      handleSdrsError(cache = cache, loadDate = loadDate, loadType = FULL_LOAD)
    }
  }

  private fun processUpdatesForOffencesThatExistInAnotherCache(
    updates: List<Offence>,
    duplicateOffences: List<uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence>,
    cache: SdrsCache
  ) {
    updates.forEach {
      val offenceToUpdate = duplicateOffences.first { d -> it.code == d.code }
      log.info(
        "Offence {} is being updated and the associated cache is also being changed, original cache {}, new cache {}",
        it.code,
        offenceToUpdate.sdrsCache,
        cache
      )
      offenceRepository.save(transform(it, offenceToUpdate, cache))
    }
  }

  private fun extractInsertsAndUpdates(
    latestOfEachOffence: List<Offence>,
    duplicateOffences: List<EntityOffence>
  ): Pair<List<Offence>, List<Offence>> {
    val duplicateOffenceCodes = duplicateOffences.map { it.code }
    val (potentialUpdates, inserts) = latestOfEachOffence.partition { duplicateOffenceCodes.contains(it.code) }
    val updates =
      potentialUpdates.filter { it.offenceStartDate.isAfter(duplicateOffences.first { d -> d.code == it.code }.startDate) }
    return inserts to updates
  }

  private fun getSdrsResponse(cache: SdrsCache): SDRSResponse {
    if (cache.messageType == GetOffence) return findAllOffencesByCache(cache)
    if (cache.messageType == GetApplications) return findAllApplicationOffences()
    return findAllMojOffences()
  }

  private fun handleSdrsError(
    sdrsResponse: SDRSResponse? = null,
    cache: SdrsCache,
    loadDate: LocalDateTime?,
    loadType: LoadType
  ) {
    if (sdrsResponse == null) {
      log.error("An unexpected error occurred when calling SDRS for cache {}", cache)
      saveLoad(cache, loadDate, FAIL, loadType)
    } else if (sdrsResponse.messageStatus.code == SDRS_99918.errorCode) {
      // SDRS-99918 indicates the absence of a cache, however it also gets thrown when there are no offences matching the alphaChar
      // e.g. no offence codes start with Q; therefore handling as a 'success' here
      log.info(
        "SDRS-9918 thrown by SDRS service due to no cache for cache {} (treated as success with no offences)",
        cache
      )
      saveLoad(cache, loadDate, SUCCESS, loadType)
    } else {
      log.error("Request to SDRS API failed for cache {} ", cache)
      log.error("Response details: {}", sdrsResponse)
      saveLoad(cache, loadDate, FAIL, loadType)
    }
  }

  private fun loadOffenceUpdates(sdrsLoadResults: List<SdrsLoadResult>) {
    val lastLoadDateByCache = sdrsLoadResults.groupBy({ it.lastSuccessfulLoadDate }, { it.cache })
    val loadDate = LocalDateTime.now()
    val deltaSyncToNomisEnabled = adminService.isFeatureEnabled(DELTA_SYNC_NOMIS)
    lastLoadDateByCache.forEach { (lastSuccessfulLoadDate, affectedCaches) ->
      if (lastSuccessfulLoadDate == null) {
        // This code should never be called - it will only run if the initial full load failed on any of the caches
        fullLoadOfCachesDuringUpdate(affectedCaches, loadDate, deltaSyncToNomisEnabled)
      } else {
        val updatedCaches = getUpdatedCachesSinceLastLoadDate(lastSuccessfulLoadDate)
        val cachesToUpdate = affectedCaches intersect updatedCaches
        log.info("Caches to update are {}", cachesToUpdate)
        cachesToUpdate.forEach { cache ->
          updateSingleCache(cache, lastSuccessfulLoadDate, loadDate, deltaSyncToNomisEnabled)
        }
      }
    }
  }

  private fun fullLoadOfCachesDuringUpdate(
    affectedCaches: List<SdrsCache>,
    loadDate: LocalDateTime?,
    deltaSyncToNomisEnabled: Boolean
  ) {
    affectedCaches.forEach {
      log.info("Cache has not been previously loaded, so attempting full load of {} in update job", it)
      if (it.isPrimaryCache) fullLoadPrimaryCache(it, loadDate)
      else fullLoadSecondaryCache(it, loadDate)
      if (deltaSyncToNomisEnabled) offenceService.fullySyncWithNomis(it)
    }
  }

  private fun updateSingleCache(
    cache: SdrsCache,
    lastLoadDate: LocalDateTime,
    loadDate: LocalDateTime?,
    deltaSyncToNomisEnabled: Boolean
  ) {
    log.info("Starting update load for cache {} ", cache)
    try {
      val sdrsResponse = findUpdatedOffences(cache, lastLoadDate)
      if (sdrsResponse.messageStatus.status == "ERRORED") {
        handleSdrsError(sdrsResponse, cache, loadDate, UPDATE)
      } else {
        val latestOfEachOffence = getLatestOfEachOffence(sdrsResponse, cache)
        latestOfEachOffence.forEach {
          offenceRepository.findOneByCode(it.code)
            .ifPresentOrElse(
              { offenceToUpdate ->
                // This condition can only be false if the offence is in two different caches (edge case on delta load)
                if (it.offenceStartDate.isAfter(offenceToUpdate.startDate))
                  offenceRepository.save(transform(it, offenceToUpdate, cache))
              },
              { offenceRepository.save(transform(it, cache)) }
            )
          sendOffenceChangedEvent(it)
        }
        saveLoad(cache, loadDate, SUCCESS, UPDATE)
        setParentOffences(cache)
        if (deltaSyncToNomisEnabled) offenceService.fullySyncWithNomis(cache)
      }
    } catch (e: Exception) {
      log.error(
        "Failed for updating a single cache from SDRS for cache {} - error message = {}", cache, e.message
      )
      handleSdrsError(cache = cache, loadDate = loadDate, loadType = UPDATE)
    }
  }

  private fun sendOffenceChangedEvent(it: Offence) {
    runCatching {
      eventService.publishOffenceChangedEvent(it.code)
    }.onFailure { error ->
      log.error(
        "Failed to send changed-event for offence code  ${it.code}",
        error
      )
    }
  }

  private fun getLatestOfEachOffence(sdrsResponse: SDRSResponse, sdrsCache: SdrsCache): List<Offence> {
    val allOffences = extractOffencesfromResponse(sdrsResponse, sdrsCache)
    log.info("Fetched {} records from SDRS for cache {} ", allOffences.size, sdrsCache)
    val latestOfEachOffence = allOffences.groupBy { it.code }.map {
      it.value.sortedByDescending { offence -> offence.offenceStartDate }[0]
    }
    return latestOfEachOffence
  }

  private fun extractOffencesfromResponse(sdrsResponse: SDRSResponse, cache: SdrsCache): List<Offence> {
    if (cache.messageType == GetOffence) return sdrsResponse.messageBody.gatewayOperationType.getOffenceResponse!!.offences
    if (cache.messageType == GetApplications) return sdrsResponse.messageBody.gatewayOperationType.getApplicationsResponse!!.offences
    return sdrsResponse.messageBody.gatewayOperationType.mojOffenceResponse!!.offences
  }

  private fun getUpdatedCachesSinceLastLoadDate(lastLoadDate: LocalDateTime): Set<SdrsCache> {
    val controlResults = makeControlTableRequest(lastLoadDate)
    val offenceRelatedDataSetNames = SdrsCache.values().map { it.sdrsDataSetName }
    return controlResults.messageBody.gatewayOperationType.getControlTableResponse!!.referenceDataSet.filter {
      offenceRelatedDataSetNames.contains(it.dataSet)
    }.map { SdrsCache.fromSdrsDataSetName(it.dataSet) }.toSet()
  }

  private fun saveLoad(cache: SdrsCache, loadDate: LocalDateTime?, status: LoadStatus, type: LoadType) {
    val loadStatusExisting = sdrsLoadResultRepository.findById(cache)
      .orElseThrow { EntityNotFoundException("No record exists for cache $cache") }
    val loadStatus = if (status == SUCCESS) {
      loadStatusExisting.copy(
        status = status, loadType = type, loadDate = loadDate, lastSuccessfulLoadDate = loadDate
      )
    } else {
      loadStatusExisting.copy(
        status = status,
        loadType = type,
        loadDate = loadDate,
      )
    }
    sdrsLoadResultRepository.save(loadStatus)

    val loadStatusHistory = SdrsLoadResultHistory(
      cache = cache,
      status = status,
      loadType = type,
      loadDate = loadDate,
    )
    sdrsLoadResultHistoryRepository.save(loadStatusHistory)
  }

  private fun createSDRSRequest(gatewayOperationTypeRequest: GatewayOperationTypeRequest, messageType: MessageType) =
    SDRSRequest(
      messageHeader = MessageHeader(
        messageID = MessageID(
          uuid = UUID.randomUUID(), relatesTo = ""
        ),
        timeStamp = ZonedDateTime.now(), messageType = messageType, from = "CONSUMER_APPLICATION", to = "SDRS_AZURE"
      ),
      messageBody = MessageBodyRequest(
        gatewayOperationTypeRequest
      )
    )

  private fun createOffenceRequest(
    offenceCode: String? = null,
    changedDate: LocalDateTime? = null,
    sdrsCache: SdrsCache,
    allOffences: String = "ALL"
  ) = createSDRSRequest(
    GatewayOperationTypeRequest(
      getOffenceRequest = GetOffenceRequest(
        cjsCode = offenceCode,
        alphaChar = sdrsCache.alphaChar,
        allOffences = allOffences,
        changedDate = changedDate,
      )
    ),
    sdrsCache.messageType
  )

  private fun createMojOffenceRequest(
    offenceCode: String? = null,
    changedDate: LocalDateTime? = null,
    allOffences: String = "ALL"
  ) = createSDRSRequest(
    GatewayOperationTypeRequest(
      getMojOffenceRequest = GetMojOffenceRequest(
        cjsCode = offenceCode,
        allOffences = allOffences,
        changedDate = changedDate,
      )
    ),
    GetMojOffence
  )

  private fun createApplicationOffenceRequest(
    offenceCode: String? = null,
    changedDate: LocalDateTime? = null,
    allOffences: String = "ALL"
  ) = createSDRSRequest(
    GatewayOperationTypeRequest(
      getApplicationRequest = GetApplicationRequest(
        cjsCode = offenceCode,
        allOffences = allOffences,
        changedDate = changedDate,
      )
    ),
    GetApplications
  )

  private fun createControlTableRequest(changedDateTime: LocalDateTime) = createSDRSRequest(
    GatewayOperationTypeRequest(
      getControlTableRequest = GetControlTableRequest(
        changedDateTime = changedDateTime
      )
    ),
    GetControlTable
  )

  private fun findAllOffencesByCache(sdrsCache: SdrsCache): SDRSResponse {
    val sdrsRequest = createOffenceRequest(sdrsCache = sdrsCache)
    return sdrsApiClient.callSDRS(sdrsRequest)
  }

  private fun findAllMojOffences(): SDRSResponse {
    val sdrsRequest = createMojOffenceRequest()
    return sdrsApiClient.callSDRS(sdrsRequest)
  }

  private fun findAllApplicationOffences(): SDRSResponse {
    val sdrsRequest = createApplicationOffenceRequest()
    return sdrsApiClient.callSDRS(sdrsRequest)
  }

  private fun findUpdatedOffences(cache: SdrsCache, lastUpdatedDate: LocalDateTime): SDRSResponse {
    if (cache.messageType == GetOffence) return sdrsApiClient.callSDRS(
      createOffenceRequest(
        sdrsCache = cache,
        changedDate = lastUpdatedDate
      )
    )
    if (cache.messageType == GetApplications) return sdrsApiClient.callSDRS(createApplicationOffenceRequest(changedDate = lastUpdatedDate))
    return sdrsApiClient.callSDRS(createMojOffenceRequest(changedDate = lastUpdatedDate))
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
