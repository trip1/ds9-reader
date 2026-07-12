package com.example.ds9reader.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.database.LibraryDatabase
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.Bookmark
import com.example.ds9reader.domain.CalibreConfig
import com.example.ds9reader.domain.LibraryError
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.ReadingSession
import com.example.ds9reader.domain.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.random.Random

class SqlLibraryRepository(
    database: LibraryDatabase,
) : LibraryRepository {
    private val db = database.libraryDatabaseQueries

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
                current_spine_index = book.currentSpineIndex.toLong(),
                current_anchor = book.currentAnchor,
                total_chapters = book.totalChapters.toLong(),
                calibre_id = book.calibreId,
                calibre_uuid = book.calibreUuid,
                is_downloaded = if (book.isDownloaded) 1 else 0,
                description = book.description,
                series = book.series,
                tags = book.tags,
            )
        }.toUnitEither()
    }

    override suspend fun upsertCalibreBook(book: Book): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = db.getBookById(book.id).executeAsOneOrNull()
            db.upsertCalibreBook(
                id = book.id,
                title = book.title,
                author = book.author,
                cover_url = book.coverUrl,
                file_path = existing?.file_path ?: book.filePath,
                file_size = existing?.file_size ?: book.fileSize,
                added_at = existing?.added_at ?: book.addedAt.ifBlank { "" },
                progress = existing?.progress ?: book.progress.toDouble(),
                current_spine_index = existing?.current_spine_index ?: book.currentSpineIndex.toLong(),
                current_anchor = existing?.current_anchor ?: book.currentAnchor,
                total_chapters = existing?.total_chapters ?: book.totalChapters.toLong(),
                calibre_id = book.calibreId,
                calibre_uuid = book.calibreUuid,
                is_downloaded = existing?.is_downloaded ?: if (book.isDownloaded) 1 else 0,
                description = book.description,
                series = book.series,
                tags = book.tags,
            )
        }.toUnitEither()
    }

    override suspend fun getAllBooks(): Either<LibraryError, List<Book>> = withContext(Dispatchers.IO) {
        runCatching { db.selectAllBooks().executeAsList().map { it.toDomain() } }.toListEither()
    }

    override suspend fun getDownloadedBooks(): Either<LibraryError, List<Book>> = withContext(Dispatchers.IO) {
        runCatching { db.selectDownloadedBooks().executeAsList().map { it.toDomain() } }.toListEither()
    }

    override suspend fun getContinueReading(): Either<LibraryError, List<Book>> = withContext(Dispatchers.IO) {
        runCatching { db.selectContinueReading().executeAsList().map { it.toDomain() } }.toListEither()
    }

    override suspend fun getBook(id: String): Either<LibraryError, Book> = withContext(Dispatchers.IO) {
        runCatching { db.getBookById(id).executeAsOneOrNull() }.fold(
            onSuccess = { row ->
                row?.toDomain()?.right() ?: LibraryError.NotFound(id).left()
            },
            onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() },
        )
    }

    override suspend fun removeBook(id: String): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching { db.deleteBook(id) }.toUnitEither()
    }

    override suspend fun updateProgress(
        bookId: String,
        progress: Float,
        spineIndex: Int,
        anchor: String,
    ): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.updateBookProgress(
                progress = progress.toDouble(),
                current_spine_index = spineIndex.toLong(),
                current_anchor = anchor,
                id = bookId,
            )
            db.markSyncDirty(bookId)
        }.toUnitEither()
    }

    override suspend fun markDownloaded(
        bookId: String,
        filePath: String,
        fileSize: Long,
    ): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching { db.markDownloaded(file_path = filePath, file_size = fileSize, id = bookId) }.toUnitEither()
    }

    override suspend fun clearDownloaded(bookId: String): Either<LibraryError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching { db.clearDownloaded(bookId) }.toUnitEither()
        }

    override suspend fun getBookmarks(bookId: String): Either<LibraryError, List<Bookmark>> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.getBookmarksForBook(bookId).executeAsList().map { row ->
                    Bookmark(
                        id = row.id,
                        bookId = row.book_id,
                        spineIndex = row.spine_index.toInt(),
                        anchor = row.anchor,
                        label = row.label,
                        createdAt = row.created_at,
                    )
                }
            }.toListEither()
        }

    override suspend fun addBookmark(bookmark: Bookmark): Either<LibraryError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.insertBookmark(
                    id = bookmark.id.ifBlank { randomId() },
                    book_id = bookmark.bookId,
                    spine_index = bookmark.spineIndex.toLong(),
                    anchor = bookmark.anchor,
                    label = bookmark.label,
                )
            }.toUnitEither()
        }

    override suspend fun removeBookmark(id: String): Either<LibraryError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching { db.deleteBookmark(id) }.toUnitEither()
        }

    override suspend fun startSession(bookId: String, anchor: String): Either<LibraryError, String> =
        withContext(Dispatchers.IO) {
            val sessionId = randomId()
            runCatching {
                db.insertSession(id = sessionId, book_id = bookId, start_anchor = anchor)
            }.fold(
                onSuccess = { sessionId.right() },
                onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() },
            )
        }

    override suspend fun endSession(
        sessionId: String,
        anchor: String,
        duration: Int,
    ): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.endSession(
                end_anchor = anchor,
                duration_seconds = duration.toLong(),
                id = sessionId,
            )
        }.toUnitEither()
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
                        startAnchor = row.start_anchor,
                        endAnchor = row.end_anchor,
                        durationSeconds = row.duration_seconds.toInt(),
                    )
                }
            }.toListEither()
        }

    override suspend fun getCalibreConfig(): Either<LibraryError, CalibreConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val row = db.getSettings().executeAsOneOrNull()
            if (row == null) {
                CalibreConfig()
            } else {
                CalibreConfig(
                    baseUrl = row.base_url,
                    username = row.username,
                    password = row.password,
                    libraryId = row.library_id,
                    deviceName = row.device_name,
                    lastSyncAt = row.last_sync_at,
                )
            }
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() },
        )
    }

    override suspend fun saveCalibreConfig(config: CalibreConfig): Either<LibraryError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.upsertSettings(
                    base_url = config.baseUrl.trim(),
                    username = config.username,
                    password = config.password,
                    library_id = config.libraryId,
                    device_name = config.deviceName.ifBlank { "DS9-Reader" },
                    last_sync_at = config.lastSyncAt,
                )
            }.toUnitEither()
        }

    override suspend fun markLibrarySynced(): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching { db.markSynced() }.toUnitEither()
    }

    override suspend fun markProgressDirty(bookId: String): Either<LibraryError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching { db.markSyncDirty(bookId) }.toUnitEither()
        }

    override suspend fun getDirtyBookIds(): Either<LibraryError, List<String>> = withContext(Dispatchers.IO) {
        runCatching { db.getDirtySyncStates().executeAsList().map { it.book_id } }.toListEither()
    }

    override suspend fun markProgressClean(
        bookId: String,
        progress: Float,
        spineIndex: Int,
        anchor: String,
    ): Either<LibraryError, Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.upsertSyncState(
                book_id = bookId,
                remote_progress = progress.toDouble(),
                remote_spine_index = spineIndex.toLong(),
                remote_anchor = anchor,
                dirty = 0,
            )
            db.markSyncClean(bookId)
        }.toUnitEither()
    }

    override suspend fun getThemeMode(): ThemeMode = withContext(Dispatchers.IO) {
        runCatching {
            val value = db.getAppSetting("theme_mode").executeAsOneOrNull()
            ThemeMode.fromStorage(value)
        }.getOrDefault(ThemeMode.System)
    }

    override suspend fun setThemeMode(mode: ThemeMode): Either<LibraryError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.upsertAppSetting(key = "theme_mode", value_ = mode.name)
            }.toUnitEither()
        }

    private fun com.example.ds9reader.database.Book.toDomain(): Book = Book(
        id = id,
        title = title,
        author = author,
        coverUrl = cover_url,
        filePath = file_path,
        fileSize = file_size,
        addedAt = added_at,
        lastOpenedAt = last_opened_at,
        progress = progress.toFloat(),
        currentSpineIndex = current_spine_index.toInt(),
        currentAnchor = current_anchor,
        totalChapters = total_chapters.toInt(),
        calibreId = calibre_id,
        calibreUuid = calibre_uuid,
        isDownloaded = is_downloaded != 0L,
        description = description,
        series = series,
        tags = tags,
    )

    private fun <T> Result<T>.toListEither(): Either<LibraryError, T> = fold(
        onSuccess = { it.right() },
        onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() },
    )

    private fun Result<*>.toUnitEither(): Either<LibraryError, Unit> = fold(
        onSuccess = { Unit.right() },
        onFailure = { LibraryError.Storage(it.message ?: "Unknown error").left() },
    )

    private fun randomId(): String =
        buildString(20) {
            repeat(20) {
                append(Random.nextInt(0, 16).toString(16))
            }
        }
}
