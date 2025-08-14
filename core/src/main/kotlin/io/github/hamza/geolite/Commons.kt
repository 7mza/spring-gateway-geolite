package io.github.hamza.geolite

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.tracing.Tracer
import reactor.core.publisher.Mono

class Commons {
    companion object {
        inline fun <reified T> parseJson(
            json: String,
            objectMapper: ObjectMapper,
        ): T = objectMapper.readValue(json.trimIndent())

        inline fun <reified T> writeJson(
            t: T,
            objectMapper: ObjectMapper,
        ): String = objectMapper.writeValueAsString(t)

        fun <T> withDynamicBaggage(
            tracer: Tracer,
            key: String,
            value: String,
            publisher: Mono<T>,
        ): Mono<T> =
            Mono.using(
                { tracer.createBaggageInScope(key, value) },
                { publisher },
                { it.close() },
            )
    }
}
