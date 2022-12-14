package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache

@Repository
interface SdrsLoadResultRepository : JpaRepository<SdrsLoadResult, SdrsCache> {
  fun findAllByOrderByCacheAsc(): List<SdrsLoadResult>
}
