package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.ScheduleService

@RestController
@RequestMapping("/schedule", produces = [MediaType.APPLICATION_JSON_VALUE])
class ScheduleController(
  private val scheduleService: ScheduleService,
) {
  @PostMapping(value = ["/create"])
  @Operation(
    summary = "Create a schedule"
  )
  fun createSchedule(@RequestBody schedule: Schedule) {
    log.info("Request received to create schedule with code {}", schedule.code)
    scheduleService.createSchedule(schedule)
  }

  @PostMapping(value = ["/link-offences/{schedulePartId}"])
  @Operation(
    summary = "Link offences to a schedule part - will also link any associated inchoate offences (i.e. if any of the passed in offences have children they will also be linked)"
  )
  fun linkOffences(
    @Parameter(required = true, example = "1000011", description = "The schedule part ID")
    @PathVariable("schedulePartId")
    schedulePartId: Long,
    @RequestBody offenceIds: List<Long>
  ) {
    log.info("Request received to link offences to schedule part {}", schedulePartId)
    scheduleService.linkOffences(schedulePartId, offenceIds)
  }

  @PostMapping(value = ["/unlink-offences"])
  @Operation(
    summary = "Unlink offences from schedules - will also unlink any associated inchoate offences (i.e. if any of the passed in offences have children they will also be unlinked)"
  )
  fun unlinkOffences(
    @RequestBody schedulePartIdAndOffenceIds: List<SchedulePartIdAndOffenceId>
  ) {
    log.info("Request received to unlink offences from schedules")
    scheduleService.unlinkOffences(schedulePartIdAndOffenceIds)
  }

  @GetMapping(value = ["/by-id/{scheduleId}"])
  @Operation(
    summary = "Get schedule by ID - includes all scheduled parts and mapped offences"
  )
  fun findScheduleById(
    @Parameter(required = true, example = "1000011", description = "The schedule ID")
    @PathVariable("scheduleId")
    scheduleId: Long,
  ): Schedule {
    log.info("Request received to find schedule by id {}", scheduleId)
    return scheduleService.findScheduleById(scheduleId)
  }

  @GetMapping(value = ["/all"])
  @Operation(
    summary = "Find all schedules - does not include mapped offences"
  )
  fun findAllSchedules(): List<Schedule> {
    log.info("Request received to find all schedules")
    return scheduleService.findAllSchedules()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
