package com.hamza.geolite.webmvc

import com.hamza.geolite.webflux.FileReader
import com.hamza.geolite.webflux.GeoIP2Service
import com.hamza.geolite.webflux.IFileReader
import com.hamza.geolite.webflux.IGeoIP2Service
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader

@AutoConfiguration
@ConditionalOnClass(name = ["org.springframework.cloud.gateway.server.mvc.GatewayServerMvcAutoConfiguration"])
class GeoliteConfiguration {
    @Bean("FileReader")
    fun fileReader(resourceLoader: ResourceLoader): IFileReader = FileReader(resourceLoader)

    @Bean("GeoIP2Service")
    fun geoIP2Service(
        @Qualifier("FileReader") fileReader: IFileReader,
        @Value($$"${geoip2.db.city}") cityDbPath: String,
        @Value($$"${geoip2.loadOnStartUp}") loadOnStartUp: Boolean,
    ): IGeoIP2Service =
        GeoIP2Service(
            fileReader = fileReader,
            cityDbPath = cityDbPath,
            loadOnStartUp = loadOnStartUp,
        )
}
