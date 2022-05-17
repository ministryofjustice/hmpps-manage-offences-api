package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import java.time.LocalDate

data class PrisonApiOffence(
  val code: String,
  private val description: String,
  private val statuteCode: PrisonApiStatute,
  private val hoCode: PrisonApiHoCode? = null,
  private val severityRanking: String? = null,
  private val activeFlag: String? = null,
  private val listSequence: Int? = null,
  private val expiryDate: LocalDate? = null,
)
