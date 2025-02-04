package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode

@NamedEntityGraph(
  name = "OffenceScheduleMapping.detail",
  attributeNodes = [
    NamedAttributeNode("offence"),
    NamedAttributeNode("schedulePart"),
  ],
)
@Entity
@Table
data class OffenceScheduleMapping(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val schedulePart: SchedulePart,
  @ManyToOne
  @Fetch(FetchMode.JOIN)
  val offence: Offence,
  val lineReference: String? = null,
  val legislationText: String? = null,
  val paragraphNumber: String? = null,
  val paragraphTitle: String? = null,
)
