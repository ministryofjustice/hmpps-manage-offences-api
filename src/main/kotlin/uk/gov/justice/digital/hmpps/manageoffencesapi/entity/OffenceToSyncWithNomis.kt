package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisSyncType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table
data class OffenceToSyncWithNomis(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  val offenceId: Long = -1,
  @Enumerated(EnumType.STRING)
  val nomisSyncType: NomisSyncType,
  val offenceEndDate: LocalDate? = null,
  val createdDate: LocalDateTime = LocalDateTime.now(),
)
