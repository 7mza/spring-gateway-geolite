package io.github.hamza.geolite.webmvc

import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CountryResponse
import io.github.hamza.geolite.GeoLiteData
import io.github.hamza.geolite.IGeoLiteService

interface IImperativeGeoLiteService : IGeoLiteService<GeoLiteData, AsnResponse, CountryResponse>
