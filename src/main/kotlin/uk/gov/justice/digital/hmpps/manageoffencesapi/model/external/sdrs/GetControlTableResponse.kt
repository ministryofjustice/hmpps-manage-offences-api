package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import tools.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(UpperCamelCaseStrategy::class)
data class GetControlTableResponse(
  val referenceDataSet: List<ControlTableRecord>,
)
