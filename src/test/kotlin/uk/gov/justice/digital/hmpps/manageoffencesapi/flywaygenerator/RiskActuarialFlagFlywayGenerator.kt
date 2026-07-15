package uk.gov.justice.digital.hmpps.manageoffencesapi.flywaygenerator

import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.input.BOMInputStream
import java.io.File

/**
 * Actuarial flag insert flyway generator
 *
 * This flyway generator takes 2 CSV's, one for the opdViolenceSex flag and one for isViolentSanction flag.
 * Inserts the flag name and value from the CSV's.
 * Selects the id of the HO code table row based on the combination of code and sub code.
 * Produces a versioned flyway script containing updates in src/main/resources/migration/common
 *
 * Running the generator:
 * - replace the string value of OPD_FLAG_PATH to the path to the opdViolenceSex flag CSV containing new entries
 * - replace the string value of VIOLENT_FLAG_PATH to the path to the isViolentSanction flag CSV containing new entries
 * - run main
 * - check src/main/resources/migration/common for newly generated update script
 */

const val OPD_FLAG_PATH = "/path/to/opdFlag.csv"
const val VIOLENT_FLAG_PATH = "/path/to/violentFlag.csv"

fun main() {
  val migrationDir = File("src/main/resources/migration/common")

  val nextVersion = (
    migrationDir.listFiles()
      ?.mapNotNull { file ->
        Regex("^V(\\d+)__.*\\.sql$")
          .find(file.name)?.groupValues?.get(1)?.toInt()
      }?.maxOrNull() ?: 0
    ) + 1

  val outputFile = File(migrationDir, "V${nextVersion}__risk_actuarial_flag_mapping.sql")

  // CSV readers and mapping
  val csvFormat = CSVFormat.DEFAULT.builder()
    .setHeader()
    .setSkipHeaderRecord(true)
    .setTrim(true)
    .get()

  val opdFlagStream = BOMInputStream.builder()
    .setInputStream(File(OPD_FLAG_PATH).inputStream())
    .get()

  val opdFlags = opdFlagStream.bufferedReader().use { reader ->
    val csvParser = csvFormat.parse(reader)
    csvParser.map { csvRecord ->
      FlagExtract(
        category = csvRecord.get("CATEGORY"),
        subCategory = csvRecord.get("SUB CAT"),
        flagName = "OPD_VIOLENCE_SEX_FLAG",
        flagValue = csvRecord.get("OPD_VIOLENCE_SEX_FLAG"),
      )
    }
  }

  val violentFlagStream = BOMInputStream.builder()
    .setInputStream(File(VIOLENT_FLAG_PATH).inputStream())
    .get()

  val violentFlags = violentFlagStream.bufferedReader().use { reader ->
    val csvParser = csvFormat.parse(reader)
    csvParser.map { csvRecord ->
      FlagExtract(
        category = csvRecord.get("CATEGORY"),
        subCategory = csvRecord.get("SUB CAT"),
        flagName = "IS_VIOLENT_SANCTION",
        flagValue = csvRecord.get("IS_VIOLENT_SANCTION"),
      )
    }
  }

  // string builder for start of script
  val weightingUpdateSql = StringBuilder()
  weightingUpdateSql.append("BEGIN;\n\n")

  opdFlags.forEach { flag -> insertFlag(flag, weightingUpdateSql) }
  violentFlags.forEach { flag -> insertFlag(flag, weightingUpdateSql) }

  // end of script
  weightingUpdateSql.append("COMMIT;\n")
  outputFile.writeText(weightingUpdateSql.toString())
  println("Success: Generated migration")
}

fun insertFlag(flag: FlagExtract, weightingUpdateSql: StringBuilder) {
  weightingUpdateSql.append(
    """
INSERT INTO risk_actuarial_ho_code_flags (risk_actuarial_ho_code_id, flag_name, flag_value, created_date)
VALUES ((SELECT id FROM risk_actuarial_ho_code WHERE category = ${flag.category} AND sub_category = ${flag.subCategory}), '${flag.flagName}', ${flag.flagValue}, CURRENT_TIMESTAMP);
    """.trimIndent(),
  ).append("\n\n")
}

data class FlagExtract(
  val category: String,
  val subCategory: String,
  val flagName: String,
  val flagValue: String,
)
