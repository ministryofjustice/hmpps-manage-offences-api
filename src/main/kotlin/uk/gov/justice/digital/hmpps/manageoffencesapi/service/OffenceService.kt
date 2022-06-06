package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiHoCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiStatute
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence as EntityOffence

@Service
class OffenceService(
  private val offenceRepository: OffenceRepository,
  private val sdrsLoadResultRepository: SdrsLoadResultRepository,
  private val prisonApiClient: PrisonApiClient,
) {
  fun findOffencesByCode(code: String): List<Offence> {
    log.info("Fetching offences by offenceCode")
    return offenceRepository.findByCodeStartsWithIgnoreCase(code).map { transform(it) }
  }

  fun findLoadResults(): List<MostRecentLoadResult> {
    log.info("Fetching offences by offenceCode")
    return sdrsLoadResultRepository.findAllByOrderByAlphaCharAsc().map { transform(it) }
  }

  @Transactional(readOnly = true)
  fun fullSyncWithNomis() {
    ('A'..'Z').forEach { alphaChar ->
      log.info("Starting full sync with NOMIS for alphaChar {} ", alphaChar)
      fullySyncOffenceGroupWithNomis(alphaChar.toString())
    }
  }

  private fun fullySyncOffenceGroupWithNomis(alphaChar: String) {
    val offencesByCode = offenceRepository.findByCodeStartsWithIgnoreCase(alphaChar).associateBy { it.code }
    val (nomisOffencesById, nomisOffences) = getAllNomisOffencecsForAlphaChar(alphaChar)

    // the keys here represent the NOMIS keys, each key is a pair of the offenceCode and the statuteCode
    val (existingOffenceKeys, newOffenceKeys) = offencesByCode.keys.map { Pair(it, offencesByCode[it]!!.statuteCode) }
      .partition { nomisOffencesById.containsKey(it) }

    val newNomisStatutes = mutableSetOf<PrisonApiStatute>()
    val newNomisHoCodes = mutableSetOf<PrisonApiHoCode>()
    val newNomisOffences = mutableSetOf<PrisonApiOffence>()
    val updatedNomisOffences = mutableSetOf<PrisonApiOffence>()

    newOffenceKeys.forEach {
      val offence = offencesByCode[it.first]!!
      val statute = createStatuteInNomis(offence, nomisOffences)
      if (!statuteExists(nomisOffences, offence)) newNomisStatutes.add(statute)
      val homeOfficeCode = createHomeOfficeCodeInNomis(offence, nomisOffences)
      if (homeOfficeCode != null && !homeOfficeCodeExists(nomisOffences, offence)) newNomisHoCodes.add(homeOfficeCode)
      newNomisOffences.add(copyOffenceToCreate(offence, statute, homeOfficeCode))
    }

    existingOffenceKeys.forEach {
      val offence = offencesByCode[it.first]!!
      val nomisOffence = nomisOffencesById[it]!!
      if (!offenceDetailsSame(offence, nomisOffence)) {
        val homeOfficeCode = createHomeOfficeCodeInNomis(offence, nomisOffences)
        if (homeOfficeCode != null && !homeOfficeCodeExists(nomisOffences, offence)) newNomisHoCodes.add(homeOfficeCode)
        updatedNomisOffences.add(copyOffenceToUpdate(offence, nomisOffence, homeOfficeCode))
      }
    }

    createStatutes(newNomisStatutes)
    createHomeOfficeCodes(newNomisHoCodes)
    createNomisOffences(newNomisOffences)
    updateNomisOffences(updatedNomisOffences)
  }

  private fun createStatutes(statutesToCreate: Set<PrisonApiStatute>) {
    if (statutesToCreate.isNotEmpty()) {
      statutesToCreate.chunked(MAX_RECORDS_IN_POST_PAYLOAD).forEach {
        prisonApiClient.createStatutes(it)
      }
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
      nomisOffence.activeFlag == isOffenceActive(offence.endDate)

  private fun copyOffenceToUpdate(
    offence: EntityOffence,
    nomisOffence: PrisonApiOffence,
    homeOfficeCode: PrisonApiHoCode?
  ): PrisonApiOffence {
    log.info("Offence code {} to be updated in NOMIS", offence.code)
    return nomisOffence.copy(
      description = offence.derivedDescription,
      hoCode = homeOfficeCode,
      activeFlag = isOffenceActive(offence.endDate),
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
      activeFlag = isOffenceActive(offence.endDate),
    )
  }

  private fun isOffenceActive(endDate: LocalDate?): String {
    if (endDate == null || endDate.isAfter(LocalDate.now())) return "Y"
    return "N"
  }

  private fun createHomeOfficeCodeInNomis(
    offence: EntityOffence,
    nomisOffences: List<PrisonApiOffence>
  ): PrisonApiHoCode? {
    if (offence.homeOfficeStatsCode == null) return null
    if (homeOfficeCodeExists(nomisOffences, offence)) {
      return nomisOffences.find { it.hoCode?.code == offence.homeOfficeStatsCode }!!.hoCode
    }
    log.info("Home office code {} to be created in NOMIS", offence.homeOfficeStatsCode)
    return PrisonApiHoCode(
      code = offence.homeOfficeStatsCode!!,
      description = offence.homeOfficeStatsCode!!,
      activeFlag = "Y"
    )
  }

  private fun homeOfficeCodeExists(
    nomisOffences: List<PrisonApiOffence>,
    offence: uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
  ) = nomisOffences.any { it.hoCode?.code == offence.homeOfficeStatsCode }

  private fun createStatuteInNomis(
    offence: EntityOffence,
    nomisOffences: List<PrisonApiOffence>
  ): PrisonApiStatute {
    if (statuteExists(nomisOffences, offence)) {
      return nomisOffences.find { it.statuteCode.code == offence.statuteCode }!!.statuteCode
    }
    log.info("Statute code {} to be created in NOMIS", offence.statuteCode)
    return PrisonApiStatute(
      code = offence.statuteCode,
      description = offence.actsAndSections ?: offence.statuteCode,
      legislatingBodyCode = "UK",
      activeFlag = "Y"
    )
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
