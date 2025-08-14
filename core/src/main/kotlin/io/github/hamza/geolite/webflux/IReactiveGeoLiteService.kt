package io.github.hamza.geolite.webflux

import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import io.github.hamza.geolite.IGeoLiteService
import reactor.core.publisher.Mono

interface IReactiveGeoLiteService : IGeoLiteService<Mono<CityResponse>, Mono<AsnResponse>, Mono<CountryResponse>>
