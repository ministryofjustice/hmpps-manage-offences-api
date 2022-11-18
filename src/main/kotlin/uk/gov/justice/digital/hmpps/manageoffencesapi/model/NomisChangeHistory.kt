package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisChangeType
import java.time.LocalDateTime

@Schema(description = "This shows a change to NOMIS")
data class NomisChangeHistory(
  val id: Long,
  @Schema(description = "This is set depending on the nomisChangeType - could be the code of the offence, statute or Home Office Stats")
  val code: String,
  @Schema(description = "This description of the nomisChangeType")
  val description: String,
  @Schema(description = "Could be INSERT or UPDATE")
  val changeType: ChangeType,
  @Schema(description = "Could be OFFENCE or STATUTE")
  val nomisChangeType: NomisChangeType,
  @Schema(description = "The date this change was made in NOMIS")
  val sentToNomisDate: LocalDateTime,
)
