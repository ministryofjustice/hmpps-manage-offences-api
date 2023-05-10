package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.HoCodesLoadHistory

@Repository
interface HoCodesLoadHistoryRepository : JpaRepository<HoCodesLoadHistory, Long> {
  fun findByLoadedFileIn(fileNames: Set<String>): Set<HoCodesLoadHistory>
}
