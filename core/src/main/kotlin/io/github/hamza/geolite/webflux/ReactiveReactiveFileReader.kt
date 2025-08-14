package io.github.hamza.geolite.webflux

import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.InputStream
import java.nio.charset.StandardCharsets

class ReactiveReactiveFileReader(
    private val resourceLoader: ResourceLoader,
) : IReactiveFileReader {
    // FIXME: OOM on large files
    override fun readFileAsBytes(
        path: String,
        bufferSize: Int,
    ): Mono<ByteArray> =
        Mono
            .fromCallable { resourceLoader.getResource(path) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { resource -> readFully(resource, bufferSize) }

    override fun readFileAsString(
        path: String,
        bufferSize: Int,
    ): Mono<String> =
        readFileAsBytes(path, bufferSize)
            .map { String(it, StandardCharsets.UTF_8) }

    override fun readFileAsInputStream(path: String): Mono<InputStream> =
        Mono
            .fromCallable {
                resourceLoader.getResource(path).inputStream
            }.subscribeOn(Schedulers.boundedElastic())

    private fun readFully(
        resource: Resource,
        bufferSize: Int,
    ): Mono<ByteArray> =
        DataBufferUtils
            .join(DataBufferUtils.read(resource, DefaultDataBufferFactory(), bufferSize))
            .map {
                val bytes = ByteArray(it.readableByteCount())
                it.read(bytes)
                DataBufferUtils.release(it)
                bytes
            }
}
