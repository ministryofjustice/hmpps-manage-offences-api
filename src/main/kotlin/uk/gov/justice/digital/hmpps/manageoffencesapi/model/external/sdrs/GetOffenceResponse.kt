package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty

data class GetOffenceResponse(
  @JsonProperty("Offence")
  val offences: List<Offence>
)
