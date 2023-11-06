package uk.gov.justice.digital.hmpps.manageoffencesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import java.util.Optional

@Repository
interface OffenceRepository : JpaRepository<Offence, Long> {
  fun findByCodeStartsWithIgnoreCase(code: String): List<Offence>
  fun findByCodeStartsWithIgnoreCaseOrCjsTitleContainsIgnoreCaseOrLegislationContainsIgnoreCase(
    codeSearch: String,
    descriptionSearch: String,
    legislationSearch: String,
  ): List<Offence>

  fun findByCodeStartsWithIgnoreCaseOrCjsTitleContainsIgnoreCase(
    codeSearch: String,
    descriptionSearch: String,
  ): List<Offence>

  fun findByCategoryAndSubCategory(category: Int, subCategory: Int): List<Offence>

  fun findBySdrsCache(sdrsCache: SdrsCache): List<Offence>
  fun findOneByCode(code: String): Optional<Offence>

  @Query("SELECT o FROM Offence o WHERE o.sdrsCache = :sdrsCache AND LENGTH(o.code) > 7 AND o.parentOffenceId IS NULL")
  fun findChildOffencesWithNoParent(sdrsCache: SdrsCache): List<Offence>

  fun findByParentOffenceId(parentOffenceId: Long): List<Offence>

  fun findByParentOffenceIdIn(parentOffenceIds: Set<Long>): List<Offence>

  fun deleteByParentOffenceIdIsNotNull()

  fun findByCodeIgnoreCaseIn(codes: Set<String>): List<Offence>

  fun findByCodeIgnoreCase(code: String): Offence?

  fun findByCategoryIsNotNullAndSubCategoryIsNotNull(): List<Offence>
}
