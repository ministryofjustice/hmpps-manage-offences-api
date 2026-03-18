package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import tools.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import tools.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class GetControlTableRequest(
  val changedDateTime: LocalDateTime,
)
