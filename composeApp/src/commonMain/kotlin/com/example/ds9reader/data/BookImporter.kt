package com.example.ds9reader.data

import arrow.core.Either
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.LibraryError

sealed interface ImportResult {
    data class Success(val book: Book) : ImportResult
    data class Error(val message: String) : ImportResult
}

/**
 * Interface for importing EPUB files and extracting metadata.
 * Platform-specific implementations use Readium streamer.
 */
interface BookImporter {
    /**
     * Import an EPUB file from the given path.
     * Returns the extracted book metadata or an error.
     */
    suspend fun import(filePath: String): Either<LibraryError, ImportResult>
}
