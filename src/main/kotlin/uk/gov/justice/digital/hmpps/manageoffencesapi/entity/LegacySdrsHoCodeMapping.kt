package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table
data class LegacySdrsHoCodeMapping(
  @Id
  val offenceCode: String,
  val category: Int? = null,
  val subCategory: Int? = null,
)
