package com.hamza.geolite.webmvc

import com.hamza.geolite.IFileReader
import java.io.InputStream

interface IImperativeFileReader : IFileReader<ByteArray, String, InputStream>
