package io.github.hamza.geolite.webflux

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.hamza.geolite.Commons
import io.github.hamza.geolite.GeoLiteData
import io.github.hamza.geolite.GeoliteSharedConfiguration
import io.micrometer.tracing.BaggageManager
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean

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
            builder.routes()
                .route("stub") {
                    it.path("/stub")
                        .filters { f ->
                            f.filter(geoLite.apply(ReactiveGeoLiteGatewayFilterFactory.Companion.Config()))
                            f.filter { exchange, chain ->
                                val baggage = baggageManager.getBaggage(properties.baggage)?.get()
                                assertThat(baggage).isNotNull
                                assertThat(Commons.parseJson<GeoLiteData>(baggage!!, objectMapper)).isEqualTo(geoLiteData)
                                chain.filter(exchange)
                            }
                        }.uri("http://localhost:$port")
                }.build()
    }
