package com.example.ds9reader.domain

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val filePath: String = "",
    val fileSize: Long = 0,
    val addedAt: String = "",
    val lastOpenedAt: String? = null,
    val progress: Float = 0f,
    val currentSpineIndex: Int = 0,
    val currentAnchor: String = "",
    val totalChapters: Int = 0,
    val calibreId: Long? = null,
    val calibreUuid: String? = null,
    val isDownloaded: Boolean = false,
    val description: String = "",
    val series: String = "",
    val tags: String = "",
)

data class Bookmark(
    val id: String,
    val bookId: String,
    val spineIndex: Int,
    val anchor: String,
    val label: String,
    val createdAt: String,
)

data class ReadingSession(
    val id: String,
    val bookId: String,
    val startedAt: String,
    val endedAt: String?,
    val startAnchor: String,
    val endAnchor: String?,
    val durationSeconds: Int,
)

data class ReadingPosition(
    val bookId: String,
    val progress: Float,
    val spineIndex: Int,
    val anchor: String,
    val updatedAtEpochMs: Long,
)

data class CalibreConfig(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val libraryId: String = "",
    val deviceName: String = "DS9-Reader",
    val lastSyncAt: String? = null,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank()
}

data class CalibreRemoteBook(
    val calibreId: Long,
    val uuid: String?,
    val title: String,
    val authors: List<String>,
    val coverUrl: String?,
    val description: String,
    val series: String,
    val tags: List<String>,
    val hasEpub: Boolean,
    val formats: List<String>,
)

data class EpubChapter(
    val index: Int,
    val href: String,
    val title: String,
    val html: String,
)

data class EpubDocument(
    val title: String,
    val author: String,
    val chapters: List<EpubChapter>,
)

sealed interface LibraryError {
    data class NotFound(val bookId: String) : LibraryError
    data class Storage(val message: String) : LibraryError
    data object FileNotFound : LibraryError
    data class Parse(val message: String) : LibraryError
    data class Network(val message: String) : LibraryError
    data class Auth(val message: String) : LibraryError
    data class Config(val message: String) : LibraryError
}

sealed interface PickerResult {
    data class Success(val paths: List<String>) : PickerResult
    data object Cancelled : PickerResult
    data class Error(val message: String) : PickerResult
}
