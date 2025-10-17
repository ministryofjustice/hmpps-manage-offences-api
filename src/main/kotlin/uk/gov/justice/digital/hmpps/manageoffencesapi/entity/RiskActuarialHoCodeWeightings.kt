package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.RiskActuarialHoCodeErrorCode

@Entity
@Table
data class RiskActuarialHoCodeWeightings(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,
  val weightingName: String,
  val weightingValue: Double? = null,
  val weightingDesc: String,
  @Enumerated(EnumType.STRING)
  val errorCode: RiskActuarialHoCodeErrorCode? = null,
  @ManyToOne
  @JoinColumn(
    name = "risk_actuarial_ho_code_id",
    nullable = false,
    foreignKey = ForeignKey(name = "risk_actuarial_ho_code_id"),
  )
  val riskActuarialHoCode: RiskActuarialHoCode?,
)
