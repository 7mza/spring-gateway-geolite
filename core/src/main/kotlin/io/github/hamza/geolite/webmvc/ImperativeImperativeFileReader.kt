package io.github.hamza.geolite.webmvc

import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class ImperativeImperativeFileReader(
    private val resourceLoader: ResourceLoader,
) : IImperativeFileReader {
    override fun readFileAsBytes(
        path: String,
        bufferSize: Int,
    ): ByteArray = readFully(resourceLoader.getResource(path), bufferSize)

    override fun readFileAsInputStream(path: String): InputStream = resourceLoader.getResource(path).inputStream

    override fun readFileAsString(
        path: String,
        bufferSize: Int,
    ): String = String(readFileAsBytes(path, bufferSize), StandardCharsets.UTF_8)

    private fun readFully(
        resource: Resource,
        bufferSize: Int,
    ): ByteArray =
        resource.inputStream.use { inputStream ->
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(bufferSize)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            return outputStream.toByteArray()
        }
}
