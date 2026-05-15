package com.example.ds9reader.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages file picker state across the app.
 * Platform-specific implementations handle the actual picking.
 */
interface FilePickerManager {
    val pickerState: StateFlow<FilePickerState>

    /**
     * Request to pick a single EPUB file.
     */
    fun requestSingleFile()

    /**
     * Request to pick a directory of EPUBs.
     */
    fun requestDirectory()

    /**
     * Dismiss the picker.
     */
    fun dismiss()
}

/**
 * State of the file picker.
 */
sealed interface FilePickerState {
    data object Idle : FilePickerState
    data object PickingFile : FilePickerState
    data object PickingDirectory : FilePickerState
    data class Result(val paths: List<String>) : FilePickerState
    data class Error(val message: String) : FilePickerState
}
