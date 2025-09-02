package io.github.hamza.geolite.webmvc

import org.springframework.core.io.ResourceLoader
import java.io.InputStream

class ImperativeImperativeFileReader(
    private val resourceLoader: ResourceLoader,
) : IImperativeFileReader {
    override fun readFileAsInputStream(path: String): InputStream = resourceLoader.getResource(path).inputStream
}
