package uk.gov.justice.digital.hmpps.manageoffencesapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class ManageOffencesApi

fun main(args: Array<String>) {
  runApplication<ManageOffencesApi>(*args)
}
