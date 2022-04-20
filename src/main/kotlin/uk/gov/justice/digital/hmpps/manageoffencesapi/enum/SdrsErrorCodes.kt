package uk.gov.justice.digital.hmpps.manageoffencesapi.enum

enum class SdrsErrorCodes(val errorCode: String, val description: String) {
  SDRS_99908("SDRS-99908", "The request UUID is not unique"),
  SDRS_99918("SDRS-99918", "The specified cache file is not present in the SDRS cache folder");
}
