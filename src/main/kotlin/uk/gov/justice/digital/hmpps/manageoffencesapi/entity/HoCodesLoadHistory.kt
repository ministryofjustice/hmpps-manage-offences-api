package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class HoCodesLoadHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  val loadedFile: String,
  val loadDate: LocalDateTime = LocalDateTime.now(),
)
