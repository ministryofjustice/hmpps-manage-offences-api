package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache.OFFENCES_A
import java.time.LocalDate
import java.time.LocalDateTime

class OffenceTest {
  @Test
  fun `Check statute and homeOfficeStatsCode with minimum data`() {
    val of = BASE_OFFENCE.copy(sdrsCache = OFFENCES_A, code = "A1234")
    assertThat(of.statuteCode).isEqualTo("A123")
    assertThat(of.homeOfficeStatsCode).isNull()
  }

  @Test
  fun `Check homeOfficeStatsCode with no subcategory`() {
    val of = BASE_OFFENCE.copy(sdrsCache = OFFENCES_A, code = "A1234", category = 12)
    assertThat(of.homeOfficeStatsCode).isEqualTo("012/")
  }

  @Test
  fun `Check homeOfficeStatsCode with no category`() {
    val of = BASE_OFFENCE.copy(sdrsCache = OFFENCES_A, code = "A1234", subCategory = 5)
    assertThat(of.homeOfficeStatsCode).isEqualTo("/05")
  }

  @Test
  fun `Check homeOfficeStatsCode with both category and sub-category`() {
    val of = BASE_OFFENCE.copy(sdrsCache = OFFENCES_A, code = "A1234", category = 12, subCategory = 5)
    assertThat(of.homeOfficeStatsCode).isEqualTo("012/05")
  }

  companion object {
    private val BASE_OFFENCE = Offence(
      code = "AABB011",
      changedDate = LocalDateTime.now(),
      revisionId = 1,
      startDate = LocalDate.now(),
      sdrsCache = OFFENCES_A,
    )
  }
}
