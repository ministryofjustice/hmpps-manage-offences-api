package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class LegacySdrsHoCodeMapping(
  @Id
  val offenceCode: String,
  val category: Int? = null,
  val subCategory: Int? = null,
)
