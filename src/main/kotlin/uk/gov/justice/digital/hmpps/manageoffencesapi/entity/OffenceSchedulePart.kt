package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode

@Entity
@Table
data class OffenceSchedulePart(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val schedulePart: SchedulePart,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val offence: Offence,
)
