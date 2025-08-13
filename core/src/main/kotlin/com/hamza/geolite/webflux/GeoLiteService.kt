package com.hamza.geolite.webflux

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.InetAddress

class GeoLiteService(
    fileReader: IFileReader,
    cityDbPath: String,
    countryDbPath: String,
    asnDbPath: String,
) : IGeoLiteService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val cityDbReaderMono: Mono<DatabaseReader> =
        fileReader
            .readFileAsInputStream("classpath:$cityDbPath")
            .map { DatabaseReader.Builder(it).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .cache()

    private val countryDbReaderMono: Mono<DatabaseReader> =
        fileReader
            .readFileAsInputStream("classpath:$countryDbPath")
            .map { DatabaseReader.Builder(it).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .cache()

    private val asnDbReaderMono: Mono<DatabaseReader> =
        fileReader
            .readFileAsInputStream("classpath:$asnDbPath")
            .map { DatabaseReader.Builder(it).build() }
            .subscribeOn(Schedulers.boundedElastic())
            .cache()

    override fun city(ip: String): Mono<CityResponse> =
        cityDbReaderMono.flatMap { reader ->
            Mono
                .fromCallable {
                    reader.city(InetAddress.getByName(ip))
                }.subscribeOn(Schedulers.boundedElastic())
                .onErrorResume { ex ->
                    // Mono.empty on filters short-circuit the whole chain
                    logger.error("city: {}", ex.message)
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
                    logger.error("country: {}", ex.message)
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
                    logger.error("asn: {}", ex.message)
                    Mono.error(ex)
                }
        }
}
