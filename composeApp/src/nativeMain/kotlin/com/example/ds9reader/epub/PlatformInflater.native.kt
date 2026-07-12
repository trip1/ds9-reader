package com.example.ds9reader.epub

/**
 * iOS/native inflater placeholder.
 * Many EPUBs use deflate; wire zlib properly in a follow-up.
 * For now, prefer desktop/android for full deflate support.
 */
actual object PlatformInflater {
    actual fun inflate(data: ByteArray, uncompressedSizeHint: Int): ByteArray {
        error("EPUB deflate inflation on iOS is not fully wired yet. Use stored EPUB or complete zlib binding.")
    }
}
