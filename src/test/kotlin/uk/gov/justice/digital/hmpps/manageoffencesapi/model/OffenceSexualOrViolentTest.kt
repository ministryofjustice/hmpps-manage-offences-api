package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OffenceSexualOrViolentTest {

  @ParameterizedTest
  @CsvSource(
//   SEX     DV,    NS      VIOL   result
    "true,  false, false,  false, SEXUAL",
    "true,  true,  false, false,  SEXUAL",
    "false, true,  false, false,  DOMESTIC_ABUSE",
    "false, false, false, false,  NONE",
    "false, false,  false, true,  VIOLENT",
    "false, false,  true, false,  NATIONAL_SECURITY",
  )
  fun `Test all combinations of schedules and codes return the correct result`(
    sexual: Boolean,
    domesticViolence: Boolean,
    nationalSecurity: Boolean,
    violent: Boolean,
    offenceSexualOrViolentIndicator: OffenceSexualOrViolentIndicator,
  ) {
    assertThat(
      OffenceSexualOrViolent.getSexualOrViolentIndicator(
        sexual = sexual,
        nationalSecurity = nationalSecurity,
        domesticViolence = domesticViolence,
        violent = violent,
      ),
    ).isEqualTo(offenceSexualOrViolentIndicator)
  }
}
