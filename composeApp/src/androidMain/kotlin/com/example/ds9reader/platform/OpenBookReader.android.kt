package com.example.ds9reader.platform

import android.content.Intent
import com.example.ds9reader.domain.Book
import com.example.ds9reader.readium.ReadiumReaderActivity
import org.koin.core.context.GlobalContext
import android.content.Context

actual fun openBookWithPlatformReader(book: Book): Boolean {
    if (!book.isDownloaded || book.filePath.isBlank()) return false
    val context = GlobalContext.get().get<Context>()
    val intent = Intent(context, ReadiumReaderActivity::class.java).apply {
        putExtra(ReadiumReaderActivity.EXTRA_BOOK_ID, book.id)
        putExtra(ReadiumReaderActivity.EXTRA_FILE_PATH, book.filePath)
        putExtra(ReadiumReaderActivity.EXTRA_TITLE, book.title)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    return true
}
