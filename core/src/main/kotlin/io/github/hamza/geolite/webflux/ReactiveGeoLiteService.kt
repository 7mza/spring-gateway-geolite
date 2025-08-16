package io.github.hamza.geolite.webflux

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import io.github.hamza.geolite.GeoliteSharedConfiguration.GeoliteProperties
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.InetAddress

class ReactiveGeoLiteService(
    fileReader: IReactiveFileReader,
    properties: GeoliteProperties,
) : IReactiveGeoLiteService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /* FIXME:
     * defensive db loading on path existence
     * return default dto instead of error for filter zip
     * return default dto immediately if no DB
     */

    private val cityDbReaderMono: Mono<DatabaseReader> =
        fileReader
            .readFileAsInputStream("classpath:${properties.db.city}")
            .map { DatabaseReader.Builder(it).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume { ex ->
                logger.error("cityDbReaderMono: {}", ex.message)
                Mono.error(ex)
            }

    private val countryDbReaderMono: Mono<DatabaseReader> =
        fileReader
            .readFileAsInputStream("classpath:${properties.db.country}")
            .map { DatabaseReader.Builder(it).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume { ex ->
                logger.error("countryDbReaderMono: {}", ex.message)
                Mono.error(ex)
            }

    private val asnDbReaderMono: Mono<DatabaseReader> =
        fileReader
            .readFileAsInputStream("classpath:${properties.db.asn}")
            .map { DatabaseReader.Builder(it).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume { ex ->
                logger.error("asnDbReaderMono: {}", ex.message)
                Mono.error(ex)
            }

    override fun city(ip: String): Mono<CityResponse> =
        cityDbReaderMono.flatMap { reader ->
            Mono
                .fromCallable {
                    reader.city(InetAddress.getByName(ip))
                }.subscribeOn(Schedulers.boundedElastic())
                .onErrorResume { ex ->
                    // Mono.empty on filters short-circuit the whole chain
                    logger.warn("city: {}", ex.message)
                    Mono.error(ex)
                }
        }

    override fun country(ip: String): Mono<CountryResponse> =
        countryDbReaderMono.flatMap { reader ->
            Mono
                .fromCallable {
                    reader.country(InetAddress.getByName(ip))
                }.subscribeOn(Schedulers.boundedElastic())
                .onErrorResume { ex ->
                    logger.warn("country: {}", ex.message)
                    Mono.error(ex)
                }
        }

    override fun asn(ip: String): Mono<AsnResponse> =
        asnDbReaderMono.flatMap { reader ->
            Mono
                .fromCallable {
                    reader.asn(InetAddress.getByName(ip))
                }.subscribeOn(Schedulers.boundedElastic())
                .onErrorResume { ex ->
                    logger.warn("asn: {}", ex.message)
                    Mono.error(ex)
                }
        }
}
