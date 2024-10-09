package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule

@Repository
interface ScheduleRepository : JpaRepository<Schedule, Long> {
  fun findOneByActAndCode(act: String, code: String): Schedule?

  @Query(
    """
    SELECT o.code FROM Schedule s 
        JOIN SchedulePart sp on sp.schedule = s 
        JOIN OffenceScheduleMapping osm on osm.schedulePart = sp 
        JOIN Offence o ON osm.offence = o 
    WHERE s.code = :scheduleCode
   """,
  )
  fun getToreraOffenceCodes(scheduleCode: String): List<String>
}
