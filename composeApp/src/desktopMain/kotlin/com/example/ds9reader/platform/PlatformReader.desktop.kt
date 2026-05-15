package com.example.ds9reader.platform

/**
 * Desktop implementation of openReader.
 * Currently a placeholder — Readium navigator is Android-only.
 * On Desktop, we'd need a WebView-based reader or custom Compose renderer.
 */
actual fun openReader(context: Any, filePath: String) {
    // TODO: Implement desktop reader (e.g., WebView with EPUB.js, or custom Compose renderer)
    println("Opening EPUB reader for: $filePath (Desktop placeholder)")
}
