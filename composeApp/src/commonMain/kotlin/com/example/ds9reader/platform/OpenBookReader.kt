package com.example.ds9reader.platform

import com.example.ds9reader.domain.Book

/**
 * Platform book opener.
 * Android uses Readium; other platforms fall back to the Compose text reader.
 *
 * @return true if the platform handled opening the book.
 */
expect fun openBookWithPlatformReader(book: Book): Boolean
