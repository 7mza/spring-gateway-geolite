package io.github.hamza.geolite.webflux

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["geolite.db.city=zebi"],
)
@AutoConfigureWebTestClient
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@AutoConfigureTracing
class ReactiveGeoLiteGatewayFilterFactoryErrorTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `GeoLite filter should continue chain if db file is unreachable`() {
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
