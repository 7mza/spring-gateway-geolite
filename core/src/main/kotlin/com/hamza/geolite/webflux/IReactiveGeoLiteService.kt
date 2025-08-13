package com.hamza.geolite.webflux

import com.hamza.geolite.IGeoLiteService
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import reactor.core.publisher.Mono

interface IReactiveGeoLiteService : IGeoLiteService<Mono<CityResponse>, Mono<AsnResponse>, Mono<CountryResponse>>
