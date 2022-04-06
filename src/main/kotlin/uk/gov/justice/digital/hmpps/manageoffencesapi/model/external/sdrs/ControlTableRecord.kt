package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class ControlTableRecord(
  val dataSet: String,
  val lastUpdate: LocalDateTime
)
