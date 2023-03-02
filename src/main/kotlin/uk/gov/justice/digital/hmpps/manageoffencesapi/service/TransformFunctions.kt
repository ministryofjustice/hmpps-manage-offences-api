package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.NomisChangeHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.ChangeType
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisChangeType.OFFENCE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.NomisChangeType.STATUTE
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.SdrsCache
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.BasicOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.LinkOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.OffenceToScheduleMapping
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.ScheduleDetails
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.NomisChangeHistory as EntityNomisChangeHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule as EntitySchedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart as EntitySchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle as ModelFeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.NomisChangeHistory as ModelNomisChangeHistory
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence as ModelOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule as ModelSchedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePart as ModelSchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Offence as PrisonApiOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi.Statute as PrisonApiStatute
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.Offence as SdrsOffence

/*
** Functions which transform JPA entity objects into their API model equivalents and vice-versa.
*/
fun transform(offence: Offence, childOffenceIds: List<Long>? = emptyList()): ModelOffence =
  ModelOffence(
    id = offence.id,
    code = offence.code,
    description = offence.description,
    offenceType = offence.offenceType,
    cjsTitle = offence.cjsTitle,
    revisionId = offence.revisionId,
    startDate = offence.startDate,
    endDate = offence.endDate,
    homeOfficeStatsCode = offence.homeOfficeStatsCode,
    changedDate = offence.changedDate,
    loadDate = offence.lastUpdatedDate,
    isChild = offence.parentCode != null,
    parentOffenceId = offence.parentOffenceId,
    childOffenceIds = childOffenceIds ?: emptyList(),
  )

fun transform(offenceScheduleMapping: OffenceScheduleMapping): OffenceToScheduleMapping =
  OffenceToScheduleMapping(
    id = offenceScheduleMapping.offence.id,
    code = offenceScheduleMapping.offence.code,
    description = offenceScheduleMapping.offence.description,
    offenceType = offenceScheduleMapping.offence.offenceType,
    cjsTitle = offenceScheduleMapping.offence.cjsTitle,
    revisionId = offenceScheduleMapping.offence.revisionId,
    startDate = offenceScheduleMapping.offence.startDate,
    endDate = offenceScheduleMapping.offence.endDate,
    homeOfficeStatsCode = offenceScheduleMapping.offence.homeOfficeStatsCode,
    changedDate = offenceScheduleMapping.offence.changedDate,
    loadDate = offenceScheduleMapping.offence.lastUpdatedDate,
    isChild = offenceScheduleMapping.offence.parentCode != null,
    parentOffenceId = offenceScheduleMapping.offence.parentOffenceId,
    lineReference = offenceScheduleMapping.lineReference,
    legislationText = offenceScheduleMapping.legislationText,
    paragraphTitle = offenceScheduleMapping.paragraphTitle,
    paragraphNumber = offenceScheduleMapping.paragraphNumber,
  )

fun transform(offenceScheduleMappings: List<OffenceScheduleMapping>?): List<ScheduleDetails> =
  offenceScheduleMappings?.groupBy { it.schedulePart.schedule }?.map {
    ScheduleDetails(
      id = it.key.id,
      act = it.key.act,
      code = it.key.code,
      url = it.key.url,
      schedulePartNumbers = it.value.map { offenceSchedulePart -> offenceSchedulePart.schedulePart.partNumber }
    )
  } ?: emptyList()

fun transform(sdrsOffence: SdrsOffence, cache: SdrsCache): Offence = Offence(
  code = sdrsOffence.code,
  description = sdrsOffence.description,
  cjsTitle = sdrsOffence.cjsTitle,
  revisionId = sdrsOffence.offenceRevisionId,
  startDate = sdrsOffence.offenceStartDate,
  endDate = sdrsOffence.offenceEndDate,
  category = sdrsOffence.category,
  subCategory = sdrsOffence.subCategory,
  changedDate = sdrsOffence.changedDate,
  actsAndSections = sdrsOffence.offenceActsAndSections,
  sdrsCache = cache,
  offenceType = sdrsOffence.offenceType,
)

fun transform(sdrsOffence: SdrsOffence, offence: Offence, sdrsCache: SdrsCache): Offence =
  offence.copy(
    description = sdrsOffence.description,
    cjsTitle = sdrsOffence.cjsTitle,
    revisionId = sdrsOffence.offenceRevisionId,
    startDate = sdrsOffence.offenceStartDate,
    endDate = sdrsOffence.offenceEndDate,
    category = sdrsOffence.category,
    subCategory = sdrsOffence.subCategory,
    changedDate = sdrsOffence.changedDate,
    actsAndSections = sdrsOffence.offenceActsAndSections,
    offenceType = sdrsOffence.offenceType,
    sdrsCache = sdrsCache
  )

fun transform(loadResult: SdrsLoadResult): MostRecentLoadResult =
  MostRecentLoadResult(
    sdrsCache = loadResult.cache,
    status = loadResult.status,
    type = loadResult.loadType,
    loadDate = loadResult.loadDate,
    lastSuccessfulLoadDate = loadResult.lastSuccessfulLoadDate
  )

fun transform(it: FeatureToggle) = ModelFeatureToggle(feature = it.feature, enabled = it.enabled)

fun transform(schedulePart: ModelSchedulePart, entitySchedule: EntitySchedule) =
  EntitySchedulePart(
    schedule = entitySchedule,
    partNumber = schedulePart.partNumber,
  )

fun transform(schedule: ModelSchedule) =
  EntitySchedule(
    act = schedule.act,
    code = schedule.code,
    url = schedule.url,
  )

fun transform(
  it: EntitySchedulePart,
  offencesByParts: Map<Long, List<OffenceScheduleMapping>>
) = ModelSchedulePart(
  id = it.id,
  partNumber = it.partNumber,
  offences = offencesByParts[it.id]?.map { transform(it) }?.sortedBy { it.code }
)

fun transform(
  schedule: EntitySchedule,
  scheduleParts: List<ModelSchedulePart>
) = ModelSchedule(
  id = schedule.id,
  act = schedule.act,
  code = schedule.code,
  url = schedule.url,
  scheduleParts = scheduleParts,
)

fun transform(it: EntitySchedule) = ModelSchedule(
  id = it.id,
  act = it.act,
  code = it.code,
  url = it.url,
  scheduleParts = null,
)

fun transform(offence: PrisonApiOffence, changeType: ChangeType) =
  NomisChangeHistory(
    code = offence.code,
    description = offence.description,
    changeType = changeType,
    nomisChangeType = OFFENCE
  )

fun transform(statute: PrisonApiStatute, changeType: ChangeType) =
  NomisChangeHistory(
    code = statute.code,
    description = statute.description,
    changeType = changeType,
    nomisChangeType = STATUTE
  )

fun transform(it: EntityNomisChangeHistory): ModelNomisChangeHistory =
  ModelNomisChangeHistory(
    id = it.id,
    code = it.code,
    description = it.description,
    changeType = it.changeType,
    nomisChangeType = it.nomisChangeType,
    sentToNomisDate = it.sentToNomisDate,
  )

fun transform(
  offence: Offence,
  it: OffenceScheduleMapping
) =
  OffenceScheduleMapping(
    offence = offence,
    schedulePart = it.schedulePart,
    paragraphNumber = it.paragraphNumber,
    paragraphTitle = it.paragraphTitle,
    legislationText = it.legislationText,
    lineReference = it.lineReference
  )

fun transform(offence: Offence, children: List<Offence>): OffenceToScheduleMapping =
  OffenceToScheduleMapping(
    id = offence.id,
    code = offence.code,
    description = offence.description,
    offenceType = offence.offenceType,
    cjsTitle = offence.cjsTitle,
    revisionId = offence.revisionId,
    startDate = offence.startDate,
    endDate = offence.endDate,
    homeOfficeStatsCode = offence.homeOfficeStatsCode,
    changedDate = offence.changedDate,
    loadDate = offence.lastUpdatedDate,
    isChild = offence.parentCode != null,
    parentOffenceId = offence.parentOffenceId,
    childOffences = children.map { transform(it) }
  )

fun transform(it: Offence): BasicOffence = BasicOffence(
  id = it.id,
  code = it.code,
  title = it.cjsTitle,
  startDate = it.startDate,
  endDate = it.endDate,
)

fun transform(
  schedulePart: SchedulePart,
  offence: Offence,
  linkOffence: LinkOffence
) = OffenceScheduleMapping(
  schedulePart = schedulePart,
  offence = offence,
  paragraphNumber = linkOffence.paragraphNumber,
  paragraphTitle = linkOffence.paragraphTitle,
  lineReference = linkOffence.lineReference,
  legislationText = linkOffence.legislationText
)
