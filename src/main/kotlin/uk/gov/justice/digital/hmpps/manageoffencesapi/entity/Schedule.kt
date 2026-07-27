package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ScheduleStatus

@Entity
@Table
data class Schedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,
  val act: String,
  val code: String,
  val url: String?,
  @Enumerated(EnumType.STRING)
  val status: ScheduleStatus = ScheduleStatus.LIVE,
)
