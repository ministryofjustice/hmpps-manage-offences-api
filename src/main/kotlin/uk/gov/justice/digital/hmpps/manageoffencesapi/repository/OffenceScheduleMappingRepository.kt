package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping

@Repository
interface OffenceScheduleMappingRepository : JpaRepository<OffenceScheduleMapping, Long> {
  fun findByScheduleParagraphSchedulePartScheduleId(scheduleId: Long): List<OffenceScheduleMapping>
  fun findByOffenceIdIn(offenceId: List<Long>): List<OffenceScheduleMapping>
  fun deleteByScheduleParagraphIdAndOffenceId(scheduleParagraphId: Long, offenceId: Long): Long
}
