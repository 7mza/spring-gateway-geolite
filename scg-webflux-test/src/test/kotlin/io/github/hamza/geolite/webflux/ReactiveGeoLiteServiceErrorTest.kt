package io.github.hamza.geolite.webflux

import com.maxmind.geoip2.exception.AddressNotFoundException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReactiveGeoLiteServiceErrorTest {
    @Autowired
    private lateinit var service: IReactiveGeoLiteService

    @Test
    fun `city with non existent ip`() {
        StepVerifier
            .create(service.city("0.0.0.0"))
            .expectError(AddressNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `country with non existent ip`() {
        StepVerifier
            .create(service.country("0.0.0.0"))
            .expectError(AddressNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `asn with non existent ip`() {
        StepVerifier
            .create(service.asn("0.0.0.0"))
            .expectError(AddressNotFoundException::class.java)
            .verify()
    }
}
