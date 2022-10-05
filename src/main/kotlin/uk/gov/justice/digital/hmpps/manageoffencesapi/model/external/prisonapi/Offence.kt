package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi

import java.time.LocalDate

data class Offence(
  val code: String,
  val description: String,
  val statuteCode: Statute,
  val hoCode: HoCode? = null,
  val severityRanking: String? = null,
  val activeFlag: String,
  val listSequence: Int? = null,
  val expiryDate: LocalDate? = null,
)
