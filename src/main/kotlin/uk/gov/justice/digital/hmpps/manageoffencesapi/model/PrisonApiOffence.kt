package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import java.time.LocalDate

data class PrisonApiOffence(
  val code: String,
  val description: String,
  val statuteCode: PrisonApiStatute,
  val hoCode: PrisonApiHoCode? = null,
  val severityRanking: String? = null,
  val activeFlag: String,
  val listSequence: Int? = null,
  val expiryDate: LocalDate? = null,
)
