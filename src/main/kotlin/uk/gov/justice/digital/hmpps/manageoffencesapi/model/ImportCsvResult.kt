package uk.gov.justice.digital.hmpps.manageoffencesapi.model

data class ImportCsvResult(
  val success: Boolean,
  val message: String,
  val errors: List<String>,
)
