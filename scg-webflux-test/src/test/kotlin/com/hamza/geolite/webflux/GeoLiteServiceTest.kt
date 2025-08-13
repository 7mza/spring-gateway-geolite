package com.hamza.geolite.webflux

import com.maxmind.geoip2.exception.AddressNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier
import java.io.FileNotFoundException
import java.io.IOException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GeoLiteServiceTest {
    @Autowired
    private lateinit var reactiveFileReader: IFileReader

    @Value($$"${geolite.db.city}")
    private lateinit var cityDbPath: String

    @Value($$"${geolite.db.country}")
    private lateinit var countryDbPath: String

    @Value($$"${geolite.db.asn}")
    private lateinit var asnDbPath: String

    private lateinit var service: IGeoLiteService

    @BeforeEach
    fun init() {
        service =
            GeoLiteService(
                fileReader = reactiveFileReader,
                cityDbPath = cityDbPath,
                countryDbPath = countryDbPath,
                asnDbPath = asnDbPath,
            )
    }

    // https://github.com/maxmind/GeoIP2-java
    @Test
    fun city() {
        StepVerifier
            .create(service.city("128.101.101.101"))
            .assertNext {
                // assertThat(it.city).isEqualTo("Minneapolis")
                // assertThat(it.cityIsoCode).isEqualTo("MN")
                // assertThat(it.country).isEqualTo("United States")
                // assertThat(it.countryIsoCode).isEqualTo("US")
                assertThat(it.country.isoCode).isEqualTo("US")
                assertThat(it.country.name).isEqualTo("United States")
                assertThat(it.country.names["zh-CN"]).isEqualTo("美国")
                assertThat(it.mostSpecificSubdivision.isoCode).isEqualTo("MN")
                assertThat(it.city.name).isEqualTo("Minneapolis")
                assertThat(it.postal.code).isEqualTo("55455")
                assertThat(it.location.latitude).isCloseTo(44.9733, within(0.1))
                assertThat(it.location.longitude).isCloseTo(-93.2323, within(0.1))
            }.verifyComplete()
    }

    @Test
    fun `city non existent ip`() {
        StepVerifier
            .create(service.city("0.0.0.0"))
            .expectError(AddressNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `db path errors`() {
        service =
            GeoLiteService(
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
            GeoLiteService(
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

    @Test
    fun asn() {
        StepVerifier
            .create(service.asn("128.101.101.101"))
            .assertNext {
                println(it)
                assertThat(it.autonomousSystemNumber).isEqualTo(217)
                assertThat(it.autonomousSystemOrganization).isEqualTo("UMN-SYSTEM")
                assertThat(it.ipAddress).isEqualTo("128.101.101.101")
                assertThat("${it.network.networkAddress.hostAddress}/${it.network.prefixLength}")
                    .isEqualTo("128.101.0.0/16")
            }.verifyComplete()
    }

    @Test
    fun country() {
        StepVerifier
            .create(service.country("128.101.101.101"))
            .assertNext {
                assertThat(it.country.isoCode).isEqualTo("US")
                assertThat(it.country.name).isEqualTo("United States")
                assertThat(it.country.names["zh-CN"]).isEqualTo("美国")
            }.verifyComplete()
    }
}
