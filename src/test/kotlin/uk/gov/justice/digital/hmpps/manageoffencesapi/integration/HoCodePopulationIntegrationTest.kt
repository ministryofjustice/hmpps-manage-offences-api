package uk.gov.justice.digital.hmpps.manageoffencesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ActuarialCategory
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ActuarialFlags
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.RiskActuarialHoCodeCategoryRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.RiskActuarialHoCodeRepository
import java.util.stream.Stream

class HoCodePopulationIntegrationTest : IntegrationTestBase() {
  @Autowired
  lateinit var riskActuarialHoCodeRepository: RiskActuarialHoCodeRepository

  @Autowired
  lateinit var riskActuarialHoCodeCategoryRepository: RiskActuarialHoCodeCategoryRepository

  @ParameterizedTest(name = "Category {0}, SubCategory {1}")
  @MethodSource("hoCodeTestCases")
  fun `test HO code data consistency`(
    category: Int,
    subCategory: Int,
    actuarialCategory: ActuarialCategory,
    opdFlag: Boolean,
    violentFlag: Boolean,
  ) {
    // When
    val hoCode = riskActuarialHoCodeRepository.findByCategoryAndSubCategory(category, subCategory)
    val hoCodeFlags = hoCode.riskActuarialHoCodeFlags
    val hoCodeCategory = hoCode.riskActuarialHoCodeCategory

    assertThat(hoCode.parentGroupDescription).isNotNull
    assertThat(hoCode.categoryDescription).isNotNull
    assertThat(hoCode.subCategoryDescription).isNotNull

    assertThat(hoCodeCategory?.categoryName).isEqualTo(actuarialCategory)

    assertThat(hoCodeFlags.size).isEqualTo(2)
    assertThat(hoCodeFlags.get(0).flagName).isEqualTo(ActuarialFlags.OPD_VIOLENCE_SEX_FLAG)
    assertThat(hoCodeFlags.get(0).flagValue).isEqualTo(opdFlag)
    assertThat(hoCodeFlags.get(1).flagName).isEqualTo(ActuarialFlags.IS_VIOLENT_SANCTION)
    assertThat(hoCodeFlags.get(1).flagValue).isEqualTo(violentFlag)
  }

  companion object {
    @JvmStatic
    fun hoCodeTestCases(): Stream<Arguments> = Stream.of(
      // category, subCategory, expectedKey, actuarialCategory, opdFlag, violentFlag
      // UNKNOWN category
      Arguments.of(
        0,
        0,
        ActuarialCategory.UNKNOWN,
        false,
        false,
      ),
      // BURGLARY_DOMESTIC category
      Arguments.of(
        28,
        1,
        ActuarialCategory.BURGLARY_DOMESTIC,
        false,
        false,
      ),
      // BURGLARY_OTHER category
      Arguments.of(
        30,
        1,
        ActuarialCategory.BURGLARY_OTHER,
        false,
        false,
      ),
      // DRUNKENNESS category
      Arguments.of(
        99,
        43,
        ActuarialCategory.DRUNKENNESS,
        false,
        true,
      ),
      // DRINK_DRIVING category
      Arguments.of(
        4,
        6,
        ActuarialCategory.DRINK_DRIVING,
        false,
        false,
      ),
      // MOTORING_OFFENCES category
      Arguments.of(
        4,
        4,
        ActuarialCategory.MOTORING_OFFENCES,
        false,
        false,
      ),
      // VEHICLE_RELATED_THEFT category
      Arguments.of(
        37,
        2,
        ActuarialCategory.VEHICLE_RELATED_THEFT,
        false,
        false,
      ),
      // FRAUD_AND_FORGERY category
      Arguments.of(
        38,
        1,
        ActuarialCategory.FRAUD_AND_FORGERY,
        false,
        false,
      ),
      // WELFARE_FRAUD category
      Arguments.of(
        53,
        33,
        ActuarialCategory.WELFARE_FRAUD,
        false,
        false,
      ),
      // DRUG_IMPORT_EXPORT_OR_PRODUCTION category
      Arguments.of(
        77,
        50,
        ActuarialCategory.DRUG_IMPORT_EXPORT_OR_PRODUCTION,
        false,
        false,
      ),
      // DRUG_POSSESSION_OR_SUPPLY category
      Arguments.of(
        92,
        59,
        ActuarialCategory.DRUG_POSSESSION_OR_SUPPLY,
        false,
        false,
      ),
      // VIOLENCE_AGAINST_THE_PERSON_ABH_PLUS category
      Arguments.of(
        1,
        1,
        ActuarialCategory.VIOLENCE_AGAINST_THE_PERSON_ABH_PLUS,
        true,
        true,
      ),
      // VIOLENCE_AGAINST_THE_PERSON_SUB_ABH category
      Arguments.of(
        8,
        21,
        ActuarialCategory.VIOLENCE_AGAINST_THE_PERSON_SUB_ABH,
        true,
        true,
      ),
      // WEAPONS_NON_FIREARM category
      Arguments.of(
        8,
        11,
        ActuarialCategory.WEAPONS_NON_FIREARM,
        true,
        true,
      ),
      // FIREARMS_MOST_SERIOUS category
      Arguments.of(
        5,
        14,
        ActuarialCategory.FIREARMS_MOST_SERIOUS,
        true,
        true,
      ),
      // FIREARMS_OTHER category
      Arguments.of(
        81,
        3,
        ActuarialCategory.FIREARMS_OTHER,
        true,
        true,
      ),
      // HANDLING_STOLEN_GOODS category
      Arguments.of(
        54,
        1,
        ActuarialCategory.HANDLING_STOLEN_GOODS,
        false,
        false,
      ),
      // CRIMINAL_DAMAGE category
      Arguments.of(
        56,
        1,
        ActuarialCategory.CRIMINAL_DAMAGE,
        false,
        true,
      ),
      // ACQUISITIVE_VIOLENCE category
      Arguments.of(
        34,
        1,
        ActuarialCategory.ACQUISITIVE_VIOLENCE,
        false,
        true,
      ),
      // OTHER_OFFENCES category
      Arguments.of(
        8,
        32,
        ActuarialCategory.OTHER_OFFENCES,
        false,
        false,
      ),
      // ABSCONDING_OR_BAIL category
      Arguments.of(
        80,
        1,
        ActuarialCategory.ABSCONDING_OR_BAIL,
        false,
        false,
      ),
      // SEXUAL_AGAINST_CHILD category
      Arguments.of(
        19,
        11,
        ActuarialCategory.SEXUAL_AGAINST_CHILD,
        true,
        false,
      ),
      // SEXUAL_NOT_AGAINST_CHILD category
      Arguments.of(
        16,
        2,
        ActuarialCategory.SEXUAL_NOT_AGAINST_CHILD,
        false,
        false,
      ),
      // THEFT_NON_MOTOR category
      Arguments.of(
        49,
        10,
        ActuarialCategory.THEFT_NON_MOTOR,
        false,
        false,
      ),
    )
  }
}
