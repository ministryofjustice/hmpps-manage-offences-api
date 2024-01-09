package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty

data class GetApplicationsResponse(
  @JsonProperty("Application")
  val offences: List<Offence> = emptyList(),
)
