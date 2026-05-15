package com.example.ds9reader.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.Bookmark
import com.example.ds9reader.domain.LibraryError
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.ReadingSession
import com.example.ds9reader.database.LibraryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map as flowMap

import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight-backed implementation of the library repository.
 */
class SqlLibraryRepository(
    database: LibraryDatabase,
) : LibraryRepository {

    private val db = database.libraryDatabaseQueries

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addBook(book: Book): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.insertBook(
                id = book.id,
                title = book.title,
                author = book.author,
                cover_url = book.coverUrl,
                file_path = book.filePath,
                file_size = book.fileSize,
                progress = book.progress.toDouble(),
                current_chapter = book.currentChapter.toLong(),
                total_chapters = book.totalChapters.toLong()
            )
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
        )
    }

    override suspend fun getAllBooks(): Either<LibraryError, List<Book>> = withContext(Dispatchers.IO) {
        runCatching {
            db.selectAllBooks().executeAsList().map { row ->
                Book(
                    id = row.id,
                    title = row.title,
                    author = row.author,
                    coverUrl = row.cover_url,
                    filePath = row.file_path,
                    fileSize = row.file_size,
                    addedAt = row.added_at,
                    lastOpenedAt = row.last_opened_at,
                    progress = row.progress.toFloat(),
                    currentChapter = row.current_chapter.toInt(),
                    totalChapters = row.total_chapters.toInt(),
                )
            }
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
        )
    }

    override suspend fun getBook(id: String): Either<LibraryError, Book> = withContext(Dispatchers.IO) {
        runCatching {
            db.getBookById(id).executeAsOneOrNull()
        }.fold(
            onSuccess = { row ->
                if (row != null) {
                    Book(
                        id = row.id,
                        title = row.title,
                        author = row.author,
                        coverUrl = row.cover_url,
                        filePath = row.file_path,
                        fileSize = row.file_size,
                        addedAt = row.added_at,
                        lastOpenedAt = row.last_opened_at,
                        progress = row.progress.toFloat(),
                        currentChapter = row.current_chapter.toInt(),
                        totalChapters = row.total_chapters.toInt(),
                    ).right()
                } else {
                    LibraryError.NotFound(id).left()
                }
            },
            onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
        )
    }

    override suspend fun removeBook(id: String): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.deleteBook(id)
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
        )
    }

    override suspend fun updateProgress(
        bookId: String,
        progress: Float,
        chapter: Int
    ): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.updateBookProgress(
                progress = progress.toDouble(),
                current_chapter = chapter.toLong(),
                id = bookId
            )
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getBookmarks(bookId: String): Either<LibraryError, List<Bookmark>> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.getBookmarksForBook(bookId).executeAsList().map { row ->
                    Bookmark(
                        id = row.id,
                        bookId = row.book_id,
                        chapterIndex = row.chapter_index.toInt(),
                        cfi = row.cfi,
                        label = row.label,
                        createdAt = row.created_at,
                    )
                }
            }.fold(
                onSuccess = { it.right() },
                onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
            )
        }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addBookmark(bookmark: Bookmark): Either<LibraryError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.insertBookmark(
                    id = bookmark.id,
                    book_id = bookmark.bookId,
                    chapter_index = bookmark.chapterIndex.toLong(),
                    cfi = bookmark.cfi,
                    label = bookmark.label
                )
            }.fold(
                onSuccess = { Unit.right() },
                onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
            )
        }

    override suspend fun removeBookmark(id: String): Either<LibraryError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.deleteBookmark(id)
            }.fold(
                onSuccess = { Unit.right() },
                onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
            )
        }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun startSession(bookId: String, cfi: String): Either<LibraryError, String> =
        withContext(Dispatchers.IO) {
            val sessionId = Uuid.random().toString()
            runCatching {
                db.insertSession(
                    id = sessionId,
                    book_id = bookId,
                    start_cfi = cfi
                )
            }.fold(
                onSuccess = { sessionId.right() },
                onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
            )
        }

    override suspend fun endSession(
        sessionId: String,
        cfi: String,
        duration: Int
    ): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.endSession(
                end_cfi = cfi,
                duration_seconds = duration.toLong(),
                id = sessionId
            )
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
        )
    }

    override suspend fun getRecentSessions(): Either<LibraryError, List<ReadingSession>> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.getRecentSessions().executeAsList().map { row ->
                    ReadingSession(
                        id = row.id,
                        bookId = row.book_id,
                        startedAt = row.started_at,
                        endedAt = row.ended_at,
                        startCfi = row.start_cfi,
                        endCfi = row.end_cfi,
                        durationSeconds = row.duration_seconds.toInt(),
                    )
                }
            }.fold(
                onSuccess = { it.right() },
                onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() }
            )
        }

    /**
     * Observable flow of all books — emits on every change.
     */
    fun observeBooks(): Flow<List<Book>> {
        return db.selectAllBooks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .flowMap { rows ->
                rows.map { row ->
                    Book(
                        id = row.id,
                        title = row.title,
                        author = row.author,
                        coverUrl = row.cover_url,
                        filePath = row.file_path,
                        fileSize = row.file_size,
                        addedAt = row.added_at,
                        lastOpenedAt = row.last_opened_at,
                        progress = row.progress.toFloat(),
                        currentChapter = row.current_chapter.toInt(),
                        totalChapters = row.total_chapters.toInt(),
                    )
                }
            }
    }
}
