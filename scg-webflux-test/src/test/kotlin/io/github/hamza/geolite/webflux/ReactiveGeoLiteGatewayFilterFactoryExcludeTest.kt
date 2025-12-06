package io.github.hamza.geolite.webflux

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.reset
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.github.hamza.geolite.Commons
import io.github.hamza.geolite.GeoliteSharedConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
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

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["geolite.exclude=city.name, country.isoCode, asn.*"],
)
@AutoConfigureWebTestClient
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@AutoConfigureTracing
class ReactiveGeoLiteGatewayFilterFactoryExcludeTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var properties: GeoliteSharedConfiguration.GeoliteProperties

    @MockitoBean("GeoLiteForwardedResolver")
    private lateinit var resolver: XForwardedRemoteAddressResolver

    @AfterEach
    fun afterEach() {
        reset()
    }

    @Test
    @DisplayName("GeoLite filter should exclude configured fields")
    fun `GeoLite filter propagate headers`() {
        val inetAddress = mock(InetAddress::class.java)
        whenever(inetAddress.hostAddress)
            .thenReturn("128.101.101.101")
        whenever(resolver.resolve(any(ServerWebExchange::class.java)))
            .thenReturn(InetSocketAddress(inetAddress, 0))

        val filteredGeoLiteData = Commons.excludedFields(geoLiteData, properties, objectMapper)

        stubFor(
            get(urlEqualTo("/stub"))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_HTML_VALUE))
                .withHeader(
                    properties.baggage,
                    equalTo(Commons.writeJson(filteredGeoLiteData, objectMapper)),
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
}
