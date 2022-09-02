package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Offence details")
data class Offence(
  @Schema(description = "Unique ID of the offence")
  val id: Long,
  @Schema(description = "The offence code")
  val code: String? = null,
  @Schema(description = "The offence description")
  val description: String? = null,
  @Schema(description = "The CJS Title (usually the same as description)")
  val cjsTitle: String? = null,
  @Schema(description = "The revision number of the offence")
  val revisionId: Int? = null,
  @Schema(description = "The offence start date")
  val startDate: LocalDate? = null,
  @Schema(description = "The offence end date")
  val endDate: LocalDate? = null,
  @Schema(description = "The offence's home office stats code")
  val homeOfficeStatsCode: String? = null,
  @Schema(description = "The date this offence was last changed in SDRS")
  val changedDate: LocalDateTime? = null,
  @Schema(description = "The date this offence was loaded into manage-offences from SDRS")
  val loadDate: LocalDateTime? = null,
  @Schema(description = "The schedules linked to this offence")
  val schedules: List<ScheduleDetails>? = emptyList(),
  @Schema(description = "If true then this is a inchoate offence; i.e. a child of another offence")
  val isChild: Boolean = false,
  @Schema(description = "The parent offence id of an inchoate offence")
  val parentOffenceId: Long? = null,
  @Schema(description = "A list of child offence ID's; i.e. inchoate offences linked to this offence")
  val childOffenceIds: List<Long>? = emptyList(),
)
