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
  val code: String? = null,
  val description: String? = null,
  val cjsTitle: String? = null,
  val revisionId: Int? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val homeOfficeStatsCode: String? = null,
  val changedDate: LocalDateTime? = null,
  val createdDate: LocalDateTime = LocalDateTime.now(),
  val lastUpdatedDate: LocalDateTime = LocalDateTime.now(),
)
