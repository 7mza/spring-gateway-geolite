package io.github.hamza.geolite.webflux

import com.maxmind.db.InvalidDatabaseException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier
import java.io.FileNotFoundException

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["geolite.db.asn=geolite/GeoLite2-Country.mmdb", "geolite.db.city=", "geolite.db.country=./toto.txt"],
)
class ReactiveGeoLiteServiceDbPathErrorTest {
    @Autowired
    private lateinit var service: IReactiveGeoLiteService

    @Test
    fun `city with with no db path`() {
        StepVerifier
            .create(service.city("0.0.0.0"))
            .expectError(InvalidDatabaseException::class.java)
            .verify()
    }

    @Test
    fun `country with wrong db path`() {
        StepVerifier
            .create(service.country("0.0.0.0"))
            .expectError(FileNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `asn with correct path but wrong db`() {
        StepVerifier
            .create(service.asn("0.0.0.0"))
            .expectError(UnsupportedOperationException::class.java)
            .verify()
    }
}
