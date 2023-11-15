package uk.gov.justice.digital.hmpps.manageoffencesapi.enum

enum class NomisScheduleName {
  PCSC_SDS, // List B: SDS between 4 and 7 years : Schedule 15 Part 2 that attract life + serious violent offences (same as List C)
  PCSC_SDS_PLUS, // List D: Schedule 15 Part 1 + Schedule 15 Part 2 that attract life
  PCSC_SEC_250, // List C: Sec250 >7 years = List C: Schedule 15 Part 2 that attract life + serious violent offences (same as List B)
  SCHEDULE_15,
  SCHEDULE_15_ATTRACTS_LIFE, // List A - Schedule 15 Part 1 + Schedule 15 Part 2 that attract life (exclude all offences that start after 28 June 2022)
  POTENTIAL_LINK_PCSC, // Used for potential pcsc mappings
}
