package io.github.hamza.geolite.webflux

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.reset
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.github.hamza.geolite.AsnData
import io.github.hamza.geolite.CityData
import io.github.hamza.geolite.Commons
import io.github.hamza.geolite.CountryData
import io.github.hamza.geolite.GeoLiteData
import io.github.hamza.geolite.GeoliteSharedConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ServerWebExchange
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import tools.jackson.databind.ObjectMapper
import java.net.InetAddress
import java.net.InetSocketAddress

val geoLiteData =
    GeoLiteData(
        forwardedFor = "128.101.101.101",
        path = "/stub",
        query = null,
        city =
            CityData(
                name = "Minneapolis",
                isoCode = "MN",
                latitude = 44.9696,
                longitude = -93.2348,
            ),
        country =
            CountryData(
                name = "United States",
                isoCode = "US",
            ),
        asn =
            AsnData(
                autonomousSystemNumber = 217,
                autonomousSystemOrganization = "University of Minnesota",
                ipAddress = "128.101.101.101",
                hostAddress = "128.101.0.0",
                prefixLength = 16,
            ),
        botScoreThreshold = 12,
    )

fun GeoLiteData.withoutCoordinates(): GeoLiteData =
    this.copy(
        city =
            this.city?.copy(
                latitude = null,
                longitude = null,
            ),
    )

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@AutoConfigureTracing
class ReactiveGeoLiteGatewayFilterFactoryTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var properties: GeoliteSharedConfiguration.GeoliteProperties

    @MockitoBean("GeoLiteForwardedResolver")
    private lateinit var resolver: XForwardedRemoteAddressResolver

    @Value($$"${test.header}")
    private lateinit var testHeader: String

    @AfterEach
    fun afterEach() {
        reset()
    }

    // https://docs.spring.io/spring-boot/reference/actuator/tracing.html#actuator.micrometer-tracing.baggage
    @Test
    @DisplayName("GeoLite filter should propagate 'management.tracing.baggage.remote-fields' as headers")
    fun `GeoLite filter propagate headers`() {
        val inetAddress = mock(InetAddress::class.java)
        whenever(inetAddress.hostAddress)
            .thenReturn("128.101.101.101")
        whenever(resolver.resolve(any(ServerWebExchange::class.java)))
            .thenReturn(InetSocketAddress(inetAddress, 0))

        stubFor(
            get(urlEqualTo("/stub"))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_HTML_VALUE))
                .withHeader(
                    properties.baggage,
                    equalTo(Commons.writeJson(geoLiteData, objectMapper)),
                ) // FIXME: lat/long are not fix
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

    @Test
    fun `GeoLite filter should propagate additional headers if configured`() {
        val inetAddress = mock(InetAddress::class.java)
        whenever(inetAddress.hostAddress)
            .thenReturn("128.101.101.101")
        whenever(resolver.resolve(any(ServerWebExchange::class.java)))
            .thenReturn(InetSocketAddress(inetAddress, 0))

        stubFor(
            get(urlPathEqualTo("/stub2"))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_HTML_VALUE))
                .withHeader(
                    properties.baggage,
                    equalTo(
                        Commons.writeJson(
                            geoLiteData.copy(
                                forwardedFor = "128.101.101.101",
                                path = "/stub2",
                                query = "toto=true&tata=123",
                                additionalHeaders =
                                    mapOf(
                                        Pair(
                                            HttpHeaders.USER_AGENT.lowercase(),
                                            listOf(testHeader),
                                        ),
                                    ),
                            ),
                            objectMapper,
                        ),
                    ),
                ).willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                        .withBody("hello world"),
                ),
        )

        val response =
            webTestClient
                .get()
                .uri("/stub2?toto=true&tata=123")
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
