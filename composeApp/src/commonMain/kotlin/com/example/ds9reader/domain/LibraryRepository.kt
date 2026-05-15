package com.example.ds9reader.domain

import arrow.core.Either
import arrow.core.NonEmptyList
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.Bookmark
import com.example.ds9reader.domain.ReadingSession

/**
 * Domain errors that can occur during library operations.
 */
sealed interface LibraryError {
    data class NotFound(val bookId: String) : LibraryError
    data class Storage(val message: String) : LibraryError
    data object FileNotFound : LibraryError
    data class Parse(val message: String) : LibraryError
}

/**
 * Repository interface for managing the book library.
 * Uses Arrow's Either for error handling.
 */
interface LibraryRepository {
    suspend fun getAllBooks(): Either<LibraryError, List<Book>>
    suspend fun getBook(id: String): Either<LibraryError, Book>
    suspend fun addBook(book: Book): Either<LibraryError, Unit>
    suspend fun removeBook(id: String): Either<LibraryError, Unit>
    suspend fun updateProgress(
        bookId: String,
        progress: Float,
        chapter: Int
    ): Either<LibraryError, Unit>

    suspend fun getBookmarks(bookId: String): Either<LibraryError, List<Bookmark>>
    suspend fun addBookmark(bookmark: Bookmark): Either<LibraryError, Unit>
    suspend fun removeBookmark(id: String): Either<LibraryError, Unit>

    suspend fun startSession(bookId: String, cfi: String): Either<LibraryError, String>
    suspend fun endSession(sessionId: String, cfi: String, duration: Int): Either<LibraryError, Unit>
    suspend fun getRecentSessions(): Either<LibraryError, List<ReadingSession>>
}
