package uk.gov.justice.digital.hmpps.manageoffencesapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration.Companion.PCSC_LISTS
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration.Companion.SCHEDULE_19ZA_OFFENCES
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration.Companion.SDS_EARLY_RELEASE_EXCLUSION_LISTS
import uk.gov.justice.digital.hmpps.manageoffencesapi.config.CacheConfiguration.Companion.TORERA_OFFENCE_CODES
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffencePcscMarkers
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceSdsExclusion
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceToScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.PcscLists
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePartIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SdsExclusionLists
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ToreraSchedulePartCodes
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.IsOffenceInScheduleService
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.ScheduleService

@RestController
@RequestMapping("/schedule", produces = [MediaType.APPLICATION_JSON_VALUE])
class ScheduleController(
  private val scheduleService: ScheduleService,
  private val isOffenceInScheduleService: IsOffenceInScheduleService,
) {
  @PostMapping(value = ["/create"])
  @PreAuthorize("hasRole('ROLE_UPDATE_OFFENCE_SCHEDULES')")
  @Operation(
    summary = "Create a schedule",
  )
  fun createSchedule(@RequestBody schedule: Schedule) {
    log.info("Request received to create a schedule with code {}", schedule.code)
    scheduleService.createSchedule(schedule)
  }

  @PostMapping(value = ["/link-offence"])
  @PreAuthorize("hasRole('ROLE_UPDATE_OFFENCE_SCHEDULES')")
  @Operation(
    summary = "Link offence to a schedule part - will also link any associated inchoate offences (i.e. if  passed in offence has children they will also be linked)",
  )
  fun linkOffences(
    @RequestBody linkOffence: LinkOffence,
  ) {
    log.info(
      "Request received to link offence {} to schedule part {}",
      linkOffence.offenceId,
      linkOffence.schedulePartId,
    )
    scheduleService.linkOffences(linkOffence)
  }

  @PostMapping(value = ["/unlink-offences"])
  @PreAuthorize("hasRole('ROLE_UPDATE_OFFENCE_SCHEDULES')")
  @Operation(
    summary = "Unlink offences from schedules - will also unlink any associated inchoate offences (i.e. if any of the passed in offences have children they will also be unlinked)",
  )
  fun unlinkOffences(
    @RequestBody schedulePartIdAndOffenceIds: List<SchedulePartIdAndOffenceId>,
  ) {
    log.info("Request received to unlink offences from schedules")
    scheduleService.unlinkOffences(schedulePartIdAndOffenceIds)
  }

  @GetMapping(value = ["/by-id/{scheduleId}"])
  @Operation(
    summary = "Get schedule by ID - includes all scheduled parts and mapped offences",
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
    summary = "Find all schedules - does not include mapped offences",
  )
  fun findAllSchedules(): List<Schedule> {
    log.info("Request received to find all schedules")
    return scheduleService.findAllSchedules()
  }

  @GetMapping(value = ["/offence-mapping/id/{offenceId}"])
  @ResponseBody
  @Operation(
    summary = "Get offence matching the passed ID - with schedule related data",
    description = "This endpoint will return the offence that matches the unique ID passed in",
  )
  fun getOffenceToScheduleMapping(
    @Parameter(required = true, example = "123456", description = "The offence ID")
    @PathVariable("offenceId")
    offenceId: Long,
  ): OffenceToScheduleMapping {
    log.info("Request received to fetch OffenceWithScheduleData for offenceId {}", offenceId)
    return scheduleService.findOffenceById(offenceId)
  }

  @GetMapping(value = ["/pcsc-indicators"])
  @ResponseBody
  @Operation(
    summary = "Determine if the passed in offence codes are related to any of the PCSC lists",
    description = "This endpoint will return a list of offences and whether they are im any of the PCSC lists",
  )
  fun getPcscMarkers(@RequestParam offenceCodes: List<String>): List<OffencePcscMarkers> {
    log.info("Request received to determine pcsc markers for ${offenceCodes.size} offence codes")
    return isOffenceInScheduleService.findPcscMarkers(offenceCodes)
  }

  @GetMapping(value = ["/sds-early-release-exclusions"])
  @ResponseBody
  @Operation(
    summary = "Determine if the passed in offence codes are to be excluded from early release due to being Sexual, Violent, Domestic Abuse, National Security or Terrorism.",
    description = "This endpoint will return a list of offences and the exclusion category if applicable.",
  )
  fun getSdsExclusionInformation(
    @RequestParam offenceCodes: List<String>,
  ): List<OffenceSdsExclusion> {
    log.info("Request received to determine sexual or violent status for ${offenceCodes.size} offence codes")
    return isOffenceInScheduleService.categoriseSdsExclusionsOffences(offenceCodes)
  }

  @Cacheable(SDS_EARLY_RELEASE_EXCLUSION_LISTS)
  @GetMapping(value = ["/sds-early-release-exclusion-lists"])
  @ResponseBody
  @Operation(
    summary = "Retrieves the lists of all the offences that are to be excluded from early release.",
    description = "This returns five lists for Sexual, Violent, Domestic Abuse, National Security or Terrorism offences.",
  )
  fun getSdsExclusionLists(): SdsExclusionLists {
    log.info("Request received to get list of sexual or violent offences")
    return scheduleService.getSdsExclusionLists()
  }

  @Cacheable(TORERA_OFFENCE_CODES)
  @GetMapping(value = ["/torera-offence-codes"])
  fun getToreraOffenceCodes(): List<String> {
    log.info("Request received to get torera related offence codes")
    return scheduleService.getToreraOffenceCodes()
  }

  @Cacheable(SCHEDULE_19ZA_OFFENCES)
  @GetMapping(value = ["/torera-offence-codes-by-schedule-part"])
  @Operation(
    summary = "Retrieve all schedule 19ZA offence codes by part number",
    description = "Returns list of offence codes broken down by schedule part, commonly used for TORERA offences",
  )
  fun getToreraScheduleParts(): ToreraSchedulePartCodes {
    log.info("Request received to get torera schedule part codes")
    return scheduleService.getToreraOffenceCodesByPart()
  }

  @Cacheable(PCSC_LISTS)
  @GetMapping(value = ["/pcsc-lists"])
  @ResponseBody
  @Operation(
    summary = "Retrieve all PCSC lists",
    description = "This endpoint will return all four PCSC lists",
  )
  fun getPcscLists(): PcscLists {
    log.info("Request received to get PCSC Lists")
    return scheduleService.getPcscLists()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
