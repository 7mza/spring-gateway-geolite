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
        ): JsonNode? =
            try {
                val node = mapper.valueToTree<ObjectNode>(data)
                properties.exclude.forEach { path ->
                    removeJsonPath(node, path.split("."))
                }
                node
            } catch (_: Exception) {
                null
            }

        private fun removeJsonPath(
            node: JsonNode?,
            path: List<String>,
        ) {
            if (node == null || path.isEmpty()) return
            val current = path[0]
            val rest = path.drop(1)
            if (node is ObjectNode) {
                if (rest.isEmpty()) {
                    node.remove(current)
                } else {
                    val child = node[current]
                    removeJsonPath(child, rest)
                }
            }
        }
    }
}
