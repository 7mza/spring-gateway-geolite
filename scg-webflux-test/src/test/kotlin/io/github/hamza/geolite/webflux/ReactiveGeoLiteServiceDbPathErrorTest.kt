package io.github.hamza.geolite.webflux

import io.github.hamza.geolite.AsnResponseWrapper
import io.github.hamza.geolite.CityResponseWrapper
import io.github.hamza.geolite.CountryResponseWrapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["geolite.db.asn=geolite/GeoLite2-Country.mmdb", "geolite.db.city=", "geolite.db.country=./toto.txt"],
)
class ReactiveGeoLiteServiceDbPathErrorTest {
    @Autowired
    private lateinit var service: IReactiveGeoLiteService

    @Test
    fun `city with with no db path should return empty wrapper and not throw an exception`() {
        StepVerifier
            .create(service.city("0.0.0.0"))
            .expectNext(CityResponseWrapper())
            .verifyComplete()
    }

    @Test
    fun `country with wrong db path should return empty wrapper and not throw an exception`() {
        StepVerifier
            .create(service.country("0.0.0.0"))
            .expectNext(CountryResponseWrapper())
            .verifyComplete()
    }

    @Test
    fun `asn with correct path but wrong db should return empty wrapper and not throw an exception`() {
        StepVerifier
            .create(service.asn("0.0.0.0"))
            .expectNext(AsnResponseWrapper())
            .verifyComplete()
    }
}
