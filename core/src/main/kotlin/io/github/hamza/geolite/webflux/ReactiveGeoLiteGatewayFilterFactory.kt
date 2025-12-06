package io.github.hamza.geolite.webflux

import io.github.hamza.geolite.Commons
import io.github.hamza.geolite.Commons.Companion.logAtLevel
import io.github.hamza.geolite.GeoLiteData
import io.github.hamza.geolite.GeoliteSharedConfiguration.GeoliteProperties
import io.github.hamza.geolite.normalize
import io.github.hamza.geolite.toCityDto
import io.github.hamza.geolite.toCountryDto
import io.github.hamza.geolite.toDto
import io.micrometer.tracing.Tracer
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper

class ReactiveGeoLiteGatewayFilterFactory(
    private val geoLiteService: IReactiveGeoLiteService,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer,
    private val resolver: XForwardedRemoteAddressResolver,
    private val properties: GeoliteProperties,
    private val environment: Environment,
) : AbstractGatewayFilterFactory<ReactiveGeoLiteGatewayFilterFactory.Companion.Config>(Config::class.java) {
    companion object {
        data class Config(
            val additionalHeaders: List<String>? = null,
        )
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun apply(config: Config): GatewayFilter =
        GatewayFilter { exchange, chain ->
            val xForwardedFor =
                resolver.resolve(exchange)?.address?.hostAddress
                    ?: exchange.request.remoteAddress
                        ?.address
                        ?.hostAddress
                    ?: "0.0.0.0"

            val path = exchange.request.uri.path
            val query = exchange.request.uri.query

            val headers = exchange.request.headers
            val wantedHeaders = config.additionalHeaders?.map { it.lowercase() }?.toSet()
            val additionalHeaders =
                when {
                    wantedHeaders?.contains("*") == true -> {
                        headers
                            .normalize()
                            .mapKeys { it.key.lowercase() }
                            .filterValues { it.any { v -> v.isNotBlank() } }
                    }

                    wantedHeaders != null -> {
                        headers
                            .normalize()
                            .mapKeys { it.key.lowercase() }
                            .filter { (key, values) ->
                                key in wantedHeaders && values.any { v -> v.isNotBlank() }
                            }
                    }

                    else -> {
                        emptyMap()
                    }
                }

            Mono
                .zip(
                    geoLiteService
                        .city(xForwardedFor),
                    geoLiteService
                        .asn(xForwardedFor),
                ).flatMap {
                    val geoLiteData =
                        GeoLiteData(
                            forwardedFor = xForwardedFor,
                            path = path,
                            query = query,
                            city = it.t1.cityResponse?.toCityDto(),
                            country = it.t1.cityResponse?.toCountryDto(),
                            asn = it.t2.asnResponse?.toDto(),
                            additionalHeaders = additionalHeaders.takeIf { h -> h.isNotEmpty() },
                            botScoreThreshold = properties.botScoreThreshold!!,
                        )

                    val shouldBlock = properties.blockBot == true && geoLiteData.getIsBot()

                    val filteredJson = Commons.excludedFields(geoLiteData, properties, objectMapper)
                    val json = Commons.writeJson(filteredJson, objectMapper)
                    logger.debug("{}: isBot:{}, {}", properties.baggage, geoLiteData.getIsBot(), json) // dev debug
                    logAtLevel(logger, "{}", properties.baggage) // actual write to mdc

                    Commons.withDynamicBaggage(
                        tracer = tracer,
                        key = properties.baggage,
                        value = json,
                        publisher =
                            if (shouldBlock) {
                                /*
                                 * just for tests:
                                 * setComplete short circuits the chain
                                 * this will forwards request to tap for testing
                                 * tap will test baggage and then 429
                                 */
                                if (environment.activeProfiles.contains("tap-test")) {
                                    chain.filter(exchange)
                                } else {
                                    val response = exchange.response
                                    response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                                    response.setComplete()
                                }
                            } else {
                                chain.filter(exchange)
                            },
                    )
                }
        }
}
