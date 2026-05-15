package com.example.ds9reader.domain

/**
 * Provides the path to the user's library directory where EPUBs are stored.
 */
interface LibraryPathProvider {
    fun getLibraryPath(): String
}
