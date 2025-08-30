package io.github.hamza.geolite

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLiteData(
    @param:JsonProperty("xForwardedFor")
    val forwardedFor: String? = null,
    val path: String? = null,
    val city: CityData? = null,
    val country: CountryData? = null,
    val asn: AsnData? = null,
    val additionalHeaders: Map<String, List<String>>? = null,
) {
    @JsonIgnore
    fun isBot(): Boolean =
        this.city?.isBot() ?: false &&
            this.country?.isBot() ?: false &&
            this.asn?.isBot() ?: false
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CityData(
    val name: String? = null,
    val isoCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    @JsonIgnore
    fun isBot(): Boolean =
        this.name.isNullOrEmpty() &&
            this.isoCode.isNullOrEmpty() &&
            this.latitude == null &&
            this.longitude == null
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CountryData(
    val name: String? = null,
    val isoCode: String? = null,
) {
    @JsonIgnore
    fun isBot(): Boolean =
        this.name.isNullOrEmpty() &&
            this.isoCode.isNullOrEmpty()
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AsnData(
    val autonomousSystemNumber: Long? = null,
    val autonomousSystemOrganization: String? = null,
    val ipAddress: String? = null,
    val hostAddress: String? = null,
    val prefixLength: Int? = null,
) {
    @JsonIgnore
    fun isBot(): Boolean =
        this.autonomousSystemNumber == null &&
            this.autonomousSystemOrganization.isNullOrEmpty()
}

fun CityResponse.toDto(
    xForwardedFor: String,
    path: String,
    asn: AsnData? = null,
    additionalHeaders: Map<String, List<String>>? = null,
): GeoLiteData =
    GeoLiteData(
        forwardedFor = xForwardedFor,
        path = path,
        city =
            CityData(
                name = this.city.name,
                isoCode = this.mostSpecificSubdivision.isoCode,
                latitude = this.location.latitude,
                longitude = this.location.longitude,
            ),
        country =
            CountryData(
                name = this.country.name,
                isoCode = this.country.isoCode,
            ),
        asn = asn,
        additionalHeaders = additionalHeaders,
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
