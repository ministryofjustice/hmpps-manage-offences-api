package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SdrsLoadStatusRepository

@Service
class OffenceService(
  private val offenceRepository: OffenceRepository,
  private val sdrsLoadStatusRepository: SdrsLoadStatusRepository,
) {
  fun findOffencesByCode(code: String): List<Offence> {
    log.info("Fetching offences by offenceCode")
    return offenceRepository.findByCodeStartsWithIgnoreCase(code).map { transform(it) }
  }

  fun findLoadResults(): List<MostRecentLoadResult> {
    log.info("Fetching offences by offenceCode")
    return sdrsLoadStatusRepository.findAll().map { transform(it) }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
