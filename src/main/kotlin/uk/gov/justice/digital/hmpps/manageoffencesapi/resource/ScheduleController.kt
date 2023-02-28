package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceWithScheduleData
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.ScheduleService

@RestController
@RequestMapping("/schedule", produces = [MediaType.APPLICATION_JSON_VALUE])
class ScheduleController(
  private val scheduleService: ScheduleService,
) {
  @PostMapping(value = ["/create"])
  @PreAuthorize("hasRole('ROLE_UPDATE_OFFENCE_SCHEDULES')")
  @Operation(
    summary = "Create a schedule"
  )
  fun createSchedule(@RequestBody schedule: Schedule) {
    log.info("Request received to create schedule with code {}", schedule.code)
    scheduleService.createSchedule(schedule)
  }

  @PostMapping(value = ["/link-offence"])
  @PreAuthorize("hasRole('ROLE_UPDATE_OFFENCE_SCHEDULES')")
  @Operation(
    summary = "Link offence to a schedule part - will also link any associated inchoate offences (i.e. if  passed in offence has children they will also be linked)"
  )
  fun linkOffences(
    @RequestBody linkOffence: LinkOffence
  ) {
    log.info("Request received to link offence {} to schedule part {}", linkOffence.offenceId, linkOffence.schedulePartId)
    scheduleService.linkOffences(linkOffence)
  }

  @PostMapping(value = ["/unlink-offences"])
  @PreAuthorize("hasRole('ROLE_UPDATE_OFFENCE_SCHEDULES')")
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

  @GetMapping(value = ["/offence/id/{offenceId}"])
  @ResponseBody
  @Operation(
    summary = "Get offence matching the passed ID - with schedule related data",
    description = "This endpoint will return the offence that matches the unique ID passed in"
  )
  fun getOffenceById(
    @Parameter(required = true, example = "123456", description = "The offence ID")
    @PathVariable("offenceId")
    offenceId: Long
  ): OffenceWithScheduleData {
    log.info("Request received to fetch OffenceWithScheduleData for offenceId {}", offenceId)
    return scheduleService.findOffenceById(offenceId)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
