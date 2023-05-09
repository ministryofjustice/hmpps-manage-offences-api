package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class HomeOfficeCode(
  @Id
  val id: String = "",
  val category: Int,
  val subCategory: Int,
  val description: String,
) {
  val homeOfficeStatsCode: String
    get() {
      return category.toString().padStart(3, '0') + "/" + subCategory.toString().padStart(2, '0')
    }
}
