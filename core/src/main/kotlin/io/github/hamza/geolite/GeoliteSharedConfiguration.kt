package io.github.hamza.geolite

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

@AutoConfiguration
class GeoliteSharedConfiguration {
    @Order(Ordered.LOWEST_PRECEDENCE)
    @Bean(name = ["GeoLiteObjectMapper"], defaultCandidate = false)
    fun objectMapper(): ObjectMapper =
        ObjectMapper().apply {
            registerKotlinModule()
        }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @Bean(name = ["GeoLiteForwardedResolver"], defaultCandidate = false)
    fun getXForwardedRemoteAddressResolver(properties: GeoliteProperties): XForwardedRemoteAddressResolver =
        XForwardedRemoteAddressResolver.maxTrustedIndex(properties.maxTrustedIndex ?: 1)

    @Configuration
    @ConfigurationProperties(prefix = "geolite")
    @RefreshScope
    class GeoliteProperties {
        lateinit var baggage: String
        lateinit var db: DbsPaths
        lateinit var exclude: List<String>
        var maxTrustedIndex: Int? = null

        class DbsPaths {
            lateinit var asn: String
            lateinit var city: String
            lateinit var country: String
        }
    }
}
