package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class SdrsLoadResult(
  @Id
  val alphaChar: String,
  val status: LoadStatus? = null,
  val loadType: LoadType? = null,
  val loadDate: LocalDateTime? = null,
  val lastSuccessfulLoadDate: LocalDateTime? = null,
)
