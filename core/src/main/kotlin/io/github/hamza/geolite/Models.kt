package io.github.hamza.geolite

import com.fasterxml.jackson.annotation.JsonInclude
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeoLiteData(
    val city: CityData? = null,
    val country: CountryData? = null,
    val asn: AsnData? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CityData(
    val name: String? = null,
    val isoCode: String? = null,
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

fun CityResponse.toDto(asn: AsnData? = null): GeoLiteData =
    GeoLiteData(
        city =
            CityData(
                name = this.city.name,
                isoCode = this.mostSpecificSubdivision.isoCode,
            ),
        country =
            CountryData(
                name = this.country.name,
                isoCode = this.country.isoCode,
            ),
        asn = asn,
    )

fun AsnResponse.toDto(): AsnData =
    AsnData(
        autonomousSystemOrganization = this.autonomousSystemOrganization,
        autonomousSystemNumber = this.autonomousSystemNumber,
        ipAddress = this.ipAddress,
        hostAddress = this.network.networkAddress.hostAddress,
        prefixLength = this.network.prefixLength,
    )
