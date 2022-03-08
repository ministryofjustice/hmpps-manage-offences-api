package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external

import com.fasterxml.jackson.annotation.JsonProperty

data class SDRSResponse(
  @JsonProperty("MessageBody")
  val messageBody: MessageBody
)
