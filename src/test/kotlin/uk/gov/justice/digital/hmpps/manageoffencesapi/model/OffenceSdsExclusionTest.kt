package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OffenceSdsExclusionTest {

  @ParameterizedTest
  @CsvSource(
//   SEX    DV     NS     VIOL   TERROR result
    "true,  false, false, false, false, SEXUAL",
    "true,  true,  false, false, false, SEXUAL",
    "false, true,  false, false, false, DOMESTIC_ABUSE",
    "false, false, false, false, false, NONE",
    "false, false, false, true,  false, VIOLENT",
    "false, false, true,  false, false, NATIONAL_SECURITY",
    "false, false, false, false, true,  TERRORISM",
  )
  fun `Test all combinations of schedules and codes return the correct result`(
    sexual: Boolean,
    domesticViolence: Boolean,
    nationalSecurity: Boolean,
    violent: Boolean,
    terrorism: Boolean,
    offenceSdsExclusionIndicator: OffenceSdsExclusionIndicator,
  ) {
    assertThat(
      OffenceSdsExclusion.getSdsExclusionIndicator(
        sexual = sexual,
        nationalSecurity = nationalSecurity,
        domesticViolence = domesticViolence,
        violent = violent,
        terrorism = terrorism,
      ),
    ).isEqualTo(offenceSdsExclusionIndicator)
  }
}
