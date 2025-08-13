package com.hamza.geolite.webmvc

import com.hamza.geolite.IFileReader
import java.io.InputStream

interface IFileReader : IFileReader<ByteArray, String, InputStream>
