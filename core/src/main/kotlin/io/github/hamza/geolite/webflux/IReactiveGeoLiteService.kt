package io.github.hamza.geolite.webflux

import io.github.hamza.geolite.AsnResponseWrapper
import io.github.hamza.geolite.CityResponseWrapper
import io.github.hamza.geolite.CountryResponseWrapper
import io.github.hamza.geolite.IGeoLiteService
import reactor.core.publisher.Mono

interface IReactiveGeoLiteService : IGeoLiteService<Mono<CityResponseWrapper>, Mono<AsnResponseWrapper>, Mono<CountryResponseWrapper>>
