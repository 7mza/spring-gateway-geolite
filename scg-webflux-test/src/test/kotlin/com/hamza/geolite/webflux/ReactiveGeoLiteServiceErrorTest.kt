package com.hamza.geolite.webflux

import com.maxmind.geoip2.exception.AddressNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier
import java.io.FileNotFoundException
import java.io.IOException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReactiveGeoLiteServiceErrorTest {
    @Autowired
    private lateinit var reactiveFileReader: IReactiveFileReader

    @Value("\${geolite.db.city}")
    private lateinit var cityDbPath: String

    @Value("\${geolite.db.country}")
    private lateinit var countryDbPath: String

    @Value("\${geolite.db.asn}")
    private lateinit var asnDbPath: String

    private lateinit var service: IReactiveGeoLiteService

    @BeforeEach
    fun init() {
        service =
            ReactiveReactiveGeoLiteService(
                fileReader = reactiveFileReader,
                cityDbPath = cityDbPath,
                countryDbPath = countryDbPath,
                asnDbPath = asnDbPath,
            )
    }

    @Test
    fun `city with non existent ip`() {
        StepVerifier
            .create(service.city("0.0.0.0"))
            .expectError(AddressNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `city with db path errors`() {
        service =
            ReactiveReactiveGeoLiteService(
                fileReader = reactiveFileReader,
                cityDbPath = "",
                countryDbPath = countryDbPath,
                asnDbPath = asnDbPath,
            )

        StepVerifier
            .create(service.city("0.0.0.0"))
            .expectError(IOException::class.java)
            .verify()

        service =
            ReactiveReactiveGeoLiteService(
                fileReader = reactiveFileReader,
                cityDbPath = "./blabla.txt",
                countryDbPath = countryDbPath,
                asnDbPath = asnDbPath,
            )

        StepVerifier
            .create(service.city("0.0.0.0"))
            .expectError(FileNotFoundException::class.java)
            .verify()
    }
}
