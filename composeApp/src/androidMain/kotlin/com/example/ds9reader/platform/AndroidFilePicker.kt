package com.example.ds9reader.platform

import android.content.Context
import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.PickerResult

/**
 * Android SAF picker is activity-scoped. For the minimal architecture we expose a stub
 * that points users to Calibre sync; a full Activity Result launcher can replace this.
 */
class AndroidFilePicker(
    @Suppress("unused") private val context: Context,
) : FilePicker {
    override suspend fun pickEpubFile(): PickerResult =
        PickerResult.Error("Use Calibre sync or implement SAF picker in MainActivity")

    override suspend fun pickEpubDirectory(): PickerResult =
        PickerResult.Error("Use Calibre sync or implement SAF picker in MainActivity")
}
