package com.hamza.geolite.webflux

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["geoip2.db.city=zebi"],
)
@AutoConfigureWireMock(port = 0)
@AutoConfigureObservability
class GeoIP2GatewayErrorFilterFactoryTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `ReactiveGeoIP2 filter should continue chain if db file is unreachable`() {
        stubFor(
            get(urlEqualTo("/stub"))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_HTML_VALUE))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody("hello world"),
                ),
        )

        val response =
            webTestClient
                .get()
                .uri("/stub")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String::class.java)
                .returnResult()
                .responseBody

        assertThat(response).isEqualTo("hello world")
    }
}
