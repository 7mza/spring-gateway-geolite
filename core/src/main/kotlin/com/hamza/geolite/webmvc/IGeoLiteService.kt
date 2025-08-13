package com.hamza.geolite.webmvc

import com.hamza.geolite.GeoLiteData
import com.hamza.geolite.IGeoLiteService
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CountryResponse

interface IGeoLiteService : IGeoLiteService<GeoLiteData, AsnResponse, CountryResponse>
