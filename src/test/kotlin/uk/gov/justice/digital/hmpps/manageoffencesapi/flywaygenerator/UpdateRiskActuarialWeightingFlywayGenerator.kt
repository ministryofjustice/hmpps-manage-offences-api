package uk.gov.justice.digital.hmpps.manageoffencesapi.flywaygenerator

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.sql.DriverManager.getConnection
import java.util.Properties
import kotlin.text.toDoubleOrNull

// Path to CT offences extract
const val CT_OFFENCES_PATH = "path to CT offences CSV"

// Path to RSR coefficients extract
const val RSR_COEFFICIENTS_UPDATE_PATH = "Path to RSR coefficients CSV"

fun main() {
  val migrationDir = File("src/main/resources/migration/common")

  // new flyway script version
  val nextVersion = (migrationDir.listFiles()
    ?.mapNotNull { file ->
      Regex("^V(\\d+)__.*\\.sql$")
        .find(file.name)?.groupValues?.get(1)?.toInt()
    }?.maxOrNull() ?: 0) + 1

  val outputFile = File(migrationDir, "V${nextVersion}__update_risk_actuarial_weightings.sql")

  // CSV readers and mapping
  val ctOffenceInputStream = File(CT_OFFENCES_PATH).inputStream()

  val csvFormat = CSVFormat.DEFAULT.builder()
    .setHeader()
    .setSkipHeaderRecord(true)
    .setTrim(true)
    .build()

  val ctOffences = ctOffenceInputStream.bufferedReader().use { reader ->
    val csvParser = CSVParser(reader, csvFormat)
    csvParser.map { csvRecord ->
      CTOffence(
        offenceGroupCode = csvRecord.get("OFFENCE_GROUP_CODE"),
        subCode = csvRecord.get("SUB_CODE"),
        ogrs3Weighting = csvRecord.get("OGRS3_WEIGHTING").toDoubleOrNull(),
        ogrs4CategoryDesc = csvRecord.get("OGRS4_CATEGORY_DESC"),
        rsrCategoryDesc = csvRecord.get("RSR_CATEGORY_DESC"),
        opdViolSex = csvRecord.get("OPD_VIOL_SEX"),
      )
    }
  }

  val rsrCoefficientsUpdateInputStream = File(RSR_COEFFICIENTS_UPDATE_PATH).inputStream()
  val rsrUpdateCoefficients = rsrCoefficientsUpdateInputStream.bufferedReader().use { reader ->
    val csvParser = CSVParser(reader, csvFormat)
    csvParser.map { csvRecord ->
      RSRCoefficient(
        coefficientName = csvRecord.get("COEFFICIENTS_NAME"),
        snsvStatic = csvRecord.get("SNSV_STATIC").toDoubleOrNull(),
        snsvDynamic = csvRecord.get("SNSV_DYNAMIC").toDoubleOrNull(),
      )
    }
  }

  val offenceCodeMappingUpdate = ctOffences.filterNot {
    filterCategory000(it.offenceGroupCode, it.subCode)
  }.map { ctOffence ->
    WeightingMapping(
      description = ctOffence.rsrCategoryDesc,
      offenceGroupCode = StringUtils.leftPad(ctOffence.offenceGroupCode, 3, '0'),
      subCode = StringUtils.leftPad(ctOffence.subCode, 2, '0'),
      ogrs3Weighting = ctOffence.ogrs3Weighting,
      snsvStaticWeighting = rsrUpdateCoefficients.firstOrNull { it.coefficientName == ctOffence.ogrs4CategoryDesc }?.snsvStatic,
      snsvDynamicWeighting = rsrUpdateCoefficients.firstOrNull { it.coefficientName == ctOffence.ogrs4CategoryDesc }?.snsvDynamic,
      snsvVatpStaticWeighting = lookupVatpWeighting(
        ctOffence,
        rsrUpdateCoefficients,
        true,
      ),
      snsvVatpDynamicWeighting = lookupVatpWeighting(
        ctOffence,
        rsrUpdateCoefficients,
        false,
      ),
    )
  }

  // setup db connection via .env file
  val envMap = File(".env").readLines()
    .filter { it.contains("=") && !it.startsWith("#") }
    .associate {
      val (key, value) = it.split("=", limit = 2)
      key.trim() to value.trim()
    }
  val url = envMap["DB_URL"]
  val props = Properties().apply {
    put("user", envMap["DB_USER"])
    put("password", envMap["DB_PASSWORD"])
  }

  // string builder for start of script
  val weightingUpdateSql = StringBuilder()
  weightingUpdateSql.append("BEGIN;\n\n")

  var updateCount = 0

  getConnection(url, props).use { connection ->
    val checkStatement = connection.prepareStatement(
      """
        SELECT w.weighting_value
        FROM risk_actuarial_ho_code_weightings w
        JOIN risk_actuarial_ho_code r ON w.risk_actuarial_ho_code_id = r.id
        WHERE r.category = ?::int
          AND r.sub_category = ?::int
          AND w.weighting_name = ?
    """.trimIndent(),
    )

    // mapping update from csv's
    offenceCodeMappingUpdate.forEach { mapping ->
      val weightings = listOf(
        WeightingUpdate(
          mapping.offenceGroupCode,
          mapping.subCode,
          "ogrs3Weighting",
          handle999Value(mapping.ogrs3Weighting),
        ),
        WeightingUpdate(
            mapping.offenceGroupCode,
            mapping.subCode,
            "snsvStaticWeighting",
            mapping.snsvStaticWeighting,
        ),
        WeightingUpdate(
          mapping.offenceGroupCode,
          mapping.subCode,
          "snsvDynamicWeighting",
          mapping.snsvDynamicWeighting,
        ),
        WeightingUpdate(
          mapping.offenceGroupCode,
          mapping.subCode,
          "snsvVatpStaticWeighting",
          mapping.snsvVatpStaticWeighting,
        ),
        WeightingUpdate(
          mapping.offenceGroupCode,
          mapping.subCode,
          "snsvVatpDynamicWeighting",
          mapping.snsvVatpDynamicWeighting,
        ),
      )

      weightings.forEach { weighting ->
        checkStatement.setString(1, weighting.category)
        checkStatement.setString(2, weighting.subCategory)
        checkStatement.setString(3, weighting.name)

        // db response
        val response = checkStatement.executeQuery()
        if (response.next()) {
          val currentDbValue = response.getDouble("weighting_value")
          val newValue = weighting.value ?: 0.0

          // new vs db value comparison
          if (java.lang.Double.compare(currentDbValue, newValue) != 0) {
            weightingUpdateSql.append(
              """
                        UPDATE risk_actuarial_ho_code_weightings AS w
                        SET weighting_value = ${weighting.value}
                        FROM risk_actuarial_ho_code AS r
                        WHERE w.risk_actuarial_ho_code_id = r.id
                          AND r.category = '${escape(weighting.category)}'
                          AND r.sub_category = '${escape(weighting.subCategory)}'
                          AND w.weighting_name = '${escape(weighting.name)}';
                    """.trimIndent(),
            ).append("\n\n")

            updateCount++
          }
        }
      }
    }
  }

  // end of script
  weightingUpdateSql.append("COMMIT;\n")

  // write if changes
  if (updateCount > 0) {
    outputFile.writeText(weightingUpdateSql.toString())
    println("Success: Generated migration with $updateCount updates.")
  } else {
    println("No differences found. Migration file not created.")
  }
}

private fun escape(value: String): String = value.replace("'", "''")

private fun handle999Value(weighting: Double?): Double? {
  val is999 = weighting == 999.0
  return when {
    weighting == null -> null
    is999 -> null
    else -> weighting
  }
}

private fun filterCategory000(offenceGroupCode: String?, subCode: String?): Boolean {
  val groupCode = StringUtils.leftPad(offenceGroupCode, 3, '0')
  val sub = StringUtils.leftPad(subCode, 2, '0')
  return groupCode == "000" && (sub == "00" || sub == "01")
}

internal fun lookupVatpWeighting(ctOffence: CTOffence, rsrCoefficients: List<RSRCoefficient>, static: Boolean) =
  if (ctOffence.ogrs4CategoryDesc.trim() == "") {
    null
  } else if (ctOffence.ogrs4CategoryDesc == "Violence against the person") {
    val rsrCategory = rsrCoefficients.firstOrNull { it.coefficientName == ctOffence.rsrCategoryDesc }
    if (static) {
      rsrCategory?.snsvStatic
    } else {
      rsrCategory?.snsvDynamic
    }
  } else {
    0.0
  }

data class WeightingMapping(
  val description: String,
  val offenceGroupCode: String,
  val subCode: String,
  val ogrs3Weighting: Double?,
  val snsvStaticWeighting: Double?,
  val snsvDynamicWeighting: Double?,
  val snsvVatpStaticWeighting: Double?,
  val snsvVatpDynamicWeighting: Double?,
)

data class WeightingUpdate(
  val category: String,
  val subCategory: String,
  val name: String,
  val value: Double?,
)

data class CTOffence(
  val offenceGroupCode: String,
  val subCode: String,
  val ogrs3Weighting: Double?,
  val ogrs4CategoryDesc: String,
  val rsrCategoryDesc: String,
  val opdViolSex: String,
)

data class RSRCoefficient(
  val coefficientName: String,
  val snsvStatic: Double?,
  val snsvDynamic: Double?,
)
