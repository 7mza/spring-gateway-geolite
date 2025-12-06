package io.github.hamza.geolite

import io.micrometer.tracing.Tracer
import org.slf4j.Logger
import org.springframework.http.HttpHeaders
import reactor.core.publisher.Mono
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

fun HttpHeaders.normalize(): Map<String, List<String>> =
    this.headerNames().associateWith { headerName ->
        this.getValuesAsList(headerName)
    }

class Commons {
    companion object {
        inline fun <reified T> parseJson(
            json: String,
            objectMapper: ObjectMapper,
        ): T = objectMapper.readValue(json.trimIndent(), T::class.java)

        inline fun <reified T> writeJson(
            t: T,
            objectMapper: ObjectMapper,
        ): String = objectMapper.writeValueAsString(t)

        fun <T : Any> withDynamicBaggage(
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
                val node = mapper.valueToTree<JsonNode>(data)
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
