package io.github.hamza.geolite.webflux

import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import io.github.hamza.geolite.AsnResponseWrapper
import io.github.hamza.geolite.CityResponseWrapper
import io.github.hamza.geolite.CountryResponseWrapper
import io.github.hamza.geolite.GeoliteSharedConfiguration.GeoliteProperties
import jakarta.annotation.PreDestroy
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
            .map { DatabaseReader.Builder(it).withCache(CHMCache()).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .let { mono ->
                if (properties.cached == true) {
                    mono
                        .onErrorResume { Mono.empty() }
                        .cache()
                } else {
                    mono
                }
            }

    private val countryDbReaderMono: Mono<DatabaseReader> =
        fileReader
            .readFileAsInputStream("classpath:${properties.db.country}")
            .map { DatabaseReader.Builder(it).withCache(CHMCache()).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .let { mono ->
                if (properties.cached == true) {
                    mono
                        .onErrorResume { Mono.empty() }
                        .cache()
                } else {
                    mono
                }
            }

    private val asnDbReaderMono: Mono<DatabaseReader> =
        fileReader
            .readFileAsInputStream("classpath:${properties.db.asn}")
            .map { DatabaseReader.Builder(it).withCache(CHMCache()).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .let { mono ->
                if (properties.cached == true) {
                    mono
                        .onErrorResume { Mono.empty() }
                        .cache()
                } else {
                    mono
                }
            }

    override fun city(ip: String): Mono<CityResponseWrapper> =
        cityDbReaderMono
            .flatMap { reader ->
                Mono
                    .fromCallable {
                        CityResponseWrapper(reader.city(InetAddress.getByName(ip)))
                    }.subscribeOn(Schedulers.boundedElastic())
            }.onErrorResume {
                // Mono.empty on filters short-circuit the whole chain
                logger.warn("city: {}", it.message)
                Mono.just(CityResponseWrapper())
            }

    override fun country(ip: String): Mono<CountryResponseWrapper> =
        countryDbReaderMono
            .flatMap { reader ->
                Mono
                    .fromCallable {
                        CountryResponseWrapper(reader.country(InetAddress.getByName(ip)))
                    }.subscribeOn(Schedulers.boundedElastic())
            }.onErrorResume {
                logger.warn("country: {}", it.message)
                Mono.just(CountryResponseWrapper())
            }

    override fun asn(ip: String): Mono<AsnResponseWrapper> =
        asnDbReaderMono
            .flatMap { reader ->
                Mono
                    .fromCallable {
                        AsnResponseWrapper(reader.asn(InetAddress.getByName(ip)))
                    }.subscribeOn(Schedulers.boundedElastic())
            }.onErrorResume {
                logger.warn("asn: {}", it.message)
                Mono.just(AsnResponseWrapper())
            }

    @PreDestroy
    fun closeDatabaseReader() {
        cityDbReaderMono.doOnNext { it.close() }.subscribe()
        countryDbReaderMono.doOnNext { it.close() }.subscribe()
        asnDbReaderMono.doOnNext { it.close() }.subscribe()
    }
}
