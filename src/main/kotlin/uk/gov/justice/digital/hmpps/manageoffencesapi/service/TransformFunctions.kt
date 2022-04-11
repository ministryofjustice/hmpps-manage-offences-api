package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.SdrsLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.MostRecentLoadResult
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.sdrs.SDRSResponse
import uk.gov.justice.digital.hmpps.manageoffencesapi.model.Offence as ModelOffence

/*
** Functions which transform JPA entity objects into their API model equivalents and vice-versa.
*/
fun transform(offence: Offence): ModelOffence {
  return ModelOffence(
    id = offence.id,
    code = offence.code,
    description = offence.description,
    cjsTitle = offence.cjsTitle,
    revisionId = offence.revisionId,
    startDate = offence.startDate,
    endDate = offence.endDate,
    changedDate = offence.changedDate,
    loadDate = offence.lastUpdatedDate,
  )
}

fun transform(sdrsResponse: SDRSResponse): List<Offence> {
  return sdrsResponse.messageBody.gatewayOperationType.getOffenceResponse!!.offences.map {
    Offence(
      code = it.code,
      description = it.description,
      cjsTitle = it.cjsTitle,
      revisionId = it.offenceRevisionId,
      startDate = it.offenceStartDate,
      endDate = it.offenceEndDate,
      changedDate = it.changedDate
    )
  }
}

fun transform(loadResult: SdrsLoadResult): MostRecentLoadResult {
  return MostRecentLoadResult(
    alphaChar = loadResult.alphaChar,
    status = loadResult.status,
    type = loadResult.loadType,
    loadDate = loadResult.loadDate,
    lastSuccessfulLoadDate = loadResult.lastSuccessfulLoadDate
  )
}
