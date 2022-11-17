package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.NomisChangeHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.LoadHistoryService
import java.time.LocalDate

@RestController
@RequestMapping("/change-history", produces = [MediaType.APPLICATION_JSON_VALUE])
class ChangeHistoryController(
  private val loadHistoryService: LoadHistoryService
) {
  @GetMapping(value = ["/nomis"])
  @ResponseBody
  @Operation(
    summary = "Fetch changes pushed to NOMIS between a from and to date range (to defaults to now)"
  )
  fun getOffencesByOffenceCode(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam from: LocalDate,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) to: LocalDate?,
  ): List<NomisChangeHistory> {
    log.info("Request received to fetch nomis load history from {} to {}", from, to)
    return loadHistoryService.getNomisChangeHistory(from, to)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
