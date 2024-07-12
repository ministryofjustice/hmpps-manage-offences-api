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
      sexual: Boolean,
      domesticViolence: Boolean,
      nationalSecurity: Boolean,
      violent: Boolean,
    ): OffenceSexualOrViolentIndicator = when {
      sexual -> SEXUAL
      domesticViolence -> DOMESTIC_ABUSE
      nationalSecurity -> NATIONAL_SECURITY
      violent -> VIOLENT
      else -> NONE
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
