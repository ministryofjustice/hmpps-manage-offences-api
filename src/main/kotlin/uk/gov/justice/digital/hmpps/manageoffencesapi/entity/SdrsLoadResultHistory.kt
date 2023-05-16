package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import java.time.LocalDateTime

@Entity
@Table
data class SdrsLoadResultHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  @Enumerated(EnumType.STRING)
  val cache: SdrsCache,
  @Enumerated(EnumType.STRING)
  val status: LoadStatus? = null,
  @Enumerated(EnumType.STRING)
  val loadType: LoadType? = null,
  val loadDate: LocalDateTime? = null,
  val nomisSyncRequired: Boolean? = false,
)
