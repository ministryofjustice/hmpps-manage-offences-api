package uk.gov.justice.digital.hmpps.manageoffencesapi.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.RiskActuarialHoCodeErrorCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.RiskActuarialHoCodeFlagsRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.RiskActuarialHoCodeRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.RiskActuarialHoCodeWeightingsRepository
import java.time.LocalDateTime

@SpringBootTest(properties = ["spring.profiles.active=test"])
@Transactional
class RiskActuarialHoCodeTest(
  @Autowired private val riskActuarialHoCodeRepository: RiskActuarialHoCodeRepository,
  @Autowired private val riskActuarialHoCodeFlagsRepository: RiskActuarialHoCodeFlagsRepository,
  @Autowired private val riskActuarialHoCodeWeightingsRepository: RiskActuarialHoCodeWeightingsRepository,
) {

  @Test
  fun `Should save a valid risk actuarial HoCode`() {
    val fixedLocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0)

    val hoCode = RiskActuarialHoCode(
      category = 123,
      subCategory = 45,
      createdDate = fixedLocalDateTime,
    )

    val hoCodeFlagOne = RiskActuarialHoCodeFlags(
      flagName = "hoCodeFlagOne",
      flagValue = true,
      createdDate = fixedLocalDateTime,
      riskActuarialHoCode = hoCode,
    )

    val hoCodeFlagTwo = RiskActuarialHoCodeFlags(
      flagName = "hoCodeFlagTwo",
      flagValue = false,
      createdDate = fixedLocalDateTime,
      riskActuarialHoCode = hoCode,
    )

    val hoCodeWeightingOne = RiskActuarialHoCodeWeightings(
      weightingName = "hoCodeWeightingNameOne",
      weightingValue = 0.123,
      weightingDesc = "some description of the weighting one",
      riskActuarialHoCode = hoCode,
    )

    val hoCodeWeightingTwo = RiskActuarialHoCodeWeightings(
      weightingName = "hoCodeWeightingNameTwo",
      weightingValue = 0.456,
      weightingDesc = "some description of the weighting two",
      errorCode = RiskActuarialHoCodeErrorCode.NEED_DETAILS_OF_EXACT_OFFENCE,
      riskActuarialHoCode = hoCode,
    )

    hoCode.riskActuarialHoCodeFlags.addAll(listOf(hoCodeFlagOne, hoCodeFlagTwo))
    hoCode.riskActuarialHoCodeWeightings.addAll(listOf(hoCodeWeightingOne, hoCodeWeightingTwo))

    val savedHoCode = riskActuarialHoCodeRepository.save(hoCode)

    assertNotEquals(0, savedHoCode.id)
    assertEquals(2, riskActuarialHoCodeFlagsRepository.count())
    assertEquals(2, riskActuarialHoCodeWeightingsRepository.count())

    val fetchedHoCode = riskActuarialHoCodeRepository.findById(savedHoCode.id).orElseThrow()
    assertEquals(123, fetchedHoCode.category)
    assertEquals(45, fetchedHoCode.subCategory)
    assertEquals(fixedLocalDateTime, fetchedHoCode.createdDate)

    assertEquals(2, fetchedHoCode.riskActuarialHoCodeFlags.size)
    assertEquals(2, fetchedHoCode.riskActuarialHoCodeWeightings.size)

    val fetchedFlagOne = fetchedHoCode.riskActuarialHoCodeFlags.first { it.id == hoCodeFlagOne.id }

    assertEquals(hoCodeFlagOne.flagName, fetchedFlagOne.flagName)
    assertEquals(hoCodeFlagOne.flagValue, fetchedFlagOne.flagValue)
    assertEquals(hoCodeFlagOne.createdDate, fetchedFlagOne.createdDate)

    val fetchedFlagTwo = fetchedHoCode.riskActuarialHoCodeFlags.first { it.id == hoCodeFlagTwo.id }

    assertEquals(hoCodeFlagTwo.flagName, fetchedFlagTwo.flagName)
    assertEquals(hoCodeFlagTwo.flagValue, fetchedFlagTwo.flagValue)
    assertEquals(hoCodeFlagTwo.createdDate, fetchedFlagTwo.createdDate)

    val fetchedWeightingOne = fetchedHoCode.riskActuarialHoCodeWeightings.first { it.id == hoCodeWeightingOne.id }

    assertEquals(hoCodeWeightingOne.weightingName, fetchedWeightingOne.weightingName)
    assertEquals(hoCodeWeightingOne.weightingValue, fetchedWeightingOne.weightingValue)
    assertEquals(hoCodeWeightingOne.weightingDesc, fetchedWeightingOne.weightingDesc)
    assertNull(fetchedWeightingOne.errorCode)

    val fetchedWeightingTwo = fetchedHoCode.riskActuarialHoCodeWeightings.first { it.id == hoCodeWeightingTwo.id }

    assertEquals(hoCodeWeightingTwo.weightingName, fetchedWeightingTwo.weightingName)
    assertEquals(hoCodeWeightingTwo.weightingValue, fetchedWeightingTwo.weightingValue)
    assertEquals(hoCodeWeightingTwo.weightingDesc, fetchedWeightingTwo.weightingDesc)
    assertEquals(hoCodeWeightingTwo.errorCode, fetchedWeightingTwo.errorCode)
  }
}
