package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature.FULL_SYNC_NOMIS
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiHoCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiStatute
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence as EntityOffence

@Service
class OffenceService(
  private val offenceRepository: OffenceRepository,
  private val sdrsLoadResultRepository: SdrsLoadResultRepository,
  private val prisonApiClient: PrisonApiClient,
  private val adminService: AdminService,
) {
  fun findOffencesByCode(code: String): List<Offence> {
    log.info("Fetching offences by offenceCode")
    return offenceRepository.findByCodeStartsWithIgnoreCase(code).map { transform(it) }
  }

  fun findLoadResults(): List<MostRecentLoadResult> {
    log.info("Fetching offences by offenceCode")
    return sdrsLoadResultRepository.findAllByOrderByAlphaCharAsc().map { transform(it) }
  }

  @Scheduled(cron = "0 0 */1 * * *")
  @Transactional(readOnly = true)
  fun fullSyncWithNomis() {
    if (!adminService.isFeatureEnabled(FULL_SYNC_NOMIS)) {
      log.info("Full sync with NOMIS not running - disabled")
      return
    }
    ('A'..'Z').forEach { alphaChar ->
      log.info("Starting full sync with NOMIS for alphaChar {} ", alphaChar)
      fullySyncOffenceGroupWithNomis(alphaChar.toString())
    }
  }

  fun fullySyncOffenceGroupWithNomis(alphaChar: String) {
    val allOffences = offenceRepository.findByCodeStartsWithIgnoreCase(alphaChar)
    val offencesByCode = allOffences.associateBy { it.code }
    val (nomisOffencesById, nomisOffences) = getAllNomisOffencecsForAlphaChar(alphaChar)

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

    existingOffenceKeys.forEach {
      val offence = offencesByCode[it.first]!!
      val nomisOffence = nomisOffencesById[it]!!
      if (!offenceDetailsSame(offence, nomisOffence)) {
        val homeOfficeCode = findAssociatedHomeOfficeCodeInNomis(offence, nomisOffences, newNomisHoCodes)
        updatedNomisOffences.add(copyOffenceToUpdate(offence, nomisOffence, homeOfficeCode))
      }
    }

    createStatutes(newNomisStatutes)
    createHomeOfficeCodes(newNomisHoCodes)
    createNomisOffences(newNomisOffences)
    updateNomisOffences(updatedNomisOffences)
  }

  private fun determineNewStatutesToCreate(
    allOffences: List<EntityOffence>,
    nomisOffences: MutableList<PrisonApiOffence>
  ): Set<PrisonApiStatute> {
    val offencesByNewStatuteCode = allOffences
      .filter { nomisOffences.none { o -> o.statuteCode.code == it.statuteCode } }
      .groupBy { it.statuteCode }

    return offencesByNewStatuteCode.map {
      val statuteDescription = it.value
        .sortedWith(
          compareByDescending(EntityOffence::activeFlag)
            .thenByDescending(EntityOffence::startDate)
        )
        .firstOrNull { offence -> offence.statuteDescription != offence.statuteCode }?.statuteDescription
      PrisonApiStatute(
        code = it.key,
        description = statuteDescription ?: it.key,
        legislatingBodyCode = "UK",
        activeFlag = "Y"
      )
    }.toSet()
  }

  private fun determineNewHoCodesToCreate(
    allOffences: List<EntityOffence>,
    nomisOffences: MutableList<PrisonApiOffence>
  ): Set<PrisonApiHoCode> =
    allOffences
      .filter { !it.homeOfficeStatsCode.isNullOrBlank() && nomisOffences.none { o -> o.hoCode?.code == it.homeOfficeStatsCode } }
      .map {
        PrisonApiHoCode(
          code = it.homeOfficeStatsCode!!,
          description = it.homeOfficeStatsCode!!,
          activeFlag = "Y"
        )
      }.toSet()

  private fun createStatutes(nomisStatutesToCreate: Set<PrisonApiStatute>) {
    nomisStatutesToCreate.chunked(MAX_RECORDS_IN_POST_PAYLOAD).forEach {
      prisonApiClient.createStatutes(it)
    }
  }

  private fun createHomeOfficeCodes(homeOfficeCodesToCreate: Set<PrisonApiHoCode>) {
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
      }
    }
  }

  private fun updateNomisOffences(nomisOffencedToUpdate: Set<PrisonApiOffence>) {
    if (nomisOffencedToUpdate.isNotEmpty()) {
      nomisOffencedToUpdate.chunked(MAX_RECORDS_IN_POST_PAYLOAD).forEach {
        prisonApiClient.updateOffences(it)
      }
    }
  }

  private fun statuteExists(
    nomisOffences: List<PrisonApiOffence>,
    offence: EntityOffence
  ) = nomisOffences.any { o -> o.statuteCode.code == offence.statuteCode }

  private fun offenceDetailsSame(offence: EntityOffence, nomisOffence: PrisonApiOffence): Boolean =
    nomisOffence.hoCode?.code == offence.homeOfficeStatsCode &&
      nomisOffence.description == offence.derivedDescription &&
      nomisOffence.activeFlag == offence.activeFlag

  private fun copyOffenceToUpdate(
    offence: EntityOffence,
    nomisOffence: PrisonApiOffence,
    homeOfficeCode: PrisonApiHoCode?
  ): PrisonApiOffence {
    log.info("Offence code {} to be updated in NOMIS", offence.code)
    return nomisOffence.copy(
      description = offence.derivedDescription,
      hoCode = homeOfficeCode,
      activeFlag = offence.activeFlag,
    )
  }

  private fun copyOffenceToCreate(
    offence: EntityOffence,
    statute: PrisonApiStatute,
    homeOfficeCode: PrisonApiHoCode?
  ): PrisonApiOffence {
    log.info("Offence code {} to be created in NOMIS", offence.code)
    return PrisonApiOffence(
      code = offence.code,
      description = offence.derivedDescription,
      statuteCode = statute,
      hoCode = homeOfficeCode,
      severityRanking = offence.category?.toString() ?: "99",
      activeFlag = offence.activeFlag,
    )
  }

  private fun findAssociatedHomeOfficeCodeInNomis(offence: EntityOffence, nomisOffences: List<PrisonApiOffence>, newNomisHoCodes: Set<PrisonApiHoCode>): PrisonApiHoCode? {
    if (offence.homeOfficeStatsCode == null) return null
    if (homeOfficeCodeExists(nomisOffences, offence)) {
      return nomisOffences.first { it.hoCode?.code == offence.homeOfficeStatsCode }.hoCode
    }
    return newNomisHoCodes.first { it.code == offence.homeOfficeStatsCode }
  }

  private fun homeOfficeCodeExists(
    nomisOffences: List<PrisonApiOffence>,
    offence: EntityOffence
  ) = nomisOffences.any { it.hoCode?.code == offence.homeOfficeStatsCode }

  private fun findAssociatedStatute(
    offence: EntityOffence,
    nomisOffences: List<PrisonApiOffence>,
    newStatutes: Set<PrisonApiStatute>
  ): PrisonApiStatute {
    if (statuteExists(nomisOffences, offence)) {
      return nomisOffences.first { it.statuteCode.code == offence.statuteCode }.statuteCode
    }
    return newStatutes.first { it.code == offence.statuteCode }
  }

  fun getAllNomisOffencecsForAlphaChar(alphaChar: String): Pair<Map<Pair<String, String>, PrisonApiOffence>, MutableList<PrisonApiOffence>> {
    var pageNumber = 0
    var totalPages = 1
    val nomisOffences: MutableList<PrisonApiOffence> = mutableListOf()
    while (pageNumber < totalPages) {
      val response = prisonApiClient.findByOffenceCodeStartsWith(alphaChar, pageNumber)
      totalPages = response.totalPages
      pageNumber++
      nomisOffences.addAll(response.content)
    }
    return nomisOffences.associateBy { it.code to it.statuteCode.code } to nomisOffences
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private const val MAX_RECORDS_IN_POST_PAYLOAD = 1
  }
}
