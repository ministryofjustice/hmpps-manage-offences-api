package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.LegacySdrsHoCodeMapping

@Repository
interface LegacySdrsHoCodeMappingRepository : JpaRepository<LegacySdrsHoCodeMapping, String>
