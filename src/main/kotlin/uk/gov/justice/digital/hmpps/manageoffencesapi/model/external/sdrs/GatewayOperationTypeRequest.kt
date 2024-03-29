package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(UpperCamelCaseStrategy::class)
data class GatewayOperationTypeRequest(
  val getControlTableRequest: GetControlTableRequest? = null,
  val getOffenceRequest: GetOffenceRequest? = null,
  val getApplicationRequest: GetApplicationRequest? = null,
  val getMojOffenceRequest: GetMojOffenceRequest? = null,
)
