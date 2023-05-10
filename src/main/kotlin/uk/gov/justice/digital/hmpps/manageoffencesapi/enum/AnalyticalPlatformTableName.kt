package uk.gov.justice.digital.hmpps.manageoffencesapi.enum

import uk.gov.justice.digital.hmpps.manageoffencesapi.service.HomeOfficeCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.HomeOfficeCodeToOffenceMapping

enum class AnalyticalPlatformTableName(val s3BasePath: String, val mappingClass: Class<out Any>) {
  HO_CODES("ho-offence-codes/prod/domain_name=general/database_name=lookup_offence_v2/table_name=ho_offence_codes/", HomeOfficeCode::class.java),
  HO_CODES_TO_OFFENCE_MAPPING("ho-offence-codes/prod/domain_name=general/database_name=lookup_offence_v2/table_name=cjs_offence_code_to_ho_offence_code/", HomeOfficeCodeToOffenceMapping::class.java),
}
