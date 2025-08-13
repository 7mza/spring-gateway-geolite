package com.hamza.geolite.webflux

import com.hamza.geolite.IFileReader
import reactor.core.publisher.Mono
import java.io.InputStream

interface IReactiveFileReader : IFileReader<Mono<ByteArray>, Mono<String>, Mono<InputStream>>
