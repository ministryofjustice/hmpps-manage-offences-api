package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class NomisScheduleMapping(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  val schedulePartId: Long,
  val nomisScheduleName: String
)
