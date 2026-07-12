package com.example.ds9reader.domain

import arrow.core.Either

interface BookStorage {
    /**
     * Persist EPUB bytes and return a platform-local path/key that can later be loaded.
     */
    suspend fun saveEpub(bookId: String, bytes: ByteArray): Either<LibraryError, String>

    /**
     * Load previously saved EPUB bytes.
     */
    suspend fun loadEpub(path: String): Either<LibraryError, ByteArray>

    /**
     * Optional local filesystem import path (desktop/android). Returns empty list on web.
     */
    suspend fun listLocalEpubs(directoryHint: String? = null): Either<LibraryError, List<String>>
}

interface SettingsStore {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
}
