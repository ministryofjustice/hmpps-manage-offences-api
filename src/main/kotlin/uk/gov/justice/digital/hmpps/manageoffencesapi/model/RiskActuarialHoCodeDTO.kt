package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ActuarialCategory

data class RiskActuarialHoCodeDTO(
  val parentGroupDescription: String,
  val categoryDescription: String,
  val subCategoryDescription: String,
  val actuarialCategory: ActuarialCategory,
  val flags: Map<String, Boolean?>,
)
