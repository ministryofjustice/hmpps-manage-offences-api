package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.DOMESTIC_ABUSE
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.NATIONAL_SECURITY
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
      domesticViolence: Boolean,
      useOffenceCodesForSexual: Boolean,
      isNationalSecurity: Boolean,
      isSexOffenceLegislation: Boolean,
      offenceCode: String,
    ): OffenceSexualOrViolentIndicator {
      return when {
        useOffenceCodesForSexual && (inSchedule15Part2 || isOffenceCodePrefixedWithSX03orSX56(offenceCode)) -> SEXUAL
        !useOffenceCodesForSexual && (inSchedule15Part2 || inSchedule3) -> SEXUAL
        isSexOffenceLegislation -> SEXUAL
        domesticViolence -> DOMESTIC_ABUSE
        isNationalSecurity -> NATIONAL_SECURITY
        inSchedule15Part1 -> VIOLENT
        else -> NONE
      }
    }

    private fun isOffenceCodePrefixedWithSX03orSX56(offenceCode: String): Boolean {
      return offenceCode.startsWith("SX03") || offenceCode.startsWith("SX56")
    }
  }
}

@Schema(description = "Categories for the offence")
enum class OffenceSexualOrViolentIndicator {
  SEXUAL,
  DOMESTIC_ABUSE,
  VIOLENT,
  NONE,
  NATIONAL_SECURITY,
}
