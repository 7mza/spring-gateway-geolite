package com.hamza.geolite

import com.maxmind.geoip2.model.CityResponse

/*
* FIXME: com.hamza.geolite.IGeoLiteService
* should return the whole com.maxmind.geoip2.model.CityResponse
* but need a complex jackson mixin
* using this sub model for now
*/

data class GeoLiteData(
    val city: String,
    val cityIsoCode: String,
    val country: String,
    val countryIsoCode: String,
)

fun CityResponse.toDto(): GeoLiteData =
    GeoLiteData(
        cityIsoCode = this.mostSpecificSubdivision.isoCode,
        city = this.city.name,
        countryIsoCode = this.country.isoCode,
        country = this.country.name,
    )
