package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiHoCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PrisonApiStatute
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadResultRepository
import java.time.LocalDate
import javax.transaction.Transactional
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

  @Transactional
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

    newOffenceKeys.forEach {
      val offence = offencesByCode[it.first]!!
      val statute = createStatuteInNomis(offence, nomisOffences)
      val homeOfficeCode = createHomeOfficeCodeInNomis(offence, nomisOffences)
      createOffenceInNomis(offence, statute, homeOfficeCode)
    }

    existingOffenceKeys.forEach {
      val offence = offencesByCode[it.first]!!
      val nomisOffence = nomisOffencesById[it]!!
      val homeOfficeCode = createHomeOfficeCodeInNomis(offence, nomisOffences)
      updateOffenceInNomis(offence, nomisOffence, homeOfficeCode)
    }
  }

  private fun updateOffenceInNomis(
    offence: EntityOffence,
    nomisOffence: PrisonApiOffence,
    homeOfficeCode: PrisonApiHoCode?
  ) {
    val nomisOffenceUpdated = nomisOffence.copy(
      description = offence.description,
      hoCode = homeOfficeCode,
      // severityRanking = "99", // TODO should derive this and how to set on update?
      activeFlag = isOffenceActive(offence.endDate),
    )
    prisonApiClient.updateOffence(nomisOffenceUpdated)
  }

  private fun createOffenceInNomis(
    offence: EntityOffence,
    statute: PrisonApiStatute,
    homeOfficeCode: PrisonApiHoCode?
  ) {
    val nomisOffence = PrisonApiOffence(
      code = offence.code,
      description = offence.description,
      statuteCode = statute,
      hoCode = homeOfficeCode,
      severityRanking = "99", // TODO should derive this?
      activeFlag = isOffenceActive(offence.endDate),
    )
    prisonApiClient.createOffence(nomisOffence)
  }

  private fun isOffenceActive(endDate: LocalDate?): String {
    if (endDate == null || endDate.isAfter(LocalDate.now())) return "Y"
    return "N"
  }

  private fun createHomeOfficeCodeInNomis(
    offence: EntityOffence,
    nomisOffences: List<PrisonApiOffence>
  ): PrisonApiHoCode? {
    // TODO Make sure format of hoCode is 6 chars both sides
    if (offence.homeOfficeStatsCode == null) return null
    // TODO replace this condition, get all ho-codes from prison-api (requires new endpoint) and only create one if the statute doesnt exist in the list
    if (nomisOffences.any { it.hoCode?.code == offence.homeOfficeStatsCode }) {
      return nomisOffences.find { it.hoCode?.code == offence.homeOfficeStatsCode }!!.hoCode
    }
    log.info("Creating home office code {} in NOMIS", offence.homeOfficeStatsCode)
    val nomisHoCode = PrisonApiHoCode(
      code = offence.homeOfficeStatsCode,
      description = offence.homeOfficeStatsCode,
      activeFlag = "Y"
    )

    prisonApiClient.createHomeOfficeCode(nomisHoCode)
    return nomisHoCode
  }

  private fun createStatuteInNomis(
    offence: EntityOffence,
    nomisOffences: List<PrisonApiOffence>
  ): PrisonApiStatute {
    // TODO replace this condition, get all statutes from prison-api (requires new endpoint) and only create one if the statute doesnt exist in the list
    if (nomisOffences.any { it.statuteCode.code == offence.statuteCode }) {
      return nomisOffences.find { it.statuteCode.code == offence.statuteCode }!!.statuteCode
    }
    log.info("Creating statute code {} in NOMIS", offence.statuteCode)
    val nomisStatute = PrisonApiStatute(
      code = offence.statuteCode,
      description = offence.statuteCode,
      legislatingBodyCode = "UK",
      activeFlag = "Y"
    )
    prisonApiClient.createStatute(nomisStatute)
    return nomisStatute
  }

  fun getAllNomisOffencecsForAlphaChar(alphaChar: String): Pair<Map<Pair<String, String>, PrisonApiOffence>, MutableList<PrisonApiOffence>> {
    var pageNumber = 0
    var totalPages = 1
    val nomisOffences: MutableList<PrisonApiOffence> = mutableListOf()
    while (pageNumber < totalPages) {
      val response = prisonApiClient.findByOffenceCodeStartsWith(alphaChar, 0)
      totalPages = response.totalPages
      pageNumber++
      nomisOffences.addAll(response.content)
    }
    return nomisOffences.associateBy { it.code to it.statuteCode.code } to nomisOffences
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
