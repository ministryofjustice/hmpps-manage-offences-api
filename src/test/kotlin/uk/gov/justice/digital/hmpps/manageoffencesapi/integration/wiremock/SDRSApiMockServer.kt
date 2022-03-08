package uk.gov.justice.digital.hmpps.manageoffencesapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders

class SDRSApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8332
  }

  fun stubGetAllOffences() {
    //    TODO Replace URL with correct one when known
    stubFor(
      get("/todo")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """ {
                    "MessageBody": {
                      "GatewayOperationType": {
                        "GetOffenceResponse": {
                          "Offence": [
                            {
                              "OffenceRevisionId": 410082,
                              "OffenceStartDate": "2013-03-01",
						                  "OffenceEndDate": "2013-03-02",
                              "code": "XX99001"
                            },
                            {
                              "OffenceRevisionId": 354116,
						                  "OffenceStartDate": "2005-09-02",
						                  "OffenceEndDate": "2005-09-03",
                              "code": "XX99001"
                            }
                          ]
                        }
		                  }
	                  },
                    "MessageHeader": {
                      "MessageID": {
                        "UUID": "7717d82c-9cc2-4983-acf1-0d42770e88bd",
                        "RelatesTo": "df2200e6-241c-4642-b391-3d53299185cd"
                      },
                      "TimeStamp": "2022-03-01T15:00:00Z",
                      "MessageType": "getOffence",
                      "From": "SDRS_AZURE",
                      "To": "CONSUMER_APPLICATION"
                    },
                    "MessageStatus": {
                      "status": "SUCCESS",
                      "code": " ",
                      "reason": " ",
                      "detail": " "
                    }
                  }
              """.trimIndent()
            )
        )
    )
  }
}
