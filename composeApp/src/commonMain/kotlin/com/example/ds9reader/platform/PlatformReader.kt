package com.example.ds9reader.platform

/**
 * Opens the platform-specific EPUB reader for the given file path.
 * 
 * @param context Platform-specific context (Screen for Compose, Android Context, etc.)
 * @param filePath Path to the EPUB file to open
 */
expect fun openReader(context: Any, filePath: String)
