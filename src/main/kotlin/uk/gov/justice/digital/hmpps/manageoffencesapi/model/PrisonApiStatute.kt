package uk.gov.justice.digital.hmpps.manageoffencesapi.model

data class PrisonApiStatute(
  val code: String,
  val description: String,
  val legislatingBodyCode: String,
  val activeFlag: String
)
