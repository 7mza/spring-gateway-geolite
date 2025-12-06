package io.github.hamza.geolite.webflux

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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ServerWebExchange
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.net.InetAddress
import java.net.InetSocketAddress

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureTracing
@Import(FilterTapTestConfiguration::class)
@ActiveProfiles("default", "tap-test")
class ReactiveGeoLiteGatewayFilterFactoryBlockBotsTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean("GeoLiteForwardedResolver")
    private lateinit var resolver: XForwardedRemoteAddressResolver

    @Test
    @DisplayName("GeoLite filter should 429 request if bot is detected")
    fun `GeoLite filter 429 bots`() {
        val inetAddress = mock(InetAddress::class.java)
        whenever(inetAddress.hostAddress)
            .thenReturn("0.0.0.0")
        whenever(resolver.resolve(any(ServerWebExchange::class.java)))
            .thenReturn(InetSocketAddress(inetAddress, 0))

        webTestClient
            .get()
            .uri("/stub3")
            .accept(MediaType.TEXT_HTML)
            .header(HttpHeaders.USER_AGENT, "")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }
}

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["geolite.blockBot=true"],
)
@AutoConfigureWebTestClient
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@AutoConfigureTracing
@Import(FilterTapTestConfiguration::class)
class ReactiveGeoLiteGatewayFilterFactoryBlockBotsOnConfTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean("GeoLiteForwardedResolver")
    private lateinit var resolver: XForwardedRemoteAddressResolver

    @Test
    @DisplayName("GeoLite filter should block bots if its configured")
    fun `GeoLite filter block on conf`() {
        val inetAddress = mock(InetAddress::class.java)
        whenever(inetAddress.hostAddress)
            .thenReturn("0.0.0.0")
        whenever(resolver.resolve(any(ServerWebExchange::class.java)))
            .thenReturn(InetSocketAddress(inetAddress, 0))

        webTestClient
            .get()
            .uri("/stub3")
            .accept(MediaType.TEXT_HTML)
            .header(HttpHeaders.USER_AGENT, "")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }
}
