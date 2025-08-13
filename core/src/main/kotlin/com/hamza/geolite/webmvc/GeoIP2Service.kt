package com.hamza.geolite.webmvc

import com.hamza.geolite.GeoIP2Data
import com.hamza.geolite.toDto
import com.maxmind.geoip2.DatabaseReader
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import java.net.InetAddress

class GeoIP2Service(
    fileReader: IFileReader,
    cityDbPath: String,
    private val loadOnStartUp: Boolean = false,
) : IGeoIP2Service {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val cityDbReader: DatabaseReader =
        DatabaseReader
            .Builder(fileReader.readFileAsInputStream("classpath:$cityDbPath"))
            .build()

    override fun lookupCity(ip: String): GeoIP2Data =
        try {
            cityDbReader.city(InetAddress.getByName(ip)).toDto()
        } catch (ex: Exception) {
            logger.error("lookupCity: {}", ex.message)
            throw ex
        }

    @PostConstruct
    fun init() {
        // fake call to load file
        if (loadOnStartUp) {
            cityDbReader
        }
    }
}
