package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OffenceSexualOrViolentTest {

  @ParameterizedTest
  @CsvSource(
//   S3     S15P1  S15P2  toggle  code     result
    "true,  false, false, false,  AB1234,  SEXUAL",
    "false, false, true,  false,  AB1234,  SEXUAL",
    "false, true,  false, false,  AB1234,  VIOLENT",
    "false, false, false, false,  AB1234,  NONE",
//   Sexual takes priority over violent
    "true , true,  false, false,  AB1234,  SEXUAL",
    "false, true,  true,  false,  AB1234,  SEXUAL",
//   Tests for the toggle
    "false, false, false, true,   SX0334,  SEXUAL",
    "false, false, false, true,   SX5634B, SEXUAL",
    "false, false, false, true,   AB5634B, NONE",
    "false, false, true,  true,   AB5634B, SEXUAL",
    "false, true,  false, true,   AB5634B, VIOLENT",
//   Sexual takes priority over violent (with codes)
    "false, true,  false, true,   SX0345,  SEXUAL",
    "false, true,  false, true,   SX5678,  SEXUAL",
    "false, true,  true,  true,   CF2478,  SEXUAL",
  )
  fun `Test all combinations of schedules and codes return the correct result`(
    inSchedule3: Boolean,
    inSchedule15Part1: Boolean,
    inSchedule15Part2: Boolean,
    useOffenceCodesForSexual: Boolean,
    offenceCode: String,
    offenceSexualOrViolentIndicator: OffenceSexualOrViolentIndicator,
  ) {
    assertThat(
      OffenceSexualOrViolent.getSexualOrViolentIndicator(
        inSchedule3 = inSchedule3,
        inSchedule15Part1 = inSchedule15Part1,
        inSchedule15Part2 = inSchedule15Part2,
        useOffenceCodesForSexual = useOffenceCodesForSexual,
        offenceCode = offenceCode,
      ),
    ).isEqualTo(offenceSexualOrViolentIndicator)
  }
}
