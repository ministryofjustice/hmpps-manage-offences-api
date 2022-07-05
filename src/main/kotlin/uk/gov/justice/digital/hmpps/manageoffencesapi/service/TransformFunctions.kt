package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.OffenceSchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Schedule as EntitySchedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SchedulePart as EntitySchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle as ModelFeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence as ModelOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Schedule as ModelSchedule
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.SchedulePart as ModelSchedulePart
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.Offence as SdrsOffence

/*
** Functions which transform JPA entity objects into their API model equivalents and vice-versa.
*/
fun transform(offence: Offence): ModelOffence =
  ModelOffence(
    id = offence.id,
    code = offence.code,
    description = offence.description,
    cjsTitle = offence.cjsTitle,
    revisionId = offence.revisionId,
    startDate = offence.startDate,
    endDate = offence.endDate,
    homeOfficeStatsCode = offence.homeOfficeStatsCode,
    changedDate = offence.changedDate,
    loadDate = offence.lastUpdatedDate,
  )

fun transform(sdrsOffence: SdrsOffence): Offence = Offence(
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
)

fun transform(sdrsOffence: SdrsOffence, offence: Offence): Offence =
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
  )

fun transform(loadResult: SdrsLoadResult): MostRecentLoadResult =
  MostRecentLoadResult(
    alphaChar = loadResult.alphaChar,
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
  offencesByParts: Map<Long, List<OffenceSchedulePart>>
) = ModelSchedulePart(
  id = it.id,
  partNumber = it.partNumber,
  offences = offencesByParts[it.id]?.map { o -> transform(o.offence) }
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
)
