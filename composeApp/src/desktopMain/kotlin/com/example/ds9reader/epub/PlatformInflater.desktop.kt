package com.example.ds9reader.epub

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

actual object PlatformInflater {
    actual fun inflate(data: ByteArray, uncompressedSizeHint: Int): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(data)
        val out = ByteArrayOutputStream(uncompressedSizeHint.coerceAtLeast(1024))
        val buffer = ByteArray(8 * 1024)
        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput()) break
                    if (inflater.needsDictionary()) error("ZIP dictionary required")
                } else {
                    out.write(buffer, 0, count)
                }
            }
        } finally {
            inflater.end()
        }
        return out.toByteArray()
    }
}
