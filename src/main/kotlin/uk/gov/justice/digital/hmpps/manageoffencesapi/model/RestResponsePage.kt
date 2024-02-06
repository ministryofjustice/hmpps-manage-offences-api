@file:Suppress("ktlint")

package uk.gov.justice.digital.hmpps.manageoffencesapi.model

import com.fasterxml.jackson.databind.JsonNode

data class RestResponsePage<T>(
  val content: List<T>,
  val number: Int,
  val size: Int,
  val totalElements: Long?,
  val pageable: JsonNode,
  val last: Boolean,
  val totalPages: Int,
  val sort: JsonNode,
  val first: Boolean,
  val numberOfElements: Int,
)
