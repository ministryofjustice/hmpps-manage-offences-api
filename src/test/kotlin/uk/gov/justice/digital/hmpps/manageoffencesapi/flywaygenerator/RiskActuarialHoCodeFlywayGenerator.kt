package uk.gov.justice.digital.hmpps.manageoffencesapi.flywaygenerator

import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.input.BOMInputStream
import java.io.File

/**
 * Actuarial HO code insert flyway generator
 *
 * This flyway generator takes 1 CSV, with CT offence codes.
 * Maps the codes to the descriptions from the CSV's.
 * Selects the id of the actuarial category table row based on the actuarial_category.
 * Produces a versioned flyway script containing updates in src/main/resources/migration/common
 *
 * Running the generator:
 * - replace the string value of CT_OFFENCES_EXTRACT_PATH to the path to the CSV
 * - run main
 * - check src/main/resources/migration/common for newly generated update script
 */

const val CT_OFFENCES_EXTRACT_PATH = "/path/to/hoCode.csv"

fun main() {
  val migrationDir = File("src/main/resources/migration/common")

  // new flyway script version
  val nextVersion = (
    migrationDir.listFiles()
      ?.mapNotNull { file ->
        Regex("^V(\\d+)__.*\\.sql$")
          .find(file.name)?.groupValues?.get(1)?.toInt()
      }?.maxOrNull() ?: 0
    ) + 1

  val outputFile = File(migrationDir, "V${nextVersion}__risk_actuarial_ho_code.sql")

  // CSV readers and mapping
  val csvFormat = CSVFormat.DEFAULT.builder()
    .setHeader()
    .setSkipHeaderRecord(true)
    .setTrim(true)
    .get()

  val dataInputStream = BOMInputStream.builder()
    .setInputStream(File(CT_OFFENCES_EXTRACT_PATH).inputStream())
    .get()

  val ctOffences = dataInputStream.bufferedReader().use { reader ->
    val csvParser = csvFormat.parse(reader)
    csvParser.map { csvRecord ->
      CTOffenceExtract(
        category = csvRecord.get("CATEGORY"),
        subCategory = csvRecord.get("SUB_CATEGORY"),
        parentGroupDesc = csvRecord.get("PARENT_GROUP_DESCRIPTION").replace("'", "''"),
        categoryDesc = csvRecord.get("CATEGORY_DESCRIPTION").replace("'", "''"),
        subCategoryDesc = csvRecord.get("SUB_CATEGORY_DESCRIPTION").replace("'", "''"),
        categoryId = csvRecord.get("RISK_ACTUARIAL_HO_CODE_CATEGORY_ID"),
      )
    }
  }

  // string builder for start of script
  val weightingUpdateSql = StringBuilder()
  weightingUpdateSql.append("BEGIN;\n\n")

  ctOffences.forEach { offence ->
    weightingUpdateSql.append(
      """
INSERT INTO risk_actuarial_ho_code (category, sub_category, parent_group_description, category_description, sub_category_description, risk_actuarial_ho_code_category_id, created_date)
VALUES (${offence.category}, ${offence.subCategory}, '${offence.parentGroupDesc}', '${offence.categoryDesc}', '${offence.subCategoryDesc}', (SELECT id FROM risk_actuarial_ho_code_category WHERE category_name = '${offence.categoryId}'), CURRENT_TIMESTAMP);
      """.trimIndent(),
    ).append("\n\n")
  }

  // end of script
  weightingUpdateSql.append("COMMIT;\n")
  outputFile.writeText(weightingUpdateSql.toString())
  println("Success: Generated migration")
}

data class CTOffenceExtract(
  val category: String,
  val subCategory: String,
  val parentGroupDesc: String,
  val categoryDesc: String,
  val subCategoryDesc: String,
  val categoryId: String,
)
