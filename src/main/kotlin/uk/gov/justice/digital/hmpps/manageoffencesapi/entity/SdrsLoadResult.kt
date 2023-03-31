package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class SdrsLoadResult(
  @Id
  @Enumerated(EnumType.STRING)
  val cache: SdrsCache,
  @Enumerated(EnumType.STRING)
  val status: LoadStatus? = null,
  @Enumerated(EnumType.STRING)
  val loadType: LoadType? = null,
  val loadDate: LocalDateTime? = null,
  val lastSuccessfulLoadDate: LocalDateTime? = null,
  val nomisSyncRequired: Boolean = false,
)
