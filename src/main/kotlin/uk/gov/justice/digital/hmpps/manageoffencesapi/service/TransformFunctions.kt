package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.Offence
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
  )
}

fun transform(sdrsResponse: SDRSResponse): List<Offence> {
  return sdrsResponse.messageBody.gatewayOperationType.getOffenceResponse!!.offences.map {
    Offence(code = it.code, description = it.description)
  }
}
