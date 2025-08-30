package io.github.hamza.geolite.webflux

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.hamza.geolite.Commons
import io.github.hamza.geolite.Commons.Companion.logAtLevel
import io.github.hamza.geolite.GeoliteSharedConfiguration.GeoliteProperties
import io.github.hamza.geolite.toDto
import io.micrometer.tracing.Tracer
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver
import reactor.core.publisher.Mono

class ReactiveGeoLiteGatewayFilterFactory(
    private val geoLiteService: IReactiveGeoLiteService,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer,
    private val resolver: XForwardedRemoteAddressResolver,
    private val properties: GeoliteProperties,
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
            val wantedHeaders = config.additionalHeaders?.map { it.lowercase() }?.toSet()
            val additionalHeaders =
                exchange.request.headers
                    .mapKeys { it.key.lowercase() }
                    .filter { it.key in (wantedHeaders ?: emptySet()) }
            Mono
                .zip(
                    geoLiteService
                        .city(xForwardedFor),
                    geoLiteService
                        .asn(xForwardedFor),
                ).flatMap {
                    val geoLiteData =
                        it.t1.cityResponse?.toDto(
                            xForwardedFor = xForwardedFor,
                            path = path,
                            asn = it.t2.asnResponse?.toDto(),
                            additionalHeaders = additionalHeaders.let { headers -> headers.ifEmpty { null } },
                        )
                    val filteredJson = Commons.excludedFields(geoLiteData, properties, objectMapper)
                    val json = Commons.writeJson(filteredJson, objectMapper)
                    logger.debug("x-forwarded-for: {}", xForwardedFor) // dev debug
                    logger.debug("baggage {}: {}", properties.baggage, json) // dev debug
                    logAtLevel(logger, "{}", properties.baggage) // actual write to mdc
                    Commons.withDynamicBaggage(
                        tracer = tracer,
                        key = properties.baggage,
                        value = json,
                        publisher = chain.filter(exchange),
                    )
                }.onErrorResume {
                    logger.error("ReactiveGeoLiteGatewayFilterFactory: {}", it.message)
                    chain.filter(exchange)
                }
        }
}
