package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class OffenceToScheduleHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  val scheduleCode: String,
  val schedulePartId: Long,
  val schedulePartNumber: Int,
  val offenceId: Long,
  val offenceCode: String,
  @Enumerated(EnumType.STRING)
  val changeType: ChangeType,
  val pushedToNomis: Boolean = false,
  val createdDate: LocalDateTime = LocalDateTime.now(),
)
