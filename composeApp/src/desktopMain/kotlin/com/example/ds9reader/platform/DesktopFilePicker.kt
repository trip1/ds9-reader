package com.example.ds9reader.platform

import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.PickerResult
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

class DesktopFilePicker : FilePicker {
    override suspend fun pickEpubFile(): PickerResult {
        return runCatching {
            val dialog = FileDialog(null as Frame?, "Select EPUB", FileDialog.LOAD)
            dialog.file = "*.epub"
            dialog.isVisible = true
            val file = dialog.file ?: return PickerResult.Cancelled
            val dir = dialog.directory ?: return PickerResult.Cancelled
            PickerResult.Success(listOf(File(dir, file).absolutePath))
        }.getOrElse { PickerResult.Error(it.message ?: "File pick failed") }
    }

    override suspend fun pickEpubDirectory(): PickerResult {
        return runCatching {
            val dialog = FileDialog(null as Frame?, "Select folder", FileDialog.LOAD)
            dialog.isVisible = true
            val dir = dialog.directory ?: return PickerResult.Cancelled
            val files = File(dir).listFiles { f -> f.isFile && f.extension.equals("epub", true) }
                ?.map { it.absolutePath }
                .orEmpty()
            if (files.isEmpty()) PickerResult.Error("No EPUB files found") else PickerResult.Success(files)
        }.getOrElse { PickerResult.Error(it.message ?: "Folder pick failed") }
    }
}
