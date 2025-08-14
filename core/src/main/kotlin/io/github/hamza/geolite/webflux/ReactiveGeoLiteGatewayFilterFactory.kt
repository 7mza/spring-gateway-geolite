package io.github.hamza.geolite.webflux

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.hamza.geolite.Commons
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
    private val baggage: String,
) : AbstractGatewayFilterFactory<ReactiveGeoLiteGatewayFilterFactory.Companion.Config>(Config::class.java) {
    companion object {
        class Config
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
            Mono.zip(
                geoLiteService
                    .city(xForwardedFor),
                geoLiteService
                    .asn(xForwardedFor),
            ).flatMap {
                val json = Commons.writeJson(it.t1.toDto(it.t2.toDto()), objectMapper)
                logger.debug("x-forwarded-for: {}", xForwardedFor)
                logger.debug("baggage {}: {}", baggage, json)
                logger.warn("{}", baggage) // force mdc, FIXME: configurable level
                Commons.withDynamicBaggage(
                    tracer = tracer,
                    key = baggage,
                    value = json,
                    publisher = chain.filter(exchange),
                )
            }.onErrorResume { chain.filter(exchange) }
        }
}
