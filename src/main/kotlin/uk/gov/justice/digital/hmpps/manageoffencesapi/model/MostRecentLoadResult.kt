package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadStatus
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.LoadType
import java.time.LocalDateTime

@Schema(description = "Details of the load by alpha char (A to Z)")
data class MostRecentLoadResult(
  @Schema(description = "Single alphabetic character between A and Z - indicates the part of the SDRS load this status relates to")
  val alphaChar: String,
  @Schema(description = "Load Status: SUCCESS or FAIL")
  val status: LoadStatus? = null,
  @Schema(description = "Load Type: FULL_LOAD or UPDATE")
  val type: LoadType? = null,
  @Schema(description = "The date and time of the load")
  val loadDate: LocalDateTime? = null,
  @Schema(description = "The date and time of the most recent successful load; if the load was successful this is the same as the loadDate")
  val lastSuccessfulLoadDate: LocalDateTime? = null,
)
