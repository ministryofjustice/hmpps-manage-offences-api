package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.DOMESTIC_ABUSE
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.DOMESTIC_ABUSE_T3
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.MURDER_T3
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.NATIONAL_SECURITY
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.NONE
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.SEXUAL
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusionIndicator.SEXUAL_T3
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
      domesticViolenceT3: Boolean = false,
      sexualT3: Boolean = false,
      murderT3: Boolean = false,
    ): OffenceSdsExclusionIndicator = when {
      sexualT3 -> SEXUAL_T3
      sexual -> SEXUAL
      domesticViolenceT3 -> DOMESTIC_ABUSE_T3
      domesticViolence -> DOMESTIC_ABUSE
      nationalSecurity -> NATIONAL_SECURITY
      terrorism -> TERRORISM
      violent -> VIOLENT
      murderT3 -> MURDER_T3
      else -> NONE
    }
  }
}

@Schema(description = "Categories for the offence")
enum class OffenceSdsExclusionIndicator {
  SEXUAL,
  SEXUAL_T3,
  DOMESTIC_ABUSE,
  DOMESTIC_ABUSE_T3,
  VIOLENT,
  NONE,
  NATIONAL_SECURITY,
  TERRORISM,
  MURDER_T3,
}
