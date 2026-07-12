package com.example.ds9reader.domain

import arrow.core.Either

interface FilePicker {
    suspend fun pickEpubFile(): PickerResult
    suspend fun pickEpubDirectory(): PickerResult
}
