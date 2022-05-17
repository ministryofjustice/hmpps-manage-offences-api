package uk.gov.justice.digital.hmpps.manageoffencesapi.model

data class PrisonApiStatute(
  val code: String,
  val description: String? = null,
  val legislatingBodyCode: String? = null,
  val activeFlag: String? = null
)
