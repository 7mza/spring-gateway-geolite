package com.hamza.geolite.webflux

import com.maxmind.geoip2.exception.AddressNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier
import java.io.FileNotFoundException
import java.io.IOException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GeoIP2ServiceTest {
    @Autowired
    private lateinit var reactiveFileReader: IFileReader

    @Value($$"${geoip2.db.city}")
    private lateinit var cityDbPath: String

    private lateinit var service: IGeoIP2Service

    // https://github.com/maxmind/GeoIP2-java
    @Test
    fun lookupCity() {
        service = GeoIP2Service(reactiveFileReader, cityDbPath)
        StepVerifier
            .create(service.lookupCity("128.101.101.101"))
            .assertNext {
                assertThat(it.city).isEqualTo("Minneapolis")
                assertThat(it.cityIsoCode).isEqualTo("MN")
                assertThat(it.country).isEqualTo("United States")
                assertThat(it.countryIsoCode).isEqualTo("US")
                // assertThat(it.country.isoCode).isEqualTo("US")
                // assertThat(it.country.name).isEqualTo("United States")
                // assertThat(it.country.names["zh-CN"]).isEqualTo("美国")
                // assertThat(it.mostSpecificSubdivision.isoCode).isEqualTo("MN")
                // assertThat(it.city.name).isEqualTo("Minneapolis")
                // assertThat(it.postal.code).isEqualTo("55455")
                // assertThat(it.location.latitude).isCloseTo(44.9733, within(0.1))
                // assertThat(it.location.longitude).isCloseTo(-93.2323, within(0.1))
            }.verifyComplete()
    }

    @Test
    fun `look up non existent ip`() {
        service = GeoIP2Service(reactiveFileReader, cityDbPath)
        StepVerifier
            .create(service.lookupCity("0.0.0.0"))
            .expectError(AddressNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `db path errors`() {
        service = GeoIP2Service(reactiveFileReader, "")
        StepVerifier
            .create(service.lookupCity("0.0.0.0"))
            .expectError(IOException::class.java)
            .verify()

        service = GeoIP2Service(reactiveFileReader, "./blabla.txt")
        StepVerifier
            .create(service.lookupCity("0.0.0.0"))
            .expectError(FileNotFoundException::class.java)
            .verify()
    }
}
