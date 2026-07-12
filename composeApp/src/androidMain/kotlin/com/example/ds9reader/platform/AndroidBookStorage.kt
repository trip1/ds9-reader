package com.example.ds9reader.platform

import android.content.Context
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.domain.BookStorage
import com.example.ds9reader.domain.LibraryError
import java.io.File

class AndroidBookStorage(
    context: Context,
) : BookStorage {
    private val rootDir = File(context.filesDir, "books").apply { mkdirs() }

    override suspend fun saveEpub(bookId: String, bytes: ByteArray): Either<LibraryError, String> {
        return runCatching {
            val file = File(rootDir, "$bookId.epub")
            file.writeBytes(bytes)
            file.absolutePath
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unable to save EPUB").left() },
        )
    }

    override suspend fun loadEpub(path: String): Either<LibraryError, ByteArray> {
        return runCatching {
            val file = File(path)
            if (!file.exists()) error("Missing file: $path")
            file.readBytes()
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unable to load EPUB").left() },
        )
    }

    override suspend fun listLocalEpubs(directoryHint: String?): Either<LibraryError, List<String>> {
        return runCatching {
            rootDir.walkTopDown().filter { it.isFile && it.extension.equals("epub", true) }.map { it.absolutePath }.toList()
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unable to list EPUBs").left() },
        )
    }
}
