package io.github.hamza.geolite.webflux

import io.github.hamza.geolite.IFileReader
import reactor.core.publisher.Mono
import java.io.InputStream

interface IReactiveFileReader : IFileReader<Mono<ByteArray>, Mono<String>, Mono<InputStream>>
