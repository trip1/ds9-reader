package com.example.ds9reader.domain

/**
 * Represents a book in the user's library.
 */
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val filePath: String,
    val fileSize: Long,
    val addedAt: String,
    val lastOpenedAt: String?,
    val progress: Float,
    val currentChapter: Int,
    val totalChapters: Int,
)

/**
 * Represents a bookmark within a book.
 */
data class Bookmark(
    val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val cfi: String,
    val label: String,
    val createdAt: String,
)

/**
 * Represents a single reading session.
 */
data class ReadingSession(
    val id: String,
    val bookId: String,
    val startedAt: String,
    val endedAt: String?,
    val startCfi: String,
    val endCfi: String?,
    val durationSeconds: Int,
)
