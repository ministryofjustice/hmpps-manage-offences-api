package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence

@Repository
interface OffenceRepository : JpaRepository<Offence, Long> {
  fun findByCodeStartsWith(code: String): List<Offence>
}
