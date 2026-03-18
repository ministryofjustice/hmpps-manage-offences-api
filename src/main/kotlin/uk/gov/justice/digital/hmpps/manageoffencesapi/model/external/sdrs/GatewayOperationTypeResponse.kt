package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(UpperCamelCaseStrategy::class)
data class GatewayOperationTypeResponse(
  val getOffenceResponse: GetOffenceResponse? = null,
  val getApplicationsResponse: GetApplicationsResponse? = null,
  @JsonProperty("MOJOffenceResponse")
  val mojOffenceResponse: MOJOffenceResponse? = null,
  val getControlTableResponse: GetControlTableResponse? = null,

)
