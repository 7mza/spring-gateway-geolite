package io.github.hamza.geolite.webflux

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ServerWebExchange
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.net.InetAddress
import java.net.InetSocketAddress

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@AutoConfigureTracing
@Import(FilterTapTestConfiguration::class)
class ReactiveGeoLiteGatewayFilterFactoryAllHeadersTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean("GeoLiteForwardedResolver")
    private lateinit var resolver: XForwardedRemoteAddressResolver

    @Test
    @DisplayName("GeoLite filter should collect all headers if configured with *")
    fun `GeoLite filter collect all logs`() {
        val inetAddress = mock(InetAddress::class.java)
        whenever(inetAddress.hostAddress)
            .thenReturn("0.0.0.0")
        whenever(resolver.resolve(any(ServerWebExchange::class.java)))
            .thenReturn(InetSocketAddress(inetAddress, 0))

        stubFor(
            get(urlEqualTo("/stub4"))
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
                .uri("/stub4")
                .accept(MediaType.TEXT_HTML)
                .header("toto", "tata")
                .header("titi", "a1", "a2")
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
