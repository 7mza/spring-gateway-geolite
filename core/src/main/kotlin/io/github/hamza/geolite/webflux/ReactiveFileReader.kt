package io.github.hamza.geolite.webflux

import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.InputStream

class ReactiveFileReader(
    private val resourceLoader: ResourceLoader,
) : IReactiveFileReader {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun readFileAsInputStream(path: String): Mono<InputStream> =
        Mono
            .fromCallable {
                resourceLoader.getResource(path).inputStream
            }.subscribeOn(Schedulers.boundedElastic())
            .onErrorResume {
                logger.warn("readFileAsInputStream: {}", it.message)
                Mono.empty()
            }
}
