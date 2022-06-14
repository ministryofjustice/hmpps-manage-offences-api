package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.FeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.FeatureToggle as ModelFeatureToggle
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence as ModelOffence
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
