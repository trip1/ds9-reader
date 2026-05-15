package com.example.ds9reader.platform

import android.content.Context
import com.example.ds9reader.ReadingActivity

/**
 * Opens the Android EPUB reader with the Readium navigator.
 */
actual fun openReader(context: Any, filePath: String) {
    if (context is Context) {
        ReadingActivity.start(context, filePath)
    }
}
