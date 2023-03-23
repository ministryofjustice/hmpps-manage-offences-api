package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty

data class MOJOffenceResponse(
  @JsonProperty("MOJOffence")
  val offences: List<Offence>,
)
