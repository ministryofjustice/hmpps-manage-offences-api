package uk.gov.justice.digital.hmpps.manageoffencesapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.stubbing.StubMapping

class PrisonApiMockServer : WireMockServer(WIREMOCK_PORT) {

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status)
      )
    )
  }

  fun stubCreateHomeOfficeCode(): StubMapping =
    stubFor(
      WireMock.post("/api/offences/ho-code")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
        )
    )

  fun stubCreateStatute(): StubMapping =
    stubFor(
      WireMock.post("/api/offences/statute")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
        )
    )

  fun stubCreateHomeOfficeCodeWhenOneAlreadyExists(): StubMapping =
    stubFor(
      WireMock.post("/api/offences/ho-code")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(409)
        )
    )
  fun stubCreateStatuteWhenOneAlreadyExists(): StubMapping =
    stubFor(
      WireMock.post("/api/offences/statute")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(409)
        )
    )

  fun stubCreateOffence(): StubMapping =
    stubFor(
      WireMock.post("/api/offences/offence")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
        )
    )

  fun stubUpdateOffence(): StubMapping =
    stubFor(
      WireMock.put("/api/offences/offence")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
        )
    )

  fun stubFindByOffenceCodeStartsWithReturnsNothing(offenceCode: Char) {
    stubFor(
      get("/api/offences/code/$offenceCode?page=0&size=1000&sort=code,ASC").willReturn(
        aResponse()
          .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
          .withBody(
            emptyNomisOffenceResponse
          )
      )
    )
  }

  fun stubFindByOffenceCodeStartsWith(offenceCode: Char) {
    stubFor(
      get("/api/offences/code/$offenceCode?page=0&size=1000&sort=code,ASC").willReturn(
        aResponse()
          .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
          .withBody(
            nomisOffences
          )
      )
    )
  }

  companion object {
    private const val WIREMOCK_PORT = 8333
    private val nomisOffences = """ {
                  "content": [
                    {
                      "code": "M1119999",
                      "description": "Actual bodily harm",
                      "statuteCode": {
                        "code": "M111",
                        "description": "Statute M111",
                        "legislatingBodyCode": "UK",
                        "activeFlag": "Y"
                      },
                      "hoCode": {
                        "code": "815/90",
                        "description": "Ho Code 815/90",
                        "activeFlag": "Y"
                      },
                      "severityRanking": "500",
                      "activeFlag": "Y"
                    },
                    {
                      "code": "M2119999",
                      "description": "Common assault",
                      "statuteCode": {
                        "code": "M111",
                        "description": "Statute M111",
                        "legislatingBodyCode": "UK",
                        "activeFlag": "Y"
                      },
                      "hoCode": {
                        "code": "815/90",
                        "description": "Ho Code 815/90",
                        "activeFlag": "Y"
                      },
                      "severityRanking": "400",
                      "activeFlag": "Y"
                    },
                    {
                      "code": "M3119999",
                      "description": "Attempted Murder",
                      "statuteCode": {
                        "code": "M111",
                        "description": "Statute M111",
                        "legislatingBodyCode": "UK",
                        "activeFlag": "Y"
                      },
                      "hoCode": {
                        "code": "815/90",
                        "description": "Ho Code 815/90",
                        "activeFlag": "Y"
                      },
                      "severityRanking": "600",
                      "activeFlag": "Y"
                    },
                    {
                      "code": "M4119999",
                      "description": "Manslaughter",
                      "statuteCode": {
                        "code": "M111",
                        "description": "Statute M111",
                        "legislatingBodyCode": "UK",
                        "activeFlag": "Y"
                      },
                      "hoCode": {
                        "code": "815/90",
                        "description": "Ho Code 815/90",
                        "activeFlag": "Y"
                      },
                      "severityRanking": "700",
                      "activeFlag": "Y"
                    },
                    {
                      "code": "M5119999",
                      "description": "Manslaughter Old",
                      "statuteCode": {
                        "code": "M111",
                        "description": "Statute M111",
                        "legislatingBodyCode": "UK",
                        "activeFlag": "Y"
                      },
                      "hoCode": {
                        "code": "815/90",
                        "description": "Ho Code 815/90",
                        "activeFlag": "Y"
                      },
                      "severityRanking": "700",
                      "activeFlag": "N"
                    }
                  ],
                  "pageable": {
                    "sort": {
                      "unsorted": false,
                      "sorted": true,
                      "empty": false
                    },
                    "offset": 0,
                    "pageNumber": 0,
                    "pageSize": 20,
                    "paged": true,
                    "unpaged": false
                  },
                  "last": true,
                  "totalElements": 5,
                  "totalPages": 1,
                  "size": 20,
                  "number": 0,
                  "sort": {
                    "unsorted": false,
                    "sorted": true,
                    "empty": false
                  },
                  "first": true,
                  "numberOfElements": 5,
                  "empty": false
                }
    """.trimIndent()

    private val emptyNomisOffenceResponse = """ {
                  "content": [],
                  "pageable": {
                    "sort": {
                      "unsorted": false,
                      "sorted": true,
                      "empty": false
                    },
                    "offset": 0,
                    "pageNumber": 0,
                    "pageSize": 20,
                    "paged": true,
                    "unpaged": false
                  },
                  "last": true,
                  "totalElements": 0,
                  "totalPages": 1,
                  "size": 20,
                  "number": 0,
                  "sort": {
                    "unsorted": false,
                    "sorted": true,
                    "empty": false
                  },
                  "first": true,
                  "numberOfElements": 0,
                  "empty": false
                }
    """.trimIndent()
  }
}
