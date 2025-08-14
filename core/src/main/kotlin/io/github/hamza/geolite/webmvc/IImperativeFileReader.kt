package io.github.hamza.geolite.webmvc

import io.github.hamza.geolite.IFileReader
import java.io.InputStream

interface IImperativeFileReader : IFileReader<ByteArray, String, InputStream>
