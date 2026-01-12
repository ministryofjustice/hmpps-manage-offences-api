package uk.gov.justice.digital.hmpps.manageoffencesapi.flywaygenerator

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.sql.DriverManager.getConnection
import java.time.LocalDateTime
import java.util.Properties
import kotlin.text.toDoubleOrNull

// todo THIS GENERATOR IS A WIP

// Path to CSV output of RSR_COEFFICIENTS
const val RSR_COEFFICIENTS_PATH = "Path to RSR coefficients CSV"

// Path to CSV output of ogrs_description_weight_map
const val OGRS_DESCRIPTION_PATH = "Path to OGRS3 descriptions CSV"

fun main() {
  val migrationDir = File("src/main/resources/migration/common")

  // new flyway script version
  val nextVersion = (migrationDir.listFiles()
    ?.mapNotNull { file ->
      Regex("^V(\\d+)__.*\\.sql$")
        .find(file.name)?.groupValues?.get(1)?.toInt()
    }?.maxOrNull() ?: 0) + 1

  val outputFile = File(migrationDir, "V${nextVersion}__create_new_offence_code_weightings.sql")

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

  val rsrCoefficientsInputStream = File(RSR_COEFFICIENTS_PATH).inputStream()
  val rsrCoefficients = rsrCoefficientsInputStream.bufferedReader().use { reader ->
    val csvParser = CSVParser(reader, csvFormat)
    csvParser.map { csvRecord ->
      RSRCoefficient(
        coefficientName = csvRecord.get("COEFFICIENTS_NAME"),
        snsvStatic = csvRecord.get("SNSV_STATIC").toDoubleOrNull(),
        snsvDynamic = csvRecord.get("SNSV_DYNAMIC").toDoubleOrNull(),
      )
    }
  }

  val ogrs3DescriptionInputStream = File(OGRS_DESCRIPTION_PATH).inputStream()
  val ogrs3Descriptions = ogrs3DescriptionInputStream.bufferedReader().use { reader ->
    val cleanedReader = reader.readText().replace("\uFEFF", "").reader()
    val csvParser = CSVParser(cleanedReader, csvFormat)
    csvParser.map { csvRecord ->
      OgrsDescriptionWeightMap(
        ogrs3description = csvRecord.get("Offence"),
        ogrs3Weighting = csvRecord.get("Parameter").toDoubleOrNull(),
      )
    }
  }

  val offenceCodeMapping = ctOffences.filterNot {
    filterCategory000(it.offenceGroupCode, it.subCode)
  }.map { ctOffence ->
    val ogrs3Description = ogrs3Descriptions.firstOrNull {
      it.ogrs3Weighting == ctOffence.ogrs3Weighting
    }?.ogrs3description ?: "Missing description"
    val ogrs4CategoryDescription =
      if (ctOffence.ogrs4CategoryDesc != "") ctOffence.ogrs4CategoryDesc else "Missing description"
    val rsrDescription = if (ctOffence.rsrCategoryDesc != "") ctOffence.rsrCategoryDesc else "Missing description"

    OffenceCodeMapping(
      offenceGroupCode = StringUtils.leftPad(ctOffence.offenceGroupCode, 3, '0'),
      subCode = StringUtils.leftPad(ctOffence.subCode, 2, '0'),
      ogrs3Weighting = ctOffence.ogrs3Weighting,
      ogrs3Description = ogrs3Description,
      snsvStaticWeighting = rsrCoefficients.firstOrNull { it.coefficientName == ctOffence.ogrs4CategoryDesc }?.snsvStatic,
      snsvStaticDescription = ogrs4CategoryDescription,
      snsvDynamicWeighting = rsrCoefficients.firstOrNull { it.coefficientName == ctOffence.ogrs4CategoryDesc }?.snsvDynamic,
      snsvDynamicDescription = ogrs4CategoryDescription,
      snsvVatpStaticWeighting = lookupVatpWeighting(
        ctOffence,
        rsrCoefficients,
        true,
      ),
      snsvVatpStaticDescription = rsrDescription,
      snsvVatpDynamicWeighting = lookupVatpWeighting(
        ctOffence,
        rsrCoefficients,
        false,
      ),
      snsvVatpDynamicDescription = rsrDescription,
      opdViolSex = ctOffence.opdViolSex,
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

  val offenceCodeUpdateSql = StringBuilder()
  var createCount = 0

  getConnection(url, props).use { connection ->
    val checkStatement = connection.prepareStatement(
      """
        SELECT w.weighting_desc, w.risk_actuarial_ho_code_id     
        FROM risk_actuarial_ho_code_weightings w
        JOIN risk_actuarial_ho_code r ON w.risk_actuarial_ho_code_id = r.id
        WHERE r.category = ?::int
          AND r.sub_category = ?::int
    """.trimIndent(),
    )

    offenceCodeMapping.forEach { mapping ->
      checkStatement.setString(1, mapping.offenceGroupCode)
      checkStatement.setString(2, mapping.subCode)

      val response = checkStatement.executeQuery()

      // response.next() == false ADD NEW OFFENCE CODE
      if (!response.next()) {
        offenceCodeUpdateSql.append(addToFlywayScript(mapping))
        createCount++
      } else {
        // response.next() == true CHECK THE DESCRIPTION
        val hoCodId = response.getInt("risk_actuarial_ho_code_id").toString()
        val currentDbDesc = response.getString("weighting_desc")?.lowercase()
        val csvDesc1 = mapping.ogrs3Description?.lowercase()
        val csvDesc2 = mapping.snsvStaticDescription?.lowercase()
        val csvDesc3 = mapping.snsvDynamicDescription?.lowercase()
        val csvDesc4 = mapping.snsvVatpStaticDescription?.lowercase()
        val csvDesc5 = mapping.snsvVatpDynamicDescription?.lowercase()


        // if not the same, update the offence code description and weight
        if(currentDbDesc != csvDesc1 &&
          currentDbDesc != csvDesc2 &&
          currentDbDesc != csvDesc3 &&
          currentDbDesc != csvDesc4 &&
          currentDbDesc != csvDesc5
        ) {
          // add new mapping to script
          offenceCodeUpdateSql.append(updateToFlywayScript(mapping, hoCodId))
          createCount++
        }
        // if same, do nothing
      }
    }

    if (createCount > 0) {
      outputFile.writeText(offenceCodeUpdateSql.toString())
      println("Success: Generated migration with $createCount updates.")
    } else {
      println("No differences found. Migration file not created.")
    }
  }
}

private fun addToFlywayScript(mapping: OffenceCodeMapping): String {
  val createdDate = LocalDateTime.now()
  val flagValue = when (mapping.opdViolSex) {
    "Y" -> "TRUE"
    "N" -> "FALSE"
    else -> "NULL"
  }

  val weightings = listOf(
    "ogrs3Weighting" to (mapping.ogrs3Weighting to mapping.ogrs3Description),
    "snsvStaticWeighting" to (mapping.snsvStaticWeighting to mapping.snsvStaticDescription),
    "snsvDynamicWeighting" to (mapping.snsvDynamicWeighting to mapping.snsvDynamicDescription),
    "snsvVatpStaticWeighting" to (mapping.snsvVatpStaticWeighting to mapping.snsvVatpStaticDescription),
    "snsvVatpDynamicWeighting" to (mapping.snsvVatpDynamicWeighting to mapping.snsvVatpDynamicDescription),
  )

  val weightingsUnionAll = weightings.joinToString("\nUNION ALL\n") { (name, pair) ->
    val (value, desc) = pair
    val descSql = desc?.replace("'", "''")?.let { "'$it'" } ?: "NULL"
    val is999 = value == 999.0
    val errorCode = if (name == "ogrs3Weighting" && is999) "'NEED_DETAILS_OF_EXACT_OFFENCE'" else "NULL"
    val valueSql = handle999Value(value)
    "SELECT id, '$name', $valueSql, $descSql, $errorCode FROM inserted"
  }

  val stringToAdd =
    """
    WITH inserted AS (
        INSERT INTO risk_actuarial_ho_code (category, sub_category, created_date)
        VALUES ('${mapping.offenceGroupCode}', '${mapping.subCode}', '$createdDate')
        RETURNING id
    )
    INSERT INTO risk_actuarial_ho_code_weightings
    (risk_actuarial_ho_code_id, weighting_name, weighting_value, weighting_desc, error_code)
    $weightingsUnionAll;

    WITH inserted AS (
        SELECT id FROM risk_actuarial_ho_code
        WHERE category = '${mapping.offenceGroupCode}' AND sub_category = '${mapping.subCode}'
    )
    INSERT INTO risk_actuarial_ho_code_flags
    (risk_actuarial_ho_code_id, flag_name, flag_value, created_date)
    SELECT id, 'opdViolSex', $flagValue, '$createdDate' FROM inserted;
    """.trimIndent()

  return stringToAdd
}

private fun updateToFlywayScript(mapping: OffenceCodeMapping, id: String): String {
  val createdDate = LocalDateTime.now()
  val flagValue = when (mapping.opdViolSex) {
    "Y" -> "TRUE"
    "N" -> "FALSE"
    else -> "NULL"
  }

  val weightings = listOf(
    "ogrs3Weighting" to (mapping.ogrs3Weighting to mapping.ogrs3Description),
    "snsvStaticWeighting" to (mapping.snsvStaticWeighting to mapping.snsvStaticDescription),
    "snsvDynamicWeighting" to (mapping.snsvDynamicWeighting to mapping.snsvDynamicDescription),
    "snsvVatpStaticWeighting" to (mapping.snsvVatpStaticWeighting to mapping.snsvVatpStaticDescription),
    "snsvVatpDynamicWeighting" to (mapping.snsvVatpDynamicWeighting to mapping.snsvVatpDynamicDescription),
  )

  val weightingsUnionAll = weightings.joinToString("\nUNION ALL\n") { (name, pair) ->
    val (value, desc) = pair
    val descSql = desc?.replace("'", "''")?.let { "'$it'" } ?: "NULL"
    val is999 = value == 999.0
    val errorCode = if (name == "ogrs3Weighting" && is999) "'NEED_DETAILS_OF_EXACT_OFFENCE'" else "NULL"
    val valueSql = handle999Value(value)
    "SELECT $id, '$name', $valueSql, $descSql, $errorCode"
  }

  // todo fix this update sql
  val stringToAdd =
    """
    DELETE FROM risk_actuarial_ho_code_weighting AS w
    WHERE w.risk_actuarial_ho_code_id = $id
    DELETE FROM risk_actuarial_ho_code_flags AS f
    WHERE f.risk_actuarial_ho_code_id = $id

    INSERT INTO risk_actuarial_ho_code_weightings
    (risk_actuarial_ho_code_id, weighting_name, weighting_value, weighting_desc, error_code)
    $weightingsUnionAll;
    
    INSERT INTO risk_actuarial_ho_code_flags
    (risk_actuarial_ho_code_id, flag_name, flag_value, created_date)
    SELECT $id, 'opdViolSex', $flagValue, '$createdDate' FROM inserted;
    """.trimIndent()

  return stringToAdd
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


data class OgrsDescriptionWeightMap(
  val ogrs3description: String?,
  val ogrs3Weighting: Double?,
)

data class OffenceCodeMapping(
  val offenceGroupCode: String,
  val subCode: String,
  val ogrs3Weighting: Double?,
  val ogrs3Description: String?,
  val snsvStaticWeighting: Double?,
  val snsvStaticDescription: String?,
  val snsvDynamicWeighting: Double?,
  val snsvDynamicDescription: String?,
  val snsvVatpStaticWeighting: Double?,
  val snsvVatpStaticDescription: String?,
  val snsvVatpDynamicWeighting: Double?,
  val snsvVatpDynamicDescription: String?,
  val opdViolSex: String,
)

