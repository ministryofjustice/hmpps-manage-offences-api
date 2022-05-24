package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OffenceTest {
  @Test
  fun `Check category and subcategory when no mojStatsCode exists`() {
    val of = Offence(code = "A1234", offenceRevisionId = 1)
    assertThat(of.category).isNull()
    assertThat(of.subCategory).isNull()
  }

  @Test
  fun `Check category and subcategory when no subcategory`() {
    val of = Offence(code = "A1234", offenceRevisionId = 1, mojStatsCode = "012/")
    assertThat(of.category).isEqualTo(12)
    assertThat(of.subCategory).isNull()
  }

  @Test
  fun `Check category and subcategory when no category`() {
    val of = Offence(code = "A1234", offenceRevisionId = 1, mojStatsCode = "/02")
    assertThat(of.category).isNull()
    assertThat(of.subCategory).isEqualTo(2)
  }

  @Test
  fun `Check category and subcategory when both exist`() {
    val of = Offence(code = "A1234", offenceRevisionId = 1, mojStatsCode = "012/02")
    assertThat(of.category).isEqualTo(12)
    assertThat(of.subCategory).isEqualTo(2)
  }
}
