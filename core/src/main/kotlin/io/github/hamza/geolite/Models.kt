package io.github.hamza.geolite

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import org.springframework.http.HttpHeaders

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLiteData(
    @param:JsonProperty("xForwardedFor")
    val forwardedFor: String? = null,
    val path: String? = null,
    val query: String? = null,
    val city: CityData? = null,
    val country: CountryData? = null,
    val asn: AsnData? = null,
    val additionalHeaders: Map<String, List<String>>? = null,
    val botScoreThreshold: Int? = null,
) {
    @JsonProperty("isBot")
    fun getIsBot(): Boolean = this.getBotScore() >= botScoreThreshold!! // FIXME: isBot() not working ?

    @JsonProperty
    fun getBotScore(): Int = this.scoreCity() + this.scoreCountry() + this.scoreAsn() + this.scoreHeaders()

    private fun scoreCity(): Int {
        var score = 0
        if (this.city?.name.isNullOrEmpty()) score += 1
        if (this.city?.isoCode.isNullOrEmpty()) score += 1
        if (this.city?.latitude == null) score += 1
        if (this.city?.longitude == null) score += 1
        return score
    }

    private fun scoreCountry(): Int {
        var score = 0
        if (this.country?.name.isNullOrEmpty()) score += 1
        if (this.country?.isoCode.isNullOrEmpty()) score += 1
        return score
    }

    private fun scoreAsn(): Int {
        var score = 0
        if (this.asn?.autonomousSystemNumber == null) score += 1
        if (this.asn?.autonomousSystemOrganization.isNullOrEmpty()) score += 1
        if (this.asn?.ipAddress.isNullOrEmpty()) score += 1
        if (this.asn?.hostAddress.isNullOrEmpty()) score += 1
        if (this.asn?.prefixLength == null) score += 1
        return score
    }

    // FIXME: from conf
    private fun scoreHeaders(): Int {
        var score = 0
        val headers = additionalHeaders ?: emptyMap()
        if (!headers.containsKey(HttpHeaders.USER_AGENT.lowercase())) score += 1
        // if (!headers.containsKey(HttpHeaders.ACCEPT_LANGUAGE.lowercase())) score += 1
        // if (!headers.containsKey(HttpHeaders.ACCEPT.lowercase())) score += 1
        // if (!headers.containsKey(HttpHeaders.REFERER.lowercase())) score += 1
        // if (!headers.containsKey(HttpHeaders.CONNECTION.lowercase())) score += 1
        // if (!headers.containsKey("sec-fetch-site")) score += 1
        return score
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CityData(
    val name: String? = null,
    val isoCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CountryData(
    val name: String? = null,
    val isoCode: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AsnData(
    val autonomousSystemNumber: Long? = null,
    val autonomousSystemOrganization: String? = null,
    val ipAddress: String? = null,
    val hostAddress: String? = null,
    val prefixLength: Int? = null,
)

fun CityResponse.toCityDto(): CityData =
    CityData(
        name = this.city.name,
        isoCode = this.mostSpecificSubdivision.isoCode,
        latitude = this.location.latitude,
        longitude = this.location.longitude,
    )

fun CityResponse.toCountryDto(): CountryData =
    CountryData(
        name = this.country.name,
        isoCode = this.country.isoCode,
    )

fun AsnResponse.toDto(): AsnData =
    AsnData(
        autonomousSystemOrganization = this.autonomousSystemOrganization,
        autonomousSystemNumber = this.autonomousSystemNumber,
        ipAddress = this.ipAddress,
        hostAddress = this.network.networkAddress.hostAddress,
        prefixLength = this.network.prefixLength,
    )

// just to avoid constructing CountryResponse in error flows
data class CountryResponseWrapper(
    val countryResponse: CountryResponse? = null,
)

// just to avoid constructing CityResponse in error flows
data class CityResponseWrapper(
    val cityResponse: CityResponse? = null,
)

// just to avoid constructing AsnResponse in error flows
data class AsnResponseWrapper(
    val asnResponse: AsnResponse? = null,
)
