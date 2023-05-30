package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table
data class PreviousOffenceToHoCodeMapping(
  @Id
  val offenceCode: String,
  val category: Int,
  val subCategory: Int,
) {
  val homeOfficeCode: String
    get() {
      return category.toString().padStart(3, '0') + "/" + subCategory.toString().padStart(2, '0')
    }
}
