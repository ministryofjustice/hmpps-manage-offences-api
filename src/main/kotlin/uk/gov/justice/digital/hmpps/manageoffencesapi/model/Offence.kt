package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Offence details")
data class Offence(
  @Schema(description = "Unique ID of the offence")
  val id: Long,
  @Schema(description = "The offence code")
  val code: String,
  @Schema(description = "The offence description (taken from SDRS CJSTitle field)")
  val description: String? = null,
  @Schema(description = "The offence type (e.g CI)")
  val offenceType: String? = null,
  @Schema(description = "The revision number of the offence")
  val revisionId: Int,
  @Schema(description = "The offence start date")
  val startDate: LocalDate,
  @Schema(description = "The offence end date")
  val endDate: LocalDate? = null,
  @Schema(description = "The offence's home office stats code")
  val homeOfficeStatsCode: String? = null,
  @Schema(description = "The date this offence was last changed in SDRS")
  val changedDate: LocalDateTime,
  @Schema(description = "The date this offence was loaded into manage-offences from SDRS")
  val loadDate: LocalDateTime? = null,
  @Schema(description = "The schedules linked to this offence")
  val schedules: List<LinkedScheduleDetails>? = null,
  @Schema(description = "If true then this is a inchoate offence; i.e. a child of another offence")
  val isChild: Boolean = false,
  @Schema(description = "The parent offence id of an inchoate offence")
  val parentOffenceId: Long? = null,
  @Schema(description = "A list of child offence ID's; i.e. inchoate offences linked to this offence")
  val childOffenceIds: List<Long>? = null,
  @Schema(description = "The legislation associated to this offence (from actsAndSections in the SDRS response)")
  val legislation: String? = null,
  // The maxPeriodIsLife and maxPeriodOfIndictmentYears fields are populated via the schedule data supplied by the HMCTS NSD team
  @Schema(description = "Set to true if max period is life")
  val maxPeriodIsLife: Boolean? = null,
  @Schema(description = "Set to the max period of indictment in years")
  val maxPeriodOfIndictmentYears: Int? = null,
)

data class OffenceToScheduleMapping(
  @Schema(description = "Unique ID of the offence")
  val id: Long,
  @Schema(description = "The offence code")
  val code: String,
  @Schema(description = "The offence description (taken from SDRS CJSTitle field)")
  val description: String? = null,
  @Schema(description = "The offence type (e.g CI)")
  val offenceType: String? = null,
  @Schema(description = "The revision number of the offence")
  val revisionId: Int,
  @Schema(description = "The offence start date")
  val startDate: LocalDate,
  @Schema(description = "The offence end date")
  val endDate: LocalDate? = null,
  @Schema(description = "The offence's home office stats code")
  val homeOfficeStatsCode: String? = null,
  @Schema(description = "The date this offence was last changed in SDRS")
  val changedDate: LocalDateTime,
  @Schema(description = "The date this offence was loaded into manage-offences from SDRS")
  val loadDate: LocalDateTime? = null,
  @Schema(description = "The schedules linked to this offence")
  val schedules: List<LinkedScheduleDetails>? = emptyList(),
  @Schema(description = "If true then this is a inchoate offence; i.e. a child of another offence")
  val isChild: Boolean = false,
  @Schema(description = "The parent offence id of an inchoate offence")
  val parentOffenceId: Long? = null,
  @Schema(description = "A list of child offence ID's; i.e. inchoate offences linked to this offence")
  val childOffences: List<BasicOffence>? = null,
  @Schema(description = "The legislation associated to this offence (from actsAndSections in the SDRS response)")
  val legislation: String? = null,
  // The maxPeriodIsLife and maxPeriodOfIndictmentYears fields are populated via the schedule data supplied by the HMCTS NSD team
  @Schema(description = "Set to true if max period is life")
  val maxPeriodIsLife: Boolean? = null,
  @Schema(description = "Set to the max period of indictment in years")
  val maxPeriodOfIndictmentYears: Int? = null,
  @Schema(description = "The line reference for the associated schedule's legislation")
  val lineReference: String? = null,
  @Schema(description = "The legislation text for the associated schedule")
  val legislationText: String? = null,
  @Schema(description = "Schedule paragraph title that this offence is mapped to")
  val paragraphTitle: String? = null,
  @Schema(description = "Schedule paragraph number that this offence is mapped to")
  val paragraphNumber: Int? = null,
)

data class LinkOffence(
  @Schema(description = "Unique ID of the offence")
  val offenceId: Long,
  @Schema(description = "The offence code")
  val schedulePartId: Long,
  @Schema(description = "The line reference for the associated schedule's legislation")
  val lineReference: String? = null,
  @Schema(description = "The legislation text for the associated schedule")
  val legislationText: String? = null,
  @Schema(description = "Schedule paragraph title that this offence is mapped to")
  val paragraphTitle: String? = null,
  @Schema(description = "Schedule paragraph number that this offence is mapped to")
  val paragraphNumber: Int? = null,
)

data class BasicOffence(
  @Schema(description = "Unique ID of the offence")
  val id: Long,
  @Schema(description = "The offence code")
  val code: String,
  @Schema(description = "The offence description (taken from SDRS CJSTitle field)")
  val description: String? = null,
  @Schema(description = "The offence start date")
  val startDate: LocalDate,
  @Schema(description = "The offence end date")
  val endDate: LocalDate? = null,
)
