package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import java.util.Optional

@Repository
interface OffenceRepository : JpaRepository<Offence, Long> {
  fun findByCodeStartsWithIgnoreCase(code: String): List<Offence>
  fun findOneByCode(code: String): Optional<Offence>

  @Query("SELECT o FROM Offence o WHERE o.code LIKE :alphaChar% AND LENGTH(o.code) > 7 AND o.parentOffenceId IS NULL")
  fun findChildOffencesWithNoParent(alphaChar: Char): List<Offence>

  fun findByParentOffenceId(parentOffenceId: Long): List<Offence>

  fun findByParentOffenceIdIn(parentOffenceIds: List<Long>): List<Offence>
}
