package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table
data class RiskActuarialHoCode(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,
  val category: Int,
  val subCategory: Int,
  val parentGroupDescription: String,
  val categoryDescription: String,
  val subCategoryDescription: String,
  val createdDate: LocalDateTime = LocalDateTime.now(),
  @OneToMany(mappedBy = "riskActuarialHoCode", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.EAGER)
  val riskActuarialHoCodeFlags: MutableList<RiskActuarialHoCodeFlags> = mutableListOf(),
  @ManyToOne
  @JoinColumn(
    name = "risk_actuarial_ho_code_category_id",
    nullable = false,
    foreignKey = ForeignKey(name = "risk_actuarial_ho_code_category_id"),
  )
  val riskActuarialHoCodeCategory: RiskActuarialHoCodeCategory?,
)
