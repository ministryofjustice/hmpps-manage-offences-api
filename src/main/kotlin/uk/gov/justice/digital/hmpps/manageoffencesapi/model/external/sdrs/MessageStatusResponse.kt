package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

data class MessageStatusResponse(
  val status: String,
  val code: String? = null,
  val reason: String? = null,
)
