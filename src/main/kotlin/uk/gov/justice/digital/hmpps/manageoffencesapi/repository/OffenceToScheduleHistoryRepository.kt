package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceToScheduleHistory

@Repository
interface OffenceToScheduleHistoryRepository : JpaRepository<OffenceToScheduleHistory, Long> {
  fun findByPushedToNomisOrderByCreatedDateDesc(pushedToNomis: Boolean): List<OffenceToScheduleHistory>
}
