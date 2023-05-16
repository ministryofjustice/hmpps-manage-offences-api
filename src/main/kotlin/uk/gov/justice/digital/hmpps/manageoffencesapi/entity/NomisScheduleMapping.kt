package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table
data class NomisScheduleMapping(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  val schedulePartId: Long,
  val nomisScheduleName: String,
)
