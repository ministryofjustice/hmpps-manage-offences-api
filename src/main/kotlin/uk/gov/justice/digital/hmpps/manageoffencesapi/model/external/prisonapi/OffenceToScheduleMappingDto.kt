package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi

data class OffenceToScheduleMappingDto(
  val offenceCode: String,
  val schedule: String,
)
