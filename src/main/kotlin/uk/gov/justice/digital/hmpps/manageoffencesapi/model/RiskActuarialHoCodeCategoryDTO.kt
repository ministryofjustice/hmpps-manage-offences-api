package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.RiskActuarialHoCodeErrorCode

data class RiskActuarialHoCodeCategoryDTO(
  val categoryName: String,
  val errorCode: RiskActuarialHoCodeErrorCode?,
)
