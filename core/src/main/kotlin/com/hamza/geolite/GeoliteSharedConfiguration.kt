package com.hamza.geolite

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

@AutoConfiguration
class GeoliteSharedConfiguration {
    @Order(Ordered.LOWEST_PRECEDENCE)
    @Bean(name = ["GeoIP2ObjectMapper"], defaultCandidate = false)
    fun objectMapper(): ObjectMapper =
        ObjectMapper().apply {
            registerKotlinModule()
        }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @Bean(name = ["GeoIP2XForwardedResolver"], defaultCandidate = false)
    fun getXForwardedRemoteAddressResolver(
        @Value($$"${geoip2.maxTrustedIndex}") maxTrustedIndex: Int,
    ): XForwardedRemoteAddressResolver = XForwardedRemoteAddressResolver.maxTrustedIndex(maxTrustedIndex)
}
