package com.hamza.geolite

interface IGeoIP2Service<T> {
    fun lookupCity(ip: String): T
}
