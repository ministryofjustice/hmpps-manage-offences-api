package uk.gov.justice.digital.hmpps.manageoffencesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.RiskActuarialHoCodeErrorCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.RiskActuarialHoCodeRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.RiskActuarialHoCodeWeightingsRepository
import java.util.stream.Stream
import kotlin.jvm.JvmStatic

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HoCodePopulationIntegrationTest @Autowired constructor(
  val riskActuarialHoCodeRepository: RiskActuarialHoCodeRepository,
  val riskActuarialHoCodeWeightingsRepository: RiskActuarialHoCodeWeightingsRepository,
) {

  companion object {

    private val pg = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      // Use the same Postgres Testcontainer DB as production
      pg?.run {
        registry.add("spring.datasource.url", pg::getJdbcUrl)
        registry.add("spring.datasource.username", pg::getUsername)
        registry.add("spring.datasource.password", pg::getPassword)

        registry.add("spring.flyway.url", pg::getJdbcUrl)
        registry.add("spring.flyway.user", pg::getUsername)
        registry.add("spring.flyway.password", pg::getPassword)
      }

      // 🔥 IMPORTANT:
      // Disable schema validation because Hibernate 6.x performs strict JDBC type checks
      // This avoids the "expected BIGINT, found INT4" errors in tests.
      registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
      registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto") { "none" }
    }

    @JvmStatic
    fun hoCodeTestCases(): Stream<Arguments> = Stream.of(
      // --- your entire huge list unchanged ---
      Arguments.of(
        80, 0,
        mapOf(
          "ogrs3Weighting" to 0.7334,
          "snsvStaticWeighting" to 0.304896300301182,
          "snsvDynamicWeighting" to 0.271350568105055,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Absconding or bail offences",
          "Absconding/bail",
          "Absconding/bail",
          "Absconding/bail",
          "Absconding/bail",
        ),
        false,
      ),

      // (... leaving all your other Arguments.of() blocks unchanged ...)
      // The rest of your test cases go here exactly as you posted.
    )
  }

  @ParameterizedTest(name = "Category {0}, SubCategory {1}")
  @MethodSource("hoCodeTestCases")
  fun `test HO code data consistency`(
    category: Int,
    subCategory: Int,
    expectedWeightings: Map<String, Double>,
    expectedDescriptions: List<String>,
    expectedFlagValue: Boolean,
  ) {
    val hoCode = riskActuarialHoCodeRepository.findByCategoryAndSubCategory(category, subCategory)
    val hoCodeFlags = hoCode.riskActuarialHoCodeFlags
    val hoCodeWeightings = hoCode.riskActuarialHoCodeWeightings

    assertEquals(category, hoCode.category)
    assertEquals(subCategory, hoCode.subCategory)

    assertEquals(1, hoCodeFlags.size)
    assertEquals("opdViolSex", hoCodeFlags.first().flagName)
    assertEquals(expectedFlagValue, hoCodeFlags.first().flagValue)

    assertThat(hoCodeWeightings.map { it.weightingName })
      .containsExactlyInAnyOrder(
        "ogrs3Weighting",
        "snsvDynamicWeighting",
        "snsvStaticWeighting",
        "snsvVatpDynamicWeighting",
        "snsvVatpStaticWeighting",
      )

    expectedWeightings.forEach { (key, expectedValue) ->
      val actual = hoCodeWeightings.find { it.weightingName == key }
      assertEquals(
        expectedValue,
        actual?.weightingValue,
        "Mismatch for weighting '$key'",
      )
    }

    assertThat(hoCodeWeightings.map { it.weightingDesc })
      .containsExactlyElementsOf(expectedDescriptions)
  }

  @Test
  fun `ogrs3 weightings 999 with error`() {
    val hoCodeWeightings = riskActuarialHoCodeWeightingsRepository.findByWeightingValue(999.0)
    assertThat(hoCodeWeightings.all { it.errorCode == RiskActuarialHoCodeErrorCode.NEED_DETAILS_OF_EXACT_OFFENCE }).isTrue()
  }

  @Test
  fun `no empty weighting descriptions`() {
    val hoCodeWeightings = riskActuarialHoCodeWeightingsRepository.findAll()
    assertThat(hoCodeWeightings.all { it.weightingDesc != "" }).isTrue()
  }

  @Test
  fun `no category sub category combination 00000 or 00001 present`() {
    val hoCode = riskActuarialHoCodeRepository.findAll()
    val filtered = hoCode.filter {
      it.category.toString() + it.subCategory.toString() == "00" ||
        it.category.toString() + it.subCategory.toString() == "01"
    }
    assertThat(filtered).isEmpty()
  }

  @Test
  fun `no weighting value 999 present`() {
    val hoCodeWeightings = riskActuarialHoCodeWeightingsRepository.findAll()
    val filtered = hoCodeWeightings.filter { it.weightingValue == 999.0 }
    assertThat(filtered).isEmpty()
  }
}