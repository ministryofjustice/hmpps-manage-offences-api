package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.NomisChangeHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.NomisChangeHistoryRepository
import java.time.LocalDate
import java.time.LocalTime

@Service
class LoadHistoryService(
  private val nomisChangeHistoryRepository: NomisChangeHistoryRepository,
) {
  fun getNomisChangeHistory(from: LocalDate, to: LocalDate?): List<NomisChangeHistory> {
    to?.let {
      return nomisChangeHistoryRepository.findBySentToNomisDateBetweenOrderBySentToNomisDateDesc(
        from.atStartOfDay(),
        it.atTime(LocalTime.MAX)
      )
        .map { h -> transform(h) }
    }
    return nomisChangeHistoryRepository.findBySentToNomisDateAfterOrderBySentToNomisDateDesc(from.atStartOfDay())
      .map { transform(it) }
  }


  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
