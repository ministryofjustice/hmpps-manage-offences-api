package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.NONE
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.SEXUAL
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.VIOLENT

@Schema(description = "Categorises the offence based on the schedule it appears in")
data class OffenceSexualOrViolent(
  val offenceCode: String,
  val schedulePart: OffenceSexualOrViolentIndicator,
) {
  companion object {
    fun getSexualOrViolentIndicator(
      inSchedule3: Boolean,
      inSchedule15Part1: Boolean,
      inSchedule15Part2: Boolean,
    ): OffenceSexualOrViolentIndicator {
      return when {
        inSchedule15Part2 || inSchedule3 -> SEXUAL
        inSchedule15Part1 -> VIOLENT
        else -> NONE
      }
    }
  }
}

@Schema(description = "Categories for the offence")
enum class OffenceSexualOrViolentIndicator {
  SEXUAL,
  VIOLENT,
  NONE,
}
