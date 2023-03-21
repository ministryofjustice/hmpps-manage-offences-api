package uk.gov.justice.digital.hmpps.manageoffencesapi.model.external.prisonapi

data class OffenceActivationDto(
  val offenceCode: String,
  val statuteCode: String,
  val activationFlag: Boolean,
)
