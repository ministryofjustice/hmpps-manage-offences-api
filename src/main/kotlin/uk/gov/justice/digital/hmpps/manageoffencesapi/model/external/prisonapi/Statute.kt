package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi

data class Statute(
  val code: String,
  val description: String,
  val legislatingBodyCode: String,
  val activeFlag: String
)
