package uk.gov.justice.digital.hmpps.manageoffencesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.RiskActuarialHoCodeErrorCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.RiskActuarialHoCodeRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.RiskActuarialHoCodeWeightingsRepository
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HoCodePopulationIntegrationTest @Autowired constructor(
  val riskActuarialHoCodeRepository: RiskActuarialHoCodeRepository,
  val riskActuarialHoCodeWeightingsRepository: RiskActuarialHoCodeWeightingsRepository,
) {

  companion object {
    @JvmStatic
    fun hoCodeTestCases(): Stream<Arguments> = Stream.of(
      // category, subCategory, expectedWeightings, expectedDescriptions, expectedFlagValue
      // Absconding/bail
      Arguments.of(
        80,
        0,
        mapOf(
          "ogrs3Weighting" to 0.7334,
          "snsvStaticWeighting" to 0.3048963,
          "snsvDynamicWeighting" to 0.271350568,
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
      // Acquisitive violent
      Arguments.of(
        29,
        0,
        mapOf(
          "ogrs3Weighting" to 0.0,
          "snsvStaticWeighting" to 0.326858081,
          "snsvDynamicWeighting" to 0.194487132,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Violence",
          "Acquisitive violence",
          "Acquisitive violence",
          "Acquisitive violence",
          "Acquisitive violence",
        ),
        true,
      ),
      // Burglary (domestic)
      Arguments.of(
        28,
        0,
        mapOf(
          "ogrs3Weighting" to -0.1239,
          "snsvStaticWeighting" to 0.151649693,
          "snsvDynamicWeighting" to -0.226808217,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Burglary (domestic)",
          "Burglary (domestic)",
          "Burglary (domestic)",
          "Burglary (domestic)",
          "Burglary (domestic)",
        ),
        false,
      ),
      // Burglary (other)
      Arguments.of(
        30,
        0,
        mapOf(
          "ogrs3Weighting" to 0.2406,
          "snsvStaticWeighting" to -0.194200416,
          "snsvDynamicWeighting" to -0.22599054,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Burglary (other)",
          "Burglary (other)",
          "Burglary (other)",
          "Burglary (other)",
          "Burglary (other)",
        ),
        false,
      ),
      // Criminal damage
      Arguments.of(
        9,
        0,
        mapOf(
          "ogrs3Weighting" to 0.205,
          "snsvStaticWeighting" to 0.186977546,
          "snsvDynamicWeighting" to 0.007780288,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Criminal damage",
          "Criminal damage",
          "Criminal damage",
          "Criminal damage",
          "Criminal damage",
        ),
        true,
      ),
      // Drink-driving
      Arguments.of(
        4,
        6,
        mapOf(
          "ogrs3Weighting" to -0.1214,
          "snsvStaticWeighting" to -0.212258595,
          "snsvDynamicWeighting" to 0.156493599,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Drink driving and related offences",
          "Drink driving",
          "Drink driving",
          "Drink driving",
          "Drink driving",
        ),
        false,
      ),
      // Drug import/export/production
      Arguments.of(
        77,
        61,
        mapOf(
          "ogrs3Weighting" to -0.7956,
          "snsvStaticWeighting" to 0.081898246,
          "snsvDynamicWeighting" to -0.088394525,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Drug import/export/production",
          "Drug import/export/production",
          "Drug import/export/production",
          "Drug import/export/production",
          "Drug import/export/production",
        ),
        false,
      ),
      // Drug possession/supply
      Arguments.of(
        92,
        50,
        mapOf(
          "ogrs3Weighting" to 0.0772,
          "snsvStaticWeighting" to 0.069450782,
          "snsvDynamicWeighting" to 0.081753217,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Drug possession/supply",
          "Drug possession/supply",
          "Drug possession/supply",
          "Drug possession/supply",
          "Drug possession/supply",
        ),
        false,
      ),
      // Drunkenness
      Arguments.of(
        99,
        43,
        mapOf(
          "ogrs3Weighting" to -0.0607,
          "snsvStaticWeighting" to 0.084178964,
          "snsvDynamicWeighting" to 0.081954557,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Other offence",
          "Drunkenness",
          "Drunkenness",
          "Drunkenness",
          "Drunkenness",
        ),
        false,
      ),
      // Fraud and forgery
      Arguments.of(
        50,
        0,
        mapOf(
          "ogrs3Weighting" to 0.1599,
          "snsvStaticWeighting" to -0.136811424,
          "snsvDynamicWeighting" to -0.210828726,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Fraud and forgery",
          "Fraud and forgery",
          "Fraud and forgery",
          "Fraud and forgery",
          "Fraud and forgery",
        ),
        false,
      ),
      // Handling stolen goods
      Arguments.of(
        54,
        0,
        mapOf(
          "ogrs3Weighting" to 0.3519,
          "snsvStaticWeighting" to -0.224505821,
          "snsvDynamicWeighting" to 0.092376574,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Handling stolen goods",
          "Handling stolen goods",
          "Handling stolen goods",
          "Handling stolen goods",
          "Handling stolen goods",
        ),
        false,
      ),
      // Motoring offences
      Arguments.of(
        4,
        4,
        mapOf(
          "ogrs3Weighting" to 0.2622,
          "snsvStaticWeighting" to -0.028644236,
          "snsvDynamicWeighting" to 0.130461601,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Other motoring",
          "Motoring offences",
          "Motoring offences",
          "Motoring offences",
          "Motoring offences",
        ),
        false,
      ),
      // Other offences
      Arguments.of(
        8,
        32,
        mapOf(
          "ogrs3Weighting" to -0.0607,
          "snsvStaticWeighting" to -0.215779995,
          "snsvDynamicWeighting" to 0.123617391,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Other offence",
          "Other offences",
          "Other offences",
          "Other offences",
          "Other offences",
        ),
        false,
      ),
      // Public order and harassment
      Arguments.of(
        64,
        0,
        mapOf(
          "ogrs3Weighting" to 0.1819,
          "snsvStaticWeighting" to 0.044613202,
          "snsvDynamicWeighting" to 0.020639683,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Public order",
          "Public order and harassment",
          "Public order and harassment",
          "Public order and harassment",
          "Public order and harassment",
        ),
        false,
      ),
      // Sexual (against child)
      Arguments.of(
        16,
        12,
        mapOf(
          "ogrs3Weighting" to -0.6534,
          "snsvStaticWeighting" to -0.442179128,
          "snsvDynamicWeighting" to -0.40700568,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Sexual (against child)",
          "Sexual (against child)",
          "Sexual (against child)",
          "Sexual (against child)",
          "Sexual (against child)",
        ),
        true,
      ),
      // Sexual (not against child)
      Arguments.of(
        16,
        0,
        mapOf(
          "ogrs3Weighting" to 0.0328,
          "snsvStaticWeighting" to -0.045522889,
          "snsvDynamicWeighting" to -0.21117235,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Sexual (not against child)",
          "Sexual (not against child)",
          "Sexual (not against child)",
          "Sexual (not against child)",
          "Sexual (not against child)",
        ),
        true,
      ),
      // Theft (non-motor)
      Arguments.of(
        39,
        0,
        mapOf(
          "ogrs3Weighting" to 0.6612,
          "snsvStaticWeighting" to 0.020240798,
          "snsvDynamicWeighting" to 0.018761194,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Theft (non-motor)",
          "Theft (non-motor)",
          "Theft (non-motor)",
          "Theft (non-motor)",
          "Theft (non-motor)",
        ),
        false,
      ),
      // Vehicle-related theft
      Arguments.of(
        37,
        0,
        mapOf(
          "ogrs3Weighting" to 0.3801,
          "snsvStaticWeighting" to 0.226781313,
          "snsvDynamicWeighting" to 0.26444584,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Taking & driving away and related offences",
          "Vehicle-related theft",
          "Vehicle-related theft",
          "Vehicle-related theft",
          "Vehicle-related theft",
        ),
        false,
      ),
      // Violence against the person
      Arguments.of(
        1,
        0,
        mapOf(
          "ogrs3Weighting" to 0.0,
          "snsvStaticWeighting" to 0.019270382,
          "snsvDynamicWeighting" to -0.006538498,
          "snsvVatpStaticWeighting" to 0.238802611,
          "snsvVatpDynamicWeighting" to 0.204895024,
        ),
        listOf(
          "Violence",
          "Violence against the person",
          "Violence against the person",
          "Violence against the person (ABH+)",
          "Violence against the person (ABH+)",
        ),
        true,
      ),
      // Welfare fraud
      Arguments.of(
        53,
        33,
        mapOf(
          "ogrs3Weighting" to 0.1599,
          "snsvStaticWeighting" to -0.016912802,
          "snsvDynamicWeighting" to -0.067383107,
          "snsvVatpStaticWeighting" to 0.0,
          "snsvVatpDynamicWeighting" to 0.0,
        ),
        listOf(
          "Fraud and forgery",
          "Welfare fraud",
          "Welfare fraud",
          "Welfare fraud",
          "Welfare fraud",
        ),
        false,
      ),
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
    // When
    val hoCode = riskActuarialHoCodeRepository.findByCategoryAndSubCategory(category, subCategory)
    val hoCodeFlags = hoCode.riskActuarialHoCodeFlags
    val hoCodeWeightings = hoCode.riskActuarialHoCodeWeightings

    // Then
    assertEquals(category, hoCode.category)
    assertEquals(subCategory, hoCode.subCategory)

    // Verify flag
    assertEquals(1, hoCodeFlags.size)
    assertEquals("opdViolSex", hoCodeFlags.first().flagName)
    assertEquals(expectedFlagValue, hoCodeFlags.first().flagValue)

    // Verify weighting names
    assertThat(hoCodeWeightings.map { it.weightingName })
      .containsExactlyInAnyOrder(
        "ogrs3Weighting",
        "snsvDynamicWeighting",
        "snsvStaticWeighting",
        "snsvVatpDynamicWeighting",
        "snsvVatpStaticWeighting",
      )

    val allMatch = expectedWeightings.all { (key, value) ->
      hoCodeWeightings.any { it.weightingName == key && it.weightingValue == value }
    }
    assert(allMatch)

    // Verify weighting descriptions
    assertThat(hoCodeWeightings.map { it.weightingDesc })
      .containsExactlyElementsOf(expectedDescriptions)
  }

  @Test
  fun `ogrs3 weightings 999 with error`() {
    // When
    val hoCodeWeightings = riskActuarialHoCodeWeightingsRepository.findByWeightingValue(999.0)
    // Then
    assertThat(hoCodeWeightings.all { it.errorCode == RiskActuarialHoCodeErrorCode.NEED_DETAILS_OF_EXACT_OFFENCE }).isTrue()
  }

  @Test
  fun `no empty weighting descriptions`() {
    // When
    val hoCodeWeightings = riskActuarialHoCodeWeightingsRepository.findAll()
    // Then
    assertThat(hoCodeWeightings.all { it.weightingDesc != "" }).isTrue()
  }

  @Test
  fun `no category sub category combination 00000 or 00001 present`() {
    // When
    val hoCode = riskActuarialHoCodeRepository.findAll()
    // Then
    // 00000 = 00, 00001 = 01 in db
    val filtered = hoCode.filter {
      it.category.toString() + it.subCategory.toString() == "00" ||
        it.category.toString() + it.subCategory.toString() == "01"
    }
    assertThat(filtered).isEmpty()
  }

  @Test
  fun `no weighting value 999 present`() {
    // When
    val hoCodeWeightings = riskActuarialHoCodeWeightingsRepository.findAll()
    // Then
    val filtered = hoCodeWeightings.filter { it.weightingValue == 999.0 }
    assertThat(filtered).isEmpty()
  }
}
