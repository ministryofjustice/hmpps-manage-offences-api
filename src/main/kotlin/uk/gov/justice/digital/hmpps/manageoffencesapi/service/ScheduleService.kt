package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ScheduleParagraph
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ScheduleParagraphIdAndOffenceId
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.OffenceScheduleMappingRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleParagraphRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.SchedulePartRepository
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.ScheduleRepository
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.ScheduleParagraph as EntityScheduleParagraph
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule as ModelSchedule

@Service
class ScheduleService(
  private val scheduleRepository: ScheduleRepository,
  private val schedulePartRepository: SchedulePartRepository,
  private val scheduleParagraphRepository: ScheduleParagraphRepository,
  private val offenceScheduleMappingRepository: OffenceScheduleMappingRepository,
  private val offenceRepository: OffenceRepository,
) {
  @Transactional
  fun createSchedule(schedule: ModelSchedule) {
    scheduleRepository.findOneByActAndCode(schedule.act, schedule.code)
      .ifPresent { throw EntityExistsException(it.id.toString()) }

    val scheduleEntity = scheduleRepository.save(transform(schedule))
    if (schedule.scheduleParts != null) {
      schedulePartRepository.saveAll(
        schedule.scheduleParts.map { schedulePart ->
          transform(
            schedulePart,
            scheduleEntity
          )
        }
      )
    }
  }

  /*
    If an offence in @offenceIds has any children (inchoate offences) they are linked automatically
    Any offences in @offenceIds that are themselves children will be ignored
   */
  // @Transactional
  // fun linkOffences(schedulePartId: Long, offenceIds: Set<Long>) {
  //   val schedulePart = schedulePartRepository.findById(schedulePartId)
  //     .orElseThrow { EntityNotFoundException("No schedulePart exists for $schedulePartId") }
  //
  //   val childOffences = offenceRepository.findByParentOffenceIdIn(offenceIds)
  //   val childOffenceIds = childOffences.map { it.id }.toSet()
  //
  //   val parentOffences = offenceRepository.findAllById(offenceIds.minus(childOffenceIds))
  //     .filter { it.parentCode == null }
  //
  //   val offences = parentOffences.plus(childOffences)
  //
  //   val offenceScheduleParts = offenceSchedulePartRepository.saveAll(
  //     offences.map { offence ->
  //       OffenceSchedulePart(
  //         schedulePart = schedulePart,
  //         offence = offence
  //       )
  //     }
  //   )
  //   offenceToScheduleHistoryRepository.saveAll(offenceScheduleParts.map { transform(it, INSERT) })
  // }

  /*
    If an offence in @offenceSchedulePartIds has any children (inchoate offences) they are unlinked automatically
    Any offences in @offenceIds that are themselves children will be ignored
   */
  @Transactional
  fun unlinkOffences(scheduleParagraphIdAndOffenceIds: List<ScheduleParagraphIdAndOffenceId>) {
    val allOffenceIds = scheduleParagraphIdAndOffenceIds.map { it.offenceId }
    val parentOffenceIds = offenceRepository.findAllById(allOffenceIds).filter { it.parentCode == null }.map { it.id }
    log.info("Got parent offence ids")

    scheduleParagraphIdAndOffenceIds.forEach {
      if (!parentOffenceIds.contains(it.offenceId)) return@forEach // ignore any children that have been directly passed in
      log.info("DELETING " + it)
      deleteOffenceScheduleMapping(it.scheduleParagraphId, it.offenceId)
      log.info("DONE")

      val childOffenceIds = offenceRepository.findByParentOffenceId(it.offenceId).map { child -> child.id }
      childOffenceIds.forEach { childOffenceId -> deleteOffenceScheduleMapping(it.scheduleParagraphId, childOffenceId) }
    }
  }

  private fun deleteOffenceScheduleMapping(scheduleParagraphId: Long, offenceId: Long) {
    val count = offenceScheduleMappingRepository.deleteByScheduleParagraphIdAndOffenceId(scheduleParagraphId, offenceId)
    log.info("count = $count")
  }

  @Transactional(readOnly = true)
  fun findScheduleById(scheduleId: Long): ModelSchedule {
    val schedule = scheduleRepository.findById(scheduleId)
      .orElseThrow { EntityNotFoundException("No schedule exists for $scheduleId") }

    val offenceScheduleMappings = offenceScheduleMappingRepository.findByScheduleParagraphSchedulePartScheduleId(scheduleId)
    val offencesByParts = offenceScheduleMappings.groupBy { it.scheduleParagraph.schedulePart }

    var scheduleParts: List<SchedulePart>?
    if (!offenceScheduleMappings.isEmpty()) {
      log.info("************************** 1")
      val offencesByParts = offenceScheduleMappings.groupBy { it.scheduleParagraph.schedulePart }
      scheduleParts = offencesByParts.map { e ->
        val osmByParagraph = e.value.groupBy { osms -> osms.scheduleParagraph }
        SchedulePart(
          id = e.key.id,
          partNumber = e.key.partNumber,
          scheduleParagraphs = osmByParagraph.map { osmEntry ->
            ScheduleParagraph(
              id = osmEntry.key.id,
              paragraphNumber = osmEntry.key.paragraphNumber,
              paragraphTitle = osmEntry.key.paragraphTitle,
              offences = osmEntry.value.map { osm2 -> transform(osm2, osmEntry.key.id) }
            )
          }.sortedBy { it.paragraphNumber }
        )
      }
    } else {
      log.info("************************** 2")
      val scheduleParagraphs = scheduleParagraphRepository.findBySchedulePartScheduleId(scheduleId)
      val paragraphsByPart = scheduleParagraphs.groupBy { it.schedulePart }
      scheduleParts = paragraphsByPart.map { e ->
        SchedulePart(
          id = e.key.id,
          partNumber = e.key.partNumber,
          scheduleParagraphs = e.value.map { transform(it) }.sortedBy { it.paragraphNumber }
        )
      }
    }

    log.info("RETURN results")
    // scheduleParts.forEach {sp ->
    //   sp.scheduleParagraphs.forEach { para ->
    //     log.info("${para.paragraphNumber} size : " + para.offences?.size)
    //   }
    // }
    val x = transform(schedule, scheduleParts)
    return x
  }

  private fun transform(it: EntityScheduleParagraph) = ScheduleParagraph(
    id = it.id,
    paragraphNumber = it.paragraphNumber,
    paragraphTitle = it.paragraphTitle,
  )

  private fun transform(schedule: Schedule, scheduleParts: List<SchedulePart>) = ModelSchedule(
    id = schedule.id,
    act = schedule.act,
    code = schedule.code,
    url = schedule.url,
    scheduleParts = scheduleParts,
  )

  @Transactional(readOnly = true)
  fun findAllSchedules(): List<ModelSchedule> {
    val schedules = scheduleRepository.findAll()

    return schedules.map {
      transform(it)
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
