package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OffenceTest {
  @Test
  fun `Check category and subcategory when no mojStatsCode exists`() {
    val of = BASE_OFFENCE.copy(code = "A1234", offenceRevisionId = 1)
    assertThat(of.category).isNull()
    assertThat(of.subCategory).isNull()
  }

  @Test
  fun `Check category and subcategory when no subcategory`() {
    val of = BASE_OFFENCE.copy(code = "A1234", offenceRevisionId = 1, mojStatsCode = "012/")
    assertThat(of.category).isEqualTo(12)
    assertThat(of.subCategory).isNull()
  }

  @Test
  fun `Check category and subcategory when no category`() {
    val of = BASE_OFFENCE.copy(code = "A1234", offenceRevisionId = 1, mojStatsCode = "/02")
    assertThat(of.category).isNull()
    assertThat(of.subCategory).isEqualTo(2)
  }

  @Test
  fun `Check category and subcategory when both exist`() {
    val of = BASE_OFFENCE.copy(code = "A1234", offenceRevisionId = 1, mojStatsCode = "012/02")
    assertThat(of.category).isEqualTo(12)
    assertThat(of.subCategory).isEqualTo(2)
  }

  companion object {
    val BASE_OFFENCE = Offence(
      code = "AABB011",
      changedDate = LocalDateTime.now(),
      offenceRevisionId = 1,
      offenceStartDate = LocalDate.now(),
    )
  }
}
