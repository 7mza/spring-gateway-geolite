package com.hamza.geolite

import com.maxmind.geoip2.model.CityResponse

/*
* FIXME: com.hamza.geolite.IGeoIP2Service
* should return the whole com.maxmind.geoip2.model.CityResponse
* but need a complex jackson mixin
* using this sub model for now
*/

data class GeoIP2Data(
    val city: String,
    val cityIsoCode: String,
    val country: String,
    val countryIsoCode: String,
)

fun CityResponse.toDto(): GeoIP2Data =
    GeoIP2Data(
        cityIsoCode = this.mostSpecificSubdivision.isoCode,
        city = this.city.name,
        countryIsoCode = this.country.isoCode,
        country = this.country.name,
    )
