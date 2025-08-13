package com.hamza.geolite.webflux

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.reset
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.hamza.geolite.Commons
import com.hamza.geolite.GeoIP2Data
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ServerWebExchange
import java.net.InetAddress
import java.net.InetSocketAddress

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@AutoConfigureObservability
class GeoIP2GatewayFilterFactoryTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean("GeoIP2XForwardedResolver")
    private lateinit var resolver: XForwardedRemoteAddressResolver

    private var geoIP2Data =
        GeoIP2Data(
            city = "Minneapolis",
            cityIsoCode = "MN",
            country = "United States",
            countryIsoCode = "US",
        )

    @AfterEach
    fun afterEach() {
        reset()
    }

    // https://docs.spring.io/spring-boot/reference/actuator/tracing.html#actuator.micrometer-tracing.baggage
    @Test
    fun `ReactiveGeoIP2 filter should populate tracing context with lookupCity response`() {
        val inetAddress = mock(InetAddress::class.java)
        whenever(inetAddress.hostAddress)
            .thenReturn("128.101.101.10")
        whenever(resolver.resolve(any(ServerWebExchange::class.java)))
            .thenReturn(InetSocketAddress(inetAddress, 0))

        stubFor(
            get(urlEqualTo("/stub"))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_HTML_VALUE))
                .withHeader("visitor_info", equalTo(Commons.writeJson(geoIP2Data, objectMapper)))
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

    @Disabled
    @Test
    fun mdc() {
        // FIXME: access mdc from reactor and check for visitor_info
    }
}
