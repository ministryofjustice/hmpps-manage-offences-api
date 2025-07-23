package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.resource.ScheduleController.Companion.log
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.ScheduleService

@RestController
@RequestMapping("/schedule/{scheduleId}/part")
class SchedulePartController(private val scheduleService: ScheduleService) {

  @PostMapping(value = ["/create"])
  @PreAuthorize("hasRole('ROLE_UPDATE_OFFENCE_SCHEDULES')")
  @Operation(
    summary = "Create a schedule part",
  )
  fun createSchedulePart(@PathVariable scheduleId: Long, @RequestBody schedulePart: SchedulePart) {
    log.info("Request received to create schedule part with number {}", schedulePart.partNumber)
    scheduleService.createSchedulePart(scheduleId, schedulePart)
  }
}
