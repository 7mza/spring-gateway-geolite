package io.github.hamza.geolite

interface IFileReader<T, U, V> {
    fun readFileAsBytes(
        path: String,
        bufferSize: Int = 4096,
    ): T

    fun readFileAsString(
        path: String,
        bufferSize: Int = 4096,
    ): U

    fun readFileAsInputStream(path: String): V
}
