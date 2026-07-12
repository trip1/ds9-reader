package com.example.ds9reader.domain

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.calibre.CalibreClient
import com.example.ds9reader.epub.EpubParser

class SyncWithCalibreUseCase(
    private val repository: LibraryRepository,
    private val calibreClient: CalibreClient,
    private val storage: BookStorage,
) {
    suspend fun syncLibrary(): Either<LibraryError, Int> {
        val configResult = repository.getCalibreConfig()
        val config = when (configResult) {
            is Either.Left -> return LibraryError.Config("Unable to load Calibre settings").left()
            is Either.Right -> configResult.value
        }
        if (!config.isConfigured) {
            return LibraryError.Config("Configure Calibre Content Server URL first").left()
        }

        val remoteResult = calibreClient.listBooks(config)
        val remote = when (remoteResult) {
            is Either.Left -> return remoteResult
            is Either.Right -> remoteResult.value
        }

        var count = 0
        for (book in remote) {
            val id = "calibre-${book.calibreId}"
            val upsert = repository.upsertCalibreBook(
                Book(
                    id = id,
                    title = book.title,
                    author = book.authors.joinToString(", "),
                    coverUrl = book.coverUrl,
                    calibreId = book.calibreId,
                    calibreUuid = book.uuid,
                    description = book.description,
                    series = book.series,
                    tags = book.tags.joinToString(", "),
                    isDownloaded = false,
                ),
            )
            if (upsert is Either.Left) return upsert
            count++
        }
        repository.markLibrarySynced()
        return count.right()
    }

    suspend fun downloadBook(bookId: String): Either<LibraryError, Book> {
        val book = when (val result = repository.getBook(bookId)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        val calibreId = book.calibreId
            ?: return LibraryError.Config("Book is not linked to Calibre").left()
        val config = when (val result = repository.getCalibreConfig()) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        if (!config.isConfigured) {
            return LibraryError.Config("Configure Calibre Content Server URL first").left()
        }

        val bytes = when (val result = calibreClient.downloadEpub(config, calibreId)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        val path = when (val result = storage.saveEpub(bookId, bytes)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        when (val result = repository.markDownloaded(bookId, path, bytes.size.toLong())) {
            is Either.Left -> return result
            is Either.Right -> Unit
        }

        when (val posResult = calibreClient.getReadingPosition(config, calibreId)) {
            is Either.Right -> {
                val pos = posResult.value
                if (pos != null) {
                    repository.updateProgress(
                        bookId = bookId,
                        progress = pos.progress,
                        spineIndex = pos.spineIndex,
                        anchor = pos.anchor,
                    )
                    repository.markProgressClean(bookId, pos.progress, pos.spineIndex, pos.anchor)
                }
            }
            is Either.Left -> Unit
        }

        return repository.getBook(bookId)
    }

    suspend fun pushProgress(bookId: String): Either<LibraryError, Unit> {
        val book = when (val result = repository.getBook(bookId)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        val calibreId = book.calibreId ?: return Unit.right()
        val config = when (val result = repository.getCalibreConfig()) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        if (!config.isConfigured) return Unit.right()

        when (
            val result = calibreClient.setReadingPosition(
                config = config,
                calibreId = calibreId,
                position = ReadingPosition(
                    bookId = bookId,
                    progress = book.progress,
                    spineIndex = book.currentSpineIndex,
                    anchor = book.currentAnchor,
                    updatedAtEpochMs = 0L,
                ),
            )
        ) {
            is Either.Left -> return result
            is Either.Right -> Unit
        }
        repository.markProgressClean(
            bookId = bookId,
            progress = book.progress,
            spineIndex = book.currentSpineIndex,
            anchor = book.currentAnchor,
        )
        return Unit.right()
    }

    suspend fun pushDirtyProgress(): Either<LibraryError, Int> {
        val dirty = when (val result = repository.getDirtyBookIds()) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        var count = 0
        for (id in dirty) {
            when (val result = pushProgress(id)) {
                is Either.Left -> return result
                is Either.Right -> count++
            }
        }
        return count.right()
    }
}

class OpenBookUseCase(
    private val repository: LibraryRepository,
    private val storage: BookStorage,
) {
    suspend fun loadDocument(bookId: String): Either<LibraryError, Pair<Book, EpubDocument>> {
        val book = when (val result = repository.getBook(bookId)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        if (!book.isDownloaded || book.filePath.isBlank()) {
            return LibraryError.FileNotFound.left()
        }
        val bytes = when (val result = storage.loadEpub(book.filePath)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        val doc = when (val result = EpubParser.parse(bytes)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        return (book to doc).right()
    }
}
