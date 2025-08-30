package io.github.hamza.geolite

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.tracing.Tracer
import org.slf4j.Logger
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

        fun logAtLevel(
            logger: Logger,
            message: String,
            vararg args: Any?,
        ) {
            when {
                logger.isTraceEnabled -> logger.trace(message, *args)
                logger.isDebugEnabled -> logger.debug(message, *args)
                logger.isInfoEnabled -> logger.info(message, *args)
                logger.isWarnEnabled -> logger.warn(message, *args)
                logger.isErrorEnabled -> logger.error(message, *args)
                else -> logger.debug(message, *args)
            }
        }

        fun excludedFields(
            data: GeoLiteData?,
            properties: GeoliteSharedConfiguration.GeoliteProperties,
            mapper: ObjectMapper,
        ): JsonNode? {
            try {
                val node = mapper.valueToTree<ObjectNode>(data)
                properties.exclude.forEach { path ->
                    val parts = path.split(".")
                    when (parts.size) {
                        1 -> {
                            node.remove(parts[0])
                        }

                        2 if parts[1] == "*" -> {
                            node.remove(parts[0])
                        }

                        2 -> {
                            val (parentKey, childKey) = parts
                            val parentNode = node[parentKey] as? ObjectNode
                            parentNode?.remove(childKey)
                        }
                    }
                }
                return node
            } catch (_: Exception) {
                return null
            }
        }
    }
}
