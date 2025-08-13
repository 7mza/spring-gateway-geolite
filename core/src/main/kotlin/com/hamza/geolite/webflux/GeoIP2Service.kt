package com.hamza.geolite.webflux

import com.hamza.geolite.GeoIP2Data
import com.hamza.geolite.toDto
import com.maxmind.geoip2.DatabaseReader
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.InetAddress

class GeoIP2Service(
    fileReader: IFileReader,
    cityDbPath: String,
    private val loadOnStartUp: Boolean = false,
) : IGeoIP2Service {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val cityDbReaderMono: Mono<DatabaseReader> =
        fileReader
            .readFileAsInputStream("classpath:$cityDbPath")
            // .readFileAsBytes("classpath:$cityDbPath")
            // .map { ByteArrayInputStream(it) }
            .map { DatabaseReader.Builder(it).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .cache()

    override fun lookupCity(ip: String): Mono<GeoIP2Data> =
        cityDbReaderMono.flatMap { reader ->
            Mono
                .fromCallable {
                    reader.city(InetAddress.getByName(ip)).toDto()
                }.subscribeOn(Schedulers.boundedElastic())
                .onErrorResume { ex ->
                    // Mono.empty on filters short-circuit the whole chain
                    logger.error("lookupCity: {}", ex.message)
                    Mono.error(ex)
                }
        }

    @PostConstruct
    fun init() {
        // fake call to load file
        if (loadOnStartUp) {
            cityDbReaderMono.subscribe(
                { logger.info("geoIP2 DB loaded") },
                { ex -> logger.error("geoIP2 DB failed to load: {}", ex.message) },
            )
        }
    }
}
