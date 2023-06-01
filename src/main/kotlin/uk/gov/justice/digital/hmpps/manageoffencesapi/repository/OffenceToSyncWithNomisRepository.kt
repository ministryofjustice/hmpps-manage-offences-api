package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceToSyncWithNomis
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisSyncType

@Repository
interface OffenceToSyncWithNomisRepository : JpaRepository<OffenceToSyncWithNomis, Long> {
  fun existsByOffenceCodeAndNomisSyncType(offenceCode: String, nomisSyncType: NomisSyncType): Boolean
}
