package com.hamza.geolite.webflux

import com.fasterxml.jackson.databind.ObjectMapper
import com.hamza.geolite.Commons
import io.micrometer.tracing.Tracer
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver

class GeoIP2GatewayFilterFactory(
    private val geoIP2Service: IGeoIP2Service,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer,
    private val resolver: XForwardedRemoteAddressResolver,
    private val mdcKey: String,
) : AbstractGatewayFilterFactory<GeoIP2GatewayFilterFactory.Companion.Config>(Config::class.java) {
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
            geoIP2Service
                .lookupCity(xForwardedFor)
                .flatMap {
                    val json = Commons.writeJson(it, objectMapper)
                    logger.warn(mdcKey)
                    Commons.withDynamicBaggage(
                        tracer = tracer,
                        key = mdcKey,
                        value = json,
                        publisher = chain.filter(exchange),
                    )
                }.onErrorResume { chain.filter(exchange) }
        }
}
