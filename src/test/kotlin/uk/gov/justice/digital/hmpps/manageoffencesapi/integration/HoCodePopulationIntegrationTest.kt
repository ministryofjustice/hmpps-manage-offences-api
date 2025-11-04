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
      // Acquisitive violent
      Arguments.of(
        29,
        0,
        mapOf(
          "ogrs3Weighting" to 0.0,
          "snsvStaticWeighting" to 0.326858080852591,
          "snsvDynamicWeighting" to 0.194487131847261,
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
          "snsvStaticWeighting" to 0.151649692548355,
          "snsvDynamicWeighting" to -0.226808217378264,
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
          "snsvStaticWeighting" to -0.194200416367268,
          "snsvDynamicWeighting" to -0.225990540250696,
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
          "snsvStaticWeighting" to 0.186977546333514,
          "snsvDynamicWeighting" to 0.00778028771796321,
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
          "snsvStaticWeighting" to -0.212258595209629,
          "snsvDynamicWeighting" to 0.156493599418026,
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
          "snsvStaticWeighting" to 0.0818982459627011,
          "snsvDynamicWeighting" to -0.0883945245425247,
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
          "snsvStaticWeighting" to 0.0694507822486554,
          "snsvDynamicWeighting" to 0.081753216630546,
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
          "snsvStaticWeighting" to 0.0841789642942883,
          "snsvDynamicWeighting" to 0.0819545573517356,
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
          "snsvStaticWeighting" to -0.136811424047985,
          "snsvDynamicWeighting" to -0.210828726274969,
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
          "snsvStaticWeighting" to -0.2245058213129,
          "snsvDynamicWeighting" to 0.0923765737156082,
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
          "snsvStaticWeighting" to -0.0286442357674878,
          "snsvDynamicWeighting" to 0.130461601097636,
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
          "snsvStaticWeighting" to -0.215779995107354,
          "snsvDynamicWeighting" to 0.123617390798186,
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
          "snsvStaticWeighting" to 0.0446132016665715,
          "snsvDynamicWeighting" to 0.0206396832377319,
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
          "snsvStaticWeighting" to -0.442179128494551,
          "snsvDynamicWeighting" to -0.407005679932105,
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
          "snsvStaticWeighting" to -0.0455228893277184,
          "snsvDynamicWeighting" to -0.211172350332101,
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
          "snsvStaticWeighting" to 0.0202407984946185,
          "snsvDynamicWeighting" to 0.0187611938936452,
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
          "snsvStaticWeighting" to 0.226781312933932,
          "snsvDynamicWeighting" to 0.264445840395185,
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
          "snsvStaticWeighting" to 0.01927038224,
          "snsvDynamicWeighting" to -0.006538498404,
          "snsvVatpStaticWeighting" to 0.238802610774108,
          "snsvVatpDynamicWeighting" to 0.204895023669854,
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
          "snsvStaticWeighting" to -0.0169128022430282,
          "snsvDynamicWeighting" to -0.0673831070937991,
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

    // Verify weighting values
    expectedWeightings.forEach { (key, expectedValue) ->
      val actual = hoCodeWeightings.find { it.weightingName == key }
      assertEquals(
        expectedValue,
        actual?.weightingValue,
        "Mismatch for weighting '$key'",
      )
    }

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
