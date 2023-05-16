package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class OffenceReactivatedInNomis(
  @Id
  val offenceId: Long = -1,
  val offenceCode: String,
  val reactivatedByUsername: String,
  val reactivatedDate: LocalDateTime = LocalDateTime.now(),
)
