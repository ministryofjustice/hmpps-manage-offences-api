package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class Offence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  val code: String,
  val description: String? = null,
  val cjsTitle: String? = null,
  val revisionId: Int? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val category: Int? = null,
  val subCategory: Int? = null,
  val actsAndSections: String? = null,
  val parentOffenceId: Long? = null,
  val changedDate: LocalDateTime? = null,
  val createdDate: LocalDateTime = LocalDateTime.now(),
  val lastUpdatedDate: LocalDateTime = LocalDateTime.now(),
) {
  val statuteCode
    get() = code.substring(0, 4)
  val homeOfficeStatsCode: String?
    get() {
      if (category == null && subCategory == null) return null
      if (subCategory == null) return category.toString().padStart(3, '0') + "/"
      if (category == null) return "/" + subCategory.toString().padStart(2, '0')
      return category.toString().padStart(3, '0') + "/" + subCategory.toString().padStart(2, '0')
    }
  val derivedDescription: String
    get() = (cjsTitle ?: description)!!
  val statuteDescription: String
    get() {
      if (actsAndSections.isNullOrBlank()) return statuteCode
      return actsAndSections
    }
  val activeFlag: String
    get() {
      if (endDate == null || endDate.isAfter(LocalDate.now())) return "Y"
      return "N"
    }

  val parentCode: String?
    get() {
      if (code.length < 8) return null
      return code.substring(0, 7)
    }
}
