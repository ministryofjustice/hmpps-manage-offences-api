package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import tools.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class GetControlTableRequest(
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val changedDateTime: LocalDateTime,
)
