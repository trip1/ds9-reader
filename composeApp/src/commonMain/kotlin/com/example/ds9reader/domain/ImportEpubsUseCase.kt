package com.example.ds9reader.domain

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.data.BookImporter
import com.example.ds9reader.data.ImportResult
import com.example.ds9reader.domain.LibraryRepository

/**
 * Use case for importing EPUB files into the library.
 * Handles both single file and directory imports.
 */
class ImportEpubsUseCase(
    private val repository: LibraryRepository,
    private val importer: BookImporter,
) {
    suspend fun importFiles(filePaths: List<String>): ImportSummary {
        val results = filePaths.mapNotNull { path ->
            importer.import(path).fold(
                ifRight = { result ->
                    when (result) {
                        is ImportResult.Success -> {
                            val book = result.book
                            repository.addBook(book).fold(
                                ifRight = { result },
                                ifLeft = { ImportResult.Error("Failed to save: ${it}") }
                            )
                        }
                        else -> result
                    }
                },
                ifLeft = { ImportResult.Error("Failed to open: $it") }
            )
        }

        return ImportSummary(
            total = results.size,
            succeeded = results.count { it is ImportResult.Success },
            failed = results.count { it is ImportResult.Error },
            errors = results.filterIsInstance<ImportResult.Error>().map { it.message }
        )
    }
}

/**
 * Summary of an import operation.
 */
data class ImportSummary(
    val total: Int,
    val succeeded: Int,
    val failed: Int,
    val errors: List<String>,
) {
    val hasErrors: Boolean get() = failed > 0
}
