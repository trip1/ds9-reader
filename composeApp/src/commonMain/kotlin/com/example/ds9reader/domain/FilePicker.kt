package com.example.ds9reader.domain

import arrow.core.Either

/**
 * Result of a file picker operation.
 */
sealed interface PickerResult {
    data object Cancelled : PickerResult
    data class Success(val paths: List<String>) : PickerResult
    data class Error(val message: String) : PickerResult
}

/**
 * Interface for platform-specific file picking.
 */
interface FilePicker {
    /**
     * Pick a single EPUB file.
     */
    suspend fun pickEpubFile(): PickerResult

    /**
     * Pick a directory containing EPUB files.
     */
    suspend fun pickEpubDirectory(): PickerResult
}
