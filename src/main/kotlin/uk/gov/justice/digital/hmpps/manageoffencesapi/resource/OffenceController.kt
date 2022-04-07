package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.OffenceService
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.SDRSService

@RestController
@RequestMapping("/offences", produces = [MediaType.APPLICATION_JSON_VALUE])
class OffenceController(
  private val offenceService: OffenceService,
  private val sdrsService: SDRSService
) {
  @GetMapping(value = ["/code/{offenceCode}"])
  @ResponseBody
  @Operation(
    summary = "Get all offences matching the passed offence code, does a start with match",
    description = "This endpoint will return the the offences that start with the passed offence code"
  )
  fun getOffencesByOffenceCode(
    @Parameter(required = true, example = "AA1256A", description = "The offence code")
    @PathVariable("offenceCode")
    offenceCode: String
  ): List<Offence> {
    log.info("Request received to fetch offences that start with offenceCode {}", offenceCode)
    return offenceService.findOffencesByCode(offenceCode)
  }

  @GetMapping(value = ["/sdrs/code/{offenceCode}"])
  @ResponseBody
  @Operation(
    summary = "Lookup an offence from the SDRS service",
    description = "This endpoint will return the the offence that matches the passed in offence code"
  )
  fun getOffenceFromSDRS(
    @Parameter(required = true, example = "AA1256A", description = "The offence code")
    @PathVariable("offenceCode")
    offenceCode: String
  ): SDRSResponse {
    log.info("Lookup offence from SDRS")
    return sdrsService.findOffenceByOffenceCode(offenceCode)
  }

  @PostMapping(value = ["/load-all-offences"])
  @ResponseBody
  @Operation(
    summary = "Fetch all offences from SDRS and load into manage offences",
    description = "This endpoint will fetch all offences from SDRS and load into the manage offences DB. This will delete all existing data and reload"
  )
  fun loadAllOffences() {
    log.info("Request received to loadAllOffences")
    sdrsService.loadAllOffences()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
