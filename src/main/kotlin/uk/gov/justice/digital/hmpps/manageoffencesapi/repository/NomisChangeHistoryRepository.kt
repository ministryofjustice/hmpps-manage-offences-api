package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.NomisChangeHistory
import java.time.LocalDateTime

@Repository
interface NomisChangeHistoryRepository : JpaRepository<NomisChangeHistory, Long> {
  fun findBySentToNomisDateAfterOrderBySentToNomisDateDesc(from: LocalDateTime): List<NomisChangeHistory>
  fun findBySentToNomisDateBetweenOrderBySentToNomisDateDesc(
    from: LocalDateTime,
    to: LocalDateTime,
  ): List<NomisChangeHistory>
}
