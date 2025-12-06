package io.github.hamza.geolite.webflux

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReactiveGeoLiteServiceTest {
    @Autowired
    private lateinit var service: IReactiveGeoLiteService

    // https://github.com/maxmind/GeoIP2-java
    @Test
    fun city() {
        StepVerifier
            .create(service.city("128.101.101.101"))
            .assertNext {
                assertThat(it.cityResponse?.country()?.isoCode()).isEqualTo("US")
                assertThat(it.cityResponse?.country()?.name()).isEqualTo("United States")
                assertThat(it.cityResponse?.country()?.names()["zh-CN"]).isEqualTo("美国")
                assertThat(it.cityResponse?.mostSpecificSubdivision()?.isoCode()).isEqualTo("MN")
                assertThat(it.cityResponse?.city()?.name()).isEqualTo("Minneapolis")
                assertThat(it.cityResponse?.postal()?.code()).isEqualTo("55455")
                assertThat(it.cityResponse?.location()?.latitude()).isCloseTo(44.9733, within(0.1))
                assertThat(it.cityResponse?.location()?.longitude()).isCloseTo(-93.2323, within(0.1))
            }.verifyComplete()
    }

    @Test
    fun asn() {
        StepVerifier
            .create(service.asn("128.101.101.101"))
            .assertNext {
                println(it)
                assertThat(it.asnResponse?.autonomousSystemNumber()).isEqualTo(217)
                assertThat(it.asnResponse?.autonomousSystemOrganization()).isEqualTo("UMN-SYSTEM")
                assertThat(it.asnResponse?.ipAddress()?.hostAddress).isEqualTo("128.101.101.101")
                assertThat(
                    "${
                        it.asnResponse?.network()?.networkAddress()?.hostAddress
                    }/${it.asnResponse?.network()?.prefixLength}",
                ).isEqualTo("128.101.0.0/16")
            }.verifyComplete()
    }

    @Test
    fun country() {
        StepVerifier
            .create(service.country("128.101.101.101"))
            .assertNext {
                assertThat(it.countryResponse?.country()?.isoCode()).isEqualTo("US")
                assertThat(it.countryResponse?.country()?.name()).isEqualTo("United States")
                assertThat(it.countryResponse?.country()?.names()["zh-CN"]).isEqualTo("美国")
            }.verifyComplete()
    }
}
