package uk.gov.justice.digital.hmpps.manageoffencesapi.enum

import com.fasterxml.jackson.annotation.JsonProperty

enum class CustodialIndicator {
  @JsonProperty("Y")
  YES,

  @JsonProperty("N")
  NO,

  @JsonProperty("E")
  EITHER,
}
