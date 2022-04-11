package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class SdrsLoadResultHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  val alphaChar: String,
  val status: LoadStatus? = null,
  val loadType: LoadType? = null,
  val loadDate: LocalDateTime? = null,
)
