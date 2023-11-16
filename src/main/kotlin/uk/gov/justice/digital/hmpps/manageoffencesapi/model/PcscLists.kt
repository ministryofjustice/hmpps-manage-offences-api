package uk.gov.justice.digital.hmpps.manageoffencesapi.model

data class PcscLists(
  val listA: Set<OffenceToScheduleMapping> = emptySet(),
  val listB: Set<OffenceToScheduleMapping> = emptySet(),
  val listC: Set<OffenceToScheduleMapping> = emptySet(),
  val listD: Set<OffenceToScheduleMapping> = emptySet(),
)
