package io.github.hamza.geolite

interface IGeoLiteService<T, U, V> {
    fun city(ip: String): T

    fun asn(ip: String): U

    fun country(ip: String): V
}
