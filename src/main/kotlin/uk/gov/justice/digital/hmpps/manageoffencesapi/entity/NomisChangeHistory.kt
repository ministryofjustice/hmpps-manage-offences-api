package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisChangeType
import java.time.LocalDateTime

@Entity
@Table
data class NomisChangeHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,
  val code: String,
  val description: String,
  @Enumerated(EnumType.STRING)
  val changeType: ChangeType,
  @Enumerated(EnumType.STRING)
  val nomisChangeType: NomisChangeType,
  val sentToNomisDate: LocalDateTime = LocalDateTime.now(),
)
