package com.example.ds9reader.platform

import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.PickerResult

class IosFilePicker : FilePicker {
    override suspend fun pickEpubFile(): PickerResult =
        PickerResult.Error("Use Calibre sync on iOS for now")

    override suspend fun pickEpubDirectory(): PickerResult =
        PickerResult.Error("Use Calibre sync on iOS for now")
}
