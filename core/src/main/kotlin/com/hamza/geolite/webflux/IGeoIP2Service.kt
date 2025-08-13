package com.hamza.geolite.webflux

import com.hamza.geolite.GeoIP2Data
import com.hamza.geolite.IGeoIP2Service
import reactor.core.publisher.Mono

interface IGeoIP2Service : IGeoIP2Service<Mono<GeoIP2Data>>
