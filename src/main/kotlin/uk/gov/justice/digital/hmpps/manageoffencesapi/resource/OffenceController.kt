package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration.Companion.OFFENCE_CODE_TO_HOME_OFFICE_CODE
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.OffenceService

@RestController
@RequestMapping("/offences", produces = [MediaType.APPLICATION_JSON_VALUE])
class OffenceController(
  private val offenceService: OffenceService,
) {
  @GetMapping(value = ["/code/{offenceCode}"])
  @ResponseBody
  @Operation(
    summary = "Get all offences matching the passed offence code, does a start with match",
    description = "This endpoint will return the offences that start with the passed offence code",
  )
  fun getOffencesByOffenceCode(
    @Parameter(required = true, example = "AA1256A", description = "The offence code")
    @PathVariable("offenceCode")
    offenceCode: String,
  ): List<Offence> {
    log.info("Request received to fetch offences that start with offenceCode {}", offenceCode)
    return offenceService.findOffencesByCode(offenceCode)
  }

  @GetMapping(value = ["/search"])
  @ResponseBody
  @Operation(
    summary = "Get all offences matching the passed offence code, does a start with match",
    description = "This endpoint will return the offences that start with the passed offence code",
  )
  fun searchOffences(
    @RequestParam(required = true) searchString: String,
    @RequestParam(required = false, defaultValue = "false") excludeLegislation: Boolean,
  ): List<Offence> {
    log.info("Request received to search offences that start with searchString {}", searchString)
    return offenceService.searchOffences(searchString, excludeLegislation)
  }

  @GetMapping(value = ["/id/{offenceId}"])
  @ResponseBody
  @Operation(
    summary = "Get offence matching the passed ID",
    description = "This endpoint will return the offence that matches the unique ID passed in",
  )
  fun getOffenceById(
    @Parameter(required = true, example = "123456", description = "The offence ID")
    @PathVariable("offenceId")
    offenceId: Long,
  ): Offence {
    log.info("Request received to fetch offence for offenceId {}", offenceId)
    return offenceService.findOffenceById(offenceId)
  }

  @GetMapping(value = ["/code/unique/{offenceCode}"])
  @ResponseBody
  @Operation(
    summary = "Get the unique offence matching the passed offence code",
    description = "This endpoint will return the offence that matches the unique code passed in",
  )
  fun getOffenceByCode(
    @Parameter(required = true, example = "COML025", description = "The offence Code")
    @PathVariable("offenceCode")
    offenceCode: String,
  ): Offence {
    log.info("Request received to fetch offence for offenceCode {}", offenceCode)
    return offenceService.findOffenceByCode(offenceCode)
  }

  @GetMapping(value = ["/code/multiple"])
  @ResponseBody
  @Operation(
    summary = "Get a list of offences that match the passed in list of offence codes",
    description = "This endpoint will return the offences that match the codes passed in",
  )
  fun getOffencesByCodes(
    @RequestParam offenceCodes: List<String>,
  ): List<Offence> {
    log.info("Request received to fetch offences for ${offenceCodes.size} offence codes")
    return offenceService.findOffenceByCodes(offenceCodes)
  }

  @Cacheable(OFFENCE_CODE_TO_HOME_OFFICE_CODE)
  @GetMapping(value = ["/ho-code/{offenceCode}"])
  @ResponseBody
  @Operation(
    summary = "Get the HO Code associated with an offence code",
    description = "This endpoint will return the HO Code associated with an offence code, could return null",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Offence code exists and associated hoCode returned (could be null/empty)",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No offence exists for the passed in offence code",
      ),
    ],
  )
  fun getHoCodeByOffenceCode(
    @Parameter(required = true, example = "AA1256A", description = "The offence code")
    @PathVariable("offenceCode")
    offenceCode: String,
  ): String? {
    log.info("Request received to fetch HO Code for offenceCode {}", offenceCode)
    return offenceService.findHoCodeByOffenceCode(offenceCode)
  }

  @GetMapping(value = ["/load-results"])
  @ResponseBody
  @Operation(
    summary = "Get the results of the most recent load",
    description = "Get the results of the most recent load",
  )
  fun findLoadResults(): List<MostRecentLoadResult> {
    log.info("Request received to find the most recent load results")
    return offenceService.findLoadResults()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
