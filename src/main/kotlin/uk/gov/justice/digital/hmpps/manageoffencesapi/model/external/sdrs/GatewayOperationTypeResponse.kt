package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(UpperCamelCaseStrategy::class)
data class GatewayOperationTypeResponse(
  val getOffenceResponse: GetOffenceResponse? = null,
  val getApplicationsResponse: GetApplicationsResponse? = null,
  @JsonProperty("MOJOffenceResponse")
  val mojOffenceResponse: MOJOffenceResponse? = null,
  val getControlTableResponse: GetControlTableResponse? = null,

)
