package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.OffenceService

@RestController
@RequestMapping("/offences", produces = [MediaType.APPLICATION_JSON_VALUE])
class OffenceController(
  private val offenceService: OffenceService
) {
  @GetMapping(value = ["/code/{offenceCode}"])
  @ResponseBody
  @Operation(
    summary = "Get all offences matching the passed offence code, does a start with match",
    description = "This endpoint will return the offences that start with the passed offence code"
  )
  fun getOffencesByOffenceCode(
    @Parameter(required = true, example = "AA1256A", description = "The offence code")
    @PathVariable("offenceCode")
    offenceCode: String
  ): List<Offence> {
    log.info("Request received to fetch offences that start with offenceCode {}", offenceCode)
    return offenceService.findOffencesByCode(offenceCode)
  }

  @GetMapping(value = ["/ho-code/{offenceCode}"])
  @ResponseBody
  @Operation(
    summary = "Get the HO Code associated with an offence code",
    description = "This endpoint will return the HO Code associated with an offence code, could be null"
  )
  fun getHoCodeByOffenceCode(
    @Parameter(required = true, example = "AA1256A", description = "The offence code")
    @PathVariable("offenceCode")
    offenceCode: String
  ): String? {
    log.info("Request received to fetch HO Code for offenceCode {}", offenceCode)
    return offenceService.findHoCodeByOffenceCode(offenceCode)
  }

  @GetMapping(value = ["/load-results"])
  @ResponseBody
  @Operation(
    summary = "Get the results of the most recent load",
    description = "Get the results of the most recent load"
  )
  fun findLoadResults(): List<MostRecentLoadResult> {
    log.info("Request received to find the most recent load results")
    return offenceService.findLoadResults()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
