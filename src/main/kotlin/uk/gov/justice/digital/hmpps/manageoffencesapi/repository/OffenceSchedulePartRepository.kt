package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceSchedulePart
import java.util.Optional

@Repository
interface OffenceSchedulePartRepository : JpaRepository<OffenceSchedulePart, Long> {
  fun findBySchedulePartScheduleId(scheduleId: Long): List<OffenceSchedulePart>
  fun findByOffenceId(offenceId: Long): List<OffenceSchedulePart>
  fun findByOffenceIdIn(offenceId: List<Long>): List<OffenceSchedulePart>
  fun deleteBySchedulePartIdAndOffenceId(schedulePartId: Long, offenceId: Long)
  fun findOneBySchedulePartIdAndOffenceId(schedulePartId: Long, offenceId: Long): Optional<OffenceSchedulePart>
}
