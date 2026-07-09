package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ActuarialCategory
import java.time.LocalDateTime

@Entity
@Table
data class RiskActuarialHoCodeCategory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,
  @Enumerated(EnumType.STRING)
  val categoryName: ActuarialCategory,
  val createdDate: LocalDateTime = LocalDateTime.now(),
  @OneToMany(mappedBy = "riskActuarialHoCodeCategory", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.EAGER)
  val riskActuarialHoCode: MutableList<RiskActuarialHoCode> = mutableListOf(),
)
