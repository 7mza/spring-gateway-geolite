package io.github.hamza.geolite.webflux

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.blockhound.BlockingOperationError
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.function.Consumer

class BlockHoundTest {
    @Test
    fun `BlockHound should trigger if blocking code is detected`() {
        val ex =
            assertThrows<RuntimeException> {
                Mono
                    .delay(Duration.ofSeconds(1))
                    .doOnNext(
                        Consumer {
                            Thread.sleep(1)
                        },
                    ).block()
            }
        assertThat(ex.cause).isInstanceOf(BlockingOperationError::class.java)
    }
}
