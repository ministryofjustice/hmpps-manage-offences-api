package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi

import java.time.LocalDate

data class HoCode(
  val code: String,
  val description: String,
  val activeFlag: String,
  val expiryDate: LocalDate? = null,
)
