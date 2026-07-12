package com.example.ds9reader.domain

import arrow.core.Either

interface LibraryRepository {
    suspend fun getAllBooks(): Either<LibraryError, List<Book>>
    suspend fun getDownloadedBooks(): Either<LibraryError, List<Book>>
    suspend fun getContinueReading(): Either<LibraryError, List<Book>>
    suspend fun getBook(id: String): Either<LibraryError, Book>
    suspend fun addBook(book: Book): Either<LibraryError, Unit>
    suspend fun upsertCalibreBook(book: Book): Either<LibraryError, Unit>
    suspend fun removeBook(id: String): Either<LibraryError, Unit>
    suspend fun updateProgress(
        bookId: String,
        progress: Float,
        spineIndex: Int,
        anchor: String,
    ): Either<LibraryError, Unit>
    suspend fun markDownloaded(bookId: String, filePath: String, fileSize: Long): Either<LibraryError, Unit>

    suspend fun getBookmarks(bookId: String): Either<LibraryError, List<Bookmark>>
    suspend fun addBookmark(bookmark: Bookmark): Either<LibraryError, Unit>
    suspend fun removeBookmark(id: String): Either<LibraryError, Unit>

    suspend fun startSession(bookId: String, anchor: String): Either<LibraryError, String>
    suspend fun endSession(sessionId: String, anchor: String, duration: Int): Either<LibraryError, Unit>
    suspend fun getRecentSessions(): Either<LibraryError, List<ReadingSession>>

    suspend fun getCalibreConfig(): Either<LibraryError, CalibreConfig>
    suspend fun saveCalibreConfig(config: CalibreConfig): Either<LibraryError, Unit>
    suspend fun markLibrarySynced(): Either<LibraryError, Unit>

    suspend fun markProgressDirty(bookId: String): Either<LibraryError, Unit>
    suspend fun getDirtyBookIds(): Either<LibraryError, List<String>>
    suspend fun markProgressClean(
        bookId: String,
        progress: Float,
        spineIndex: Int,
        anchor: String,
    ): Either<LibraryError, Unit>
}
