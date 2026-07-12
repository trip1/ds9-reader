package com.example.ds9reader.epub

/**
 * Wasm inflate helper.
 * For full browser support, inject a raw-inflate function on globalThis.DS9_INFLATE_RAW.
 * Until then, only stored (method 0) ZIP entries are supported by ZipReader.
 */
actual object PlatformInflater {
    actual fun inflate(data: ByteArray, uncompressedSizeHint: Int): ByteArray {
        error(
            "Deflated EPUB entries need browser inflate support. " +
                "Provide globalThis.DS9_INFLATE_RAW(Uint8Array)=>Uint8Array or use a stored-compression EPUB.",
        )
    }
}
