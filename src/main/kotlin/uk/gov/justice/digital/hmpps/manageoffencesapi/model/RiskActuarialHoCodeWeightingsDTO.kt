package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.RiskActuarialHoCodeErrorCode

data class RiskActuarialHoCodeWeightingsDTO(
  val name: String,
  val value: Double?,
  val description: String,
  val errorCode: RiskActuarialHoCodeErrorCode?,
)
