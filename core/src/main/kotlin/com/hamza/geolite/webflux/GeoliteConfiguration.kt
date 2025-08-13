package com.hamza.geolite.webflux

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.tracing.Tracer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader

@AutoConfiguration
@ConditionalOnClass(
    name = [
        "org.springframework.cloud.gateway.config.GatewayAutoConfiguration",
        "io.micrometer.tracing.Tracer",
    ],
)
class GeoliteConfiguration {
    @Bean("ReactiveFileReader")
    fun fileReader(resourceLoader: ResourceLoader): IFileReader = FileReader(resourceLoader)

    @Bean("ReactiveGeoIP2Service")
    fun geoIP2Service(
        @Qualifier("ReactiveFileReader") fileReader: IFileReader,
        @Value($$"${geoip2.db.city}") cityDbPath: String,
        @Value($$"${geoip2.loadOnStartUp}") loadOnStartUp: Boolean,
    ): IGeoIP2Service =
        GeoIP2Service(
            fileReader = fileReader,
            cityDbPath = cityDbPath,
            loadOnStartUp = loadOnStartUp,
        )

    @Bean
    fun geoIP2Filter(
        @Qualifier("ReactiveGeoIP2Service") geoIP2Service: IGeoIP2Service,
        @Qualifier("GeoIP2ObjectMapper") objectMapper: ObjectMapper,
        tracer: Tracer,
        @Qualifier("GeoIP2XForwardedResolver") resolver: XForwardedRemoteAddressResolver,
        @Value($$"${geoip2.mdcKey}") mdcKey: String,
    ): AbstractGatewayFilterFactory<GeoIP2GatewayFilterFactory.Companion.Config> =
        GeoIP2GatewayFilterFactory(
            geoIP2Service = geoIP2Service,
            objectMapper = objectMapper,
            tracer = tracer,
            resolver = resolver,
            mdcKey = mdcKey,
        )
}
