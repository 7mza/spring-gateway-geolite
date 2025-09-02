package io.github.hamza.geolite

interface IFileReader<T, U, V> {
    fun readFileAsInputStream(path: String): V
}
