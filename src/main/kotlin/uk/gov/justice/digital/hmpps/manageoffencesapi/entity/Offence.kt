package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table
data class Offence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  val code: String,
  val description: String? = null,
  val cjsTitle: String? = null,
  val revisionId: Int,
  val startDate: LocalDate,
  val endDate: LocalDate? = null,
  val category: Int? = null,
  val subCategory: Int? = null,
  val offenceType: String? = null,
  @Column(name = "ACTS_AND_SECTIONS")
  val legislation: String? = null,
  val parentOffenceId: Long? = null,
  @Enumerated(EnumType.STRING)
  val sdrsCache: SdrsCache,
  val changedDate: LocalDateTime,
  val createdDate: LocalDateTime = LocalDateTime.now(),
  val lastUpdatedDate: LocalDateTime = LocalDateTime.now(),
  // The maxPeriodIsLife and maxPeriodOfIndictmentYears columns are populated via the schedule data supplied by the HMCTS NSD team
  val maxPeriodIsLife: Boolean? = false,
  val maxPeriodOfIndictmentYears: Int? = null,
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
      if (legislation.isNullOrBlank()) return statuteCode
      return legislation
    }
  val activeFlag: String
    get() {
      if (endDate == null || endDate.isAfter(LocalDate.now())) return "Y"
      return "N"
    }
  val expiryDate: LocalDate?
    get() {
      if (endDate == null || endDate.isAfter(LocalDate.now())) return null
      return LocalDate.now()
    }
  val parentCode: String?
    get() {
      if (code.length < 8) return null
      return code.substring(0, 7)
    }
}
