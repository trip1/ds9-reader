package com.example.ds9reader.platform

import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.PickerResult
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class PlatformFilePicker : FilePicker {

    override suspend fun pickEpubFile(): PickerResult {
        return suspendCoroutine { continuation ->
            try {
                val dialog = FileDialog(null as Frame?, "Select EPUB File", FileDialog.LOAD)
                dialog.isMultipleMode = false
                dialog.setFilenameFilter { _, name ->
                    name.endsWith(".epub", ignoreCase = true)
                }
                dialog.isVisible = true

                val file = dialog.file
                if (file != null) {
                    val directory = dialog.directory
                    val path = File(directory, file).absolutePath
                    continuation.resume(PickerResult.Success(listOf(path)))
                } else {
                    continuation.resume(PickerResult.Cancelled)
                }
            } catch (e: Exception) {
                continuation.resume(PickerResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override suspend fun pickEpubDirectory(): PickerResult {
        return suspendCoroutine { continuation ->
            try {
                val dialog = FileDialog(null as Frame?, "Select EPUB Directory", FileDialog.LOAD)
                dialog.mode = FileDialog.LOAD
                dialog.isVisible = true

                val directory = dialog.directory
                if (directory != null) {
                    val dir = File(directory)
                    val epubFiles = dir.listFiles { f ->
                        f.isFile && f.name.endsWith(".epub", ignoreCase = true)
                    }?.map { it.absolutePath } ?: emptyList()

                    if (epubFiles.isEmpty()) {
                        continuation.resume(PickerResult.Error("No EPUB files found in directory"))
                    } else {
                        continuation.resume(PickerResult.Success(epubFiles))
                    }
                } else {
                    continuation.resume(PickerResult.Cancelled)
                }
            } catch (e: Exception) {
                continuation.resume(PickerResult.Error(e.message ?: "Unknown error"))
            }
        }
    }
}
