package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisScheduleName
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisSyncType
import java.time.LocalDateTime

@Entity
@Table
data class OffenceToSyncWithNomis(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,
  val offenceCode: String,
  @Enumerated(EnumType.STRING)
  val nomisSyncType: NomisSyncType,
  @Enumerated(EnumType.STRING)
  val nomisScheduleName: NomisScheduleName? = null,
  val createdDate: LocalDateTime = LocalDateTime.now(),
)
