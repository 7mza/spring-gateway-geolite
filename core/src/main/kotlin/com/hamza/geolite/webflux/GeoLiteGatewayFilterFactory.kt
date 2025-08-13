package com.hamza.geolite.webflux

import com.fasterxml.jackson.databind.ObjectMapper
import com.hamza.geolite.Commons
import com.hamza.geolite.toDto
import io.micrometer.tracing.Tracer
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver

class GeoLiteGatewayFilterFactory(
    private val geoLiteService: IGeoLiteService,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer,
    private val resolver: XForwardedRemoteAddressResolver,
    private val baggage: String,
) : AbstractGatewayFilterFactory<GeoLiteGatewayFilterFactory.Companion.Config>(Config::class.java) {
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
            geoLiteService
                .city(xForwardedFor)
                .flatMap {
                    val json = Commons.writeJson(it.toDto(), objectMapper)
                    logger.debug("{}: {}", baggage, it.toDto())
                    Commons.withDynamicBaggage(
                        tracer = tracer,
                        key = baggage,
                        value = json,
                        publisher = chain.filter(exchange),
                    )
                }.onErrorResume { chain.filter(exchange) }
        }
}
