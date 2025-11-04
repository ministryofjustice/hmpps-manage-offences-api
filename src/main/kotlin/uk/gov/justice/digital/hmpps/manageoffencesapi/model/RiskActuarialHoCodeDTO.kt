package uk.gov.justice.digital.hmpps.manageoffencesapi.model

data class RiskActuarialHoCodeDTO(
  val category: Int,
  val subCategory: Int,
  val flags: List<RiskActuarialHoCodeFlagsDTO>,
  val weightings: List<RiskActuarialHoCodeWeightingsDTO>,
)
