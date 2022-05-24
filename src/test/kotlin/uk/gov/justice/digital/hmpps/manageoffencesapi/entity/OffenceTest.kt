package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OffenceTest {
  @Test
  fun `Check statute and homeOfficeStatsCode with minimum data`() {
    val of = Offence(code = "A1234")
    assertThat(of.statuteCode).isEqualTo("A123")
    assertThat(of.homeOfficeStatsCode).isNull()
  }

  @Test
  fun `Check homeOfficeStatsCode with no subcategory`() {
    val of = Offence(code = "A1234", category = 12)
    assertThat(of.homeOfficeStatsCode).isEqualTo("012/")
  }

  @Test
  fun `Check homeOfficeStatsCode with no category`() {
    val of = Offence(code = "A1234", subCategory = 5)
    assertThat(of.homeOfficeStatsCode).isEqualTo("/05")
  }

  @Test
  fun `Check homeOfficeStatsCode with both category and sub-category`() {
    val of = Offence(code = "A1234", category = 12, subCategory = 5)
    assertThat(of.homeOfficeStatsCode).isEqualTo("012/05")
  }
}
