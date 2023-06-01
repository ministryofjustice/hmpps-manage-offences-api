package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table
data class OffenceReactivatedInNomis(
  @Id
  val offenceCode: String,
  val reactivatedByUsername: String,
  val reactivatedDate: LocalDateTime = LocalDateTime.now(),
)
