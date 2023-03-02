package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping

@Repository
interface OffenceScheduleMappingRepository : JpaRepository<OffenceScheduleMapping, Long> {
  fun findBySchedulePartScheduleId(scheduleId: Long): List<OffenceScheduleMapping>
  fun findByOffenceIdIn(offenceId: List<Long>): List<OffenceScheduleMapping>
  fun findByOffenceId(offenceId: Long): List<OffenceScheduleMapping>
  fun deleteBySchedulePartIdAndOffenceId(schedulePartId: Long, offenceId: Long): Long
}
