package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table
data class OffenceScheduleMapping(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val scheduleParagraph: ScheduleParagraph,
  val lineReference: String?,
  val legislationText: String?,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val offence: Offence,
)
