package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.NONE
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.SEXUAL
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSexualOrViolentIndicator.VIOLENT

class OffenceSexualOrViolentTest {

  @Test
  fun `Test all combinations of schedules return the correct result`() {
    assertEquals(
      SEXUAL,
      OffenceSexualOrViolent.getSexualOrViolentIndicator(
        inSchedule3 = true,
        inSchedule15Part1 = false,
        inSchedule15Part2 = false,
      ),
    )
    assertEquals(
      SEXUAL,
      OffenceSexualOrViolent.getSexualOrViolentIndicator(
        inSchedule3 = false,
        inSchedule15Part1 = false,
        inSchedule15Part2 = true,
      ),
    )
    assertEquals(
      VIOLENT,
      OffenceSexualOrViolent.getSexualOrViolentIndicator(
        inSchedule3 = false,
        inSchedule15Part1 = true,
        inSchedule15Part2 = false,
      ),
    )
    assertEquals(
      NONE,
      OffenceSexualOrViolent.getSexualOrViolentIndicator(
        inSchedule3 = false,
        inSchedule15Part1 = false,
        inSchedule15Part2 = false,
      ),
    )

    // Sexual takes priority over Violent
    assertEquals(
      SEXUAL,
      OffenceSexualOrViolent.getSexualOrViolentIndicator(
        inSchedule3 = true,
        inSchedule15Part1 = true,
        inSchedule15Part2 = false,
      ),
    )
    assertEquals(
      SEXUAL,
      OffenceSexualOrViolent.getSexualOrViolentIndicator(
        inSchedule3 = false,
        inSchedule15Part1 = true,
        inSchedule15Part2 = true,
      ),
    )
  }
}
