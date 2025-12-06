package io.github.hamza.geolite.webflux

import io.github.hamza.geolite.GeoliteSharedConfiguration.GeoliteProperties
import io.micrometer.tracing.Tracer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import tools.jackson.databind.ObjectMapper

@AutoConfiguration
@ConditionalOnClass(
    name = [
        "org.springframework.cloud.gateway.config.GatewayAutoConfiguration",
        "io.micrometer.tracing.Tracer",
    ],
)
class GeoliteConfiguration {
    @Bean("ReactiveFileReader")
    fun fileReader(resourceLoader: ResourceLoader): IReactiveFileReader = ReactiveFileReader(resourceLoader)

    @Bean("ReactiveGeoLiteService")
    fun geoLiteService(
        @Qualifier("ReactiveFileReader") fileReader: IReactiveFileReader,
        properties: GeoliteProperties,
    ): IReactiveGeoLiteService =
        ReactiveGeoLiteService(
            fileReader = fileReader,
            properties = properties,
        )

    @Bean
    fun geoLiteFilter(
        @Qualifier("ReactiveGeoLiteService") geoLiteService: IReactiveGeoLiteService,
        @Qualifier("GeoLiteObjectMapper") objectMapper: ObjectMapper,
        tracer: Tracer,
        @Qualifier("GeoLiteForwardedResolver") resolver: XForwardedRemoteAddressResolver,
        properties: GeoliteProperties,
        environment: Environment,
    ): AbstractGatewayFilterFactory<ReactiveGeoLiteGatewayFilterFactory.Companion.Config> =
        ReactiveGeoLiteGatewayFilterFactory(
            geoLiteService = geoLiteService,
            objectMapper = objectMapper,
            tracer = tracer,
            resolver = resolver,
            properties = properties,
            environment = environment,
        )
}
