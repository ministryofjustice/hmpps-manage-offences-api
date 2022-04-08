package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository

@Service
class OffenceService(private val offenceRepository: OffenceRepository) {
  fun findOffencesByCode(code: String): List<Offence> {
    log.info("Fetching offences by offenceCode")
    return offenceRepository.findByCodeStartsWith(code).map { transform(it) }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
