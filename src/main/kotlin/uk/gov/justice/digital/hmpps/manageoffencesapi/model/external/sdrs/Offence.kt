package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.CustodialIndicator
import java.time.LocalDate
import java.time.LocalDateTime

@JsonNaming(UpperCamelCaseStrategy::class)
data class Offence(
  val offenceRevisionId: Int,
  @JsonProperty("code")
  val code: String,
  val description: String? = null,
  val cjsTitle: String? = null,
  @JsonProperty("MOJStatsCode")
  val mojStatsCode: String? = null,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate? = null,
  val changedDate: LocalDateTime,
  val offenceActsAndSections: String? = null,
  val offenceType: String? = null,
  @JsonProperty("CustodialIndicator")
  val custodialIndicator: CustodialIndicator?=null,
) {
  val isEndDateInFuture: Boolean
    get() = offenceEndDate?.isAfter(LocalDate.now()) ?: false
  val category: Int?
    get() = mojStatsCode?.substringBefore('/')?.toIntOrNull()
  val subCategory: Int?
    get() = mojStatsCode?.substringAfter('/')?.toIntOrNull()
}
