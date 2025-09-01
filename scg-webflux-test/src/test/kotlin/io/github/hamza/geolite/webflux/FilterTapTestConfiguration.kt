package io.github.hamza.geolite.webflux

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.hamza.geolite.Commons
import io.github.hamza.geolite.GeoLiteData
import io.github.hamza.geolite.GeoliteSharedConfiguration
import io.micrometer.tracing.BaggageManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@TestConfiguration
class FilterTapTestConfiguration
    @Autowired
    constructor(
        private val geoLite: AbstractGatewayFilterFactory<ReactiveGeoLiteGatewayFilterFactory.Companion.Config>,
        private val baggageManager: BaggageManager,
        private val objectMapper: ObjectMapper,
        private val properties: GeoliteSharedConfiguration.GeoliteProperties,
        @param:Value("\${wiremock.server.port}") private val port: Int,
    ) {
        @Bean
        fun routes(builder: RouteLocatorBuilder): RouteLocator =
            builder
                .routes()
                .route("stub") {
                    it
                        .path("/stub")
                        .filters { f ->
                            f
                                .filter(geoLite.apply(ReactiveGeoLiteGatewayFilterFactory.Companion.Config()))
                                .filter { exchange, chain ->
                                    val baggage = baggageManager.getBaggage(properties.baggage)?.get()
                                    assertThat(baggage).isNotNull

                                    val parsed = Commons.parseJson<GeoLiteData>(baggage!!, objectMapper)
                                    assertThat(parsed.withoutCoordinates()).isEqualTo(geoLiteData.withoutCoordinates())

                                    assertThat(parsed.city?.latitude)
                                        .isCloseTo(geoLiteData.city?.latitude, within(0.1))
                                    assertThat(parsed.city?.longitude)
                                        .isCloseTo(geoLiteData.city?.longitude, within(0.1))

                                    assertThat(parsed.getIsBot()).isFalse
                                    assertThat(parsed.getBotScore()).isLessThan(properties.botScoreThreshold)

                                    chain.filter(exchange)
                                }
                        }.uri("http://localhost:$port")
                }.route("stub2") {
                    it
                        .path("/stub2")
                        .filters { f ->
                            f
                                .filter(
                                    geoLite.apply(
                                        ReactiveGeoLiteGatewayFilterFactory.Companion.Config(
                                            additionalHeaders = listOf(HttpHeaders.USER_AGENT.lowercase()),
                                        ),
                                    ),
                                ).filter { exchange, chain ->
                                    val baggage = baggageManager.getBaggage(properties.baggage)?.get()
                                    assertThat(baggage).isNotNull

                                    val parsed = Commons.parseJson<GeoLiteData>(baggage!!, objectMapper)
                                    assertThat(parsed.withoutCoordinates())
                                        .isEqualTo(
                                            geoLiteData
                                                .withoutCoordinates()
                                                .copy(
                                                    path = "/stub2",
                                                    query = "toto=true&tata=123",
                                                    additionalHeaders =
                                                        mapOf(
                                                            Pair(
                                                                HttpHeaders.USER_AGENT.lowercase(),
                                                                listOf("ReactorNetty/1.2.9"),
                                                            ),
                                                        ),
                                                ),
                                        )

                                    assertThat(parsed.city?.latitude)
                                        .isCloseTo(geoLiteData.city?.latitude, within(0.1))
                                    assertThat(parsed.city?.longitude)
                                        .isCloseTo(geoLiteData.city?.longitude, within(0.1))

                                    assertThat(parsed.getIsBot()).isFalse
                                    assertThat(parsed.getBotScore()).isLessThan(properties.botScoreThreshold)

                                    chain.filter(exchange)
                                }
                        }.uri("http://localhost:$port")
                }.route("stub3") {
                    it
                        .path("/stub3")
                        .filters { f ->
                            f
                                .filter(
                                    geoLite.apply(
                                        ReactiveGeoLiteGatewayFilterFactory.Companion.Config(
                                            additionalHeaders = listOf(HttpHeaders.USER_AGENT.lowercase()),
                                        ),
                                    ),
                                ).filter { exchange, _ ->
                                    val baggage = baggageManager.getBaggage(properties.baggage)?.get()
                                    assertThat(baggage).isNotNull

                                    val parsed = Commons.parseJson<GeoLiteData>(baggage!!, objectMapper)
                                    assertThat(parsed).isEqualTo(
                                        GeoLiteData(
                                            forwardedFor = "0.0.0.0",
                                            path = "/stub3",
                                            botScoreThreshold = properties.botScoreThreshold,
                                        ),
                                    )
                                    assertThat(parsed.getIsBot()).isTrue
                                    assertThat(parsed.getBotScore()).isGreaterThanOrEqualTo(properties.botScoreThreshold)

                                    val response = exchange.response
                                    response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                                    response.setComplete()
                                }
                        }.uri("http://localhost:$port")
                }.route("stub4") {
                    it
                        .path("/stub4")
                        .filters { f ->
                            f
                                .filter(
                                    geoLite.apply(
                                        ReactiveGeoLiteGatewayFilterFactory.Companion.Config(
                                            additionalHeaders = listOf("*"),
                                        ),
                                    ),
                                ).filter { exchange, chain ->
                                    val baggage = baggageManager.getBaggage(properties.baggage)?.get()
                                    assertThat(baggage).isNotNull

                                    val parsed = Commons.parseJson<GeoLiteData>(baggage!!, objectMapper)

                                    assertThat(parsed.additionalHeaders).isNotNull
                                    val headers = parsed.additionalHeaders!!
                                    assertThat(headers.size).isEqualTo(7)
                                    assertThat(headers[HttpHeaders.ACCEPT_ENCODING.lowercase()])
                                        .isNotNull()
                                        .contains("gzip")
                                    assertThat(headers[HttpHeaders.USER_AGENT.lowercase()])
                                        .isNotNull()
                                        .contains("ReactorNetty/1.2.9")
                                    assertThat(headers[HttpHeaders.HOST.lowercase()])
                                        .isNotNull()
                                        .anyMatch { l -> l.contains("localhost") }
                                    assertThat(headers["webtestclient-request-id"]).isNotNull().contains("1")
                                    assertThat(headers[HttpHeaders.ACCEPT.lowercase()]).isNotNull().contains(MediaType.TEXT_HTML_VALUE)
                                    assertThat(headers["toto"]).isNotNull().contains("tata")
                                    assertThat(headers["titi"]).isNotNull().contains("a1", "a2")

                                    chain.filter(exchange)
                                }
                        }.uri("http://localhost:$port")
                }.build()
    }
