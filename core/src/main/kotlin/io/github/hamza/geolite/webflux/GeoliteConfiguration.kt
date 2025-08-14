package io.github.hamza.geolite.webflux

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
    fun fileReader(resourceLoader: ResourceLoader): IReactiveFileReader = ReactiveReactiveFileReader(resourceLoader)

    @Bean("ReactiveGeoLiteService")
    fun geoLiteService(
        @Qualifier("ReactiveFileReader") fileReader: IReactiveFileReader,
        @Value("\${geolite.db.city}") cityDbPath: String,
        @Value("\${geolite.db.country}") countryDbPath: String,
        @Value("\${geolite.db.asn}") asnDbPath: String,
    ): IReactiveGeoLiteService =
        ReactiveReactiveGeoLiteService(
            fileReader = fileReader,
            cityDbPath = cityDbPath,
            countryDbPath = countryDbPath,
            asnDbPath = asnDbPath,
        )

    @Bean
    fun geoLiteFilter(
        @Qualifier("ReactiveGeoLiteService") geoLiteService: IReactiveGeoLiteService,
        @Qualifier("GeoLiteObjectMapper") objectMapper: ObjectMapper,
        tracer: Tracer,
        @Qualifier("GeoLiteForwardedResolver") resolver: XForwardedRemoteAddressResolver,
        @Value("\${geolite.baggage}") baggage: String,
    ): AbstractGatewayFilterFactory<ReactiveGeoLiteGatewayFilterFactory.Companion.Config> =
        ReactiveGeoLiteGatewayFilterFactory(
            geoLiteService = geoLiteService,
            objectMapper = objectMapper,
            tracer = tracer,
            resolver = resolver,
            baggage = baggage,
        )
}
