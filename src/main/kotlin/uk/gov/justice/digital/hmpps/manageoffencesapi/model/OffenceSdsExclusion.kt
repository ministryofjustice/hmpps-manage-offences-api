package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.DOMESTIC_ABUSE
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.NATIONAL_SECURITY
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.NONE
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.SEXUAL
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.TERRORISM
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.VIOLENT

@Schema(description = "Categorises the offence based on the exclusion list it appears in")
data class OffenceSdsExclusion(
  val offenceCode: String,
  val schedulePart: OffenceSdsExclusionIndicator,
) {
  companion object {
    fun getSdsExclusionIndicator(
      sexual: Boolean,
      domesticViolence: Boolean,
      nationalSecurity: Boolean,
      terrorism: Boolean,
      violent: Boolean,
    ): OffenceSdsExclusionIndicator = when {
      sexual -> SEXUAL
      domesticViolence -> DOMESTIC_ABUSE
      nationalSecurity -> NATIONAL_SECURITY
      terrorism -> TERRORISM
      violent -> VIOLENT
      else -> NONE
    }
  }
}

@Schema(description = "Categories for the offence")
enum class OffenceSdsExclusionIndicator {
  SEXUAL,
  DOMESTIC_ABUSE,
  VIOLENT,
  NONE,
  NATIONAL_SECURITY,
  TERRORISM,
}
