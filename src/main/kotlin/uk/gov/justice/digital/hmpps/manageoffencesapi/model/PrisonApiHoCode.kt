package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import java.time.LocalDate

data class PrisonApiHoCode(
  val code: String,
  val description: String,
  val activeFlag: String,
  val expiryDate: LocalDate? = null,
)
