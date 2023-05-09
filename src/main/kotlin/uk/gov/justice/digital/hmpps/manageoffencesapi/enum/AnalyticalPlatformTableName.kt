package uk.gov.justice.digital.hmpps.manageoffencesapi.enum

import uk.gov.justice.digital.hmpps.manageoffencesapi.entity.HomeOfficeCode
import uk.gov.justice.digital.hmpps.manageoffencesapi.service.HomeOfficeCodeToOffenceMapping

enum class AnalyticalPlatformTableName(val s3Path: String, val clazz: Class<out Any>) {
  HO_CODES("ho-offence-codes/dev/domain_name=general/database_name=lookup_offence_v2_dev_dbt/table_name=ho_offence_codes_stg/", HomeOfficeCode::class.java), // TODO replace path with production path from AP when it is ready
  HO_CODES_TO_OFFENCE_MAPPING("ho-offence-codes/dev/domain_name=general/database_name=lookup_offence_v2_dev_dbt/table_name=cjs_offence_code_to_ho_offence_code_stg/", HomeOfficeCodeToOffenceMapping::class.java), // TODO replace path with production path from AP when it is ready
}
