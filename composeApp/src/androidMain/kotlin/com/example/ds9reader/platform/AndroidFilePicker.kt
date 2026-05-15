package com.example.ds9reader.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.PickerResult

/**
 * Android file picker implementation using Activity Result Contracts.
 * 
 * This should be created in the Activity and passed to the Compose layer,
 * or managed through a ViewModel.
 */
class AndroidFilePicker(
    private val activity: ComponentActivity
) : FilePicker {

    private var fileResult: ((PickerResult) -> Unit)? = null
    private var directoryResult: ((PickerResult) -> Unit)? = null

    private val pickFileLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uris: Array<Uri>? ->
        val result = fileResult ?: return@registerForActivityResult
        if (uris.isNullOrEmpty()) {
            result(PickerResult.Cancelled)
        } else {
            val paths = uris.mapNotNull { uri ->
                resolvePathFromUri(activity, uri)
            }
            if (paths.isEmpty()) {
                result(PickerResult.Error("Could not resolve file path"))
            } else {
                result(PickerResult.Success(paths))
            }
        }
        fileResult = null
    }

    private val pickDirectoryLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val result = directoryResult ?: return@registerForActivityResult
        if (uri == null) {
            result(PickerResult.Cancelled)
        } else {
            // For document tree, we need to enumerate files
            // This is handled asynchronously in the actual implementation
            result(PickerResult.Success(listOf(uri.toString())))
        }
        directoryResult = null
    }

    override suspend fun pickEpubFile(): PickerResult {
        return suspendCoroutine { continuation ->
            fileResult = { continuation.resume(it) }
            pickFileLauncher.launch(arrayOf("application/epub+zip"))
        }
    }

    override suspend fun pickEpubDirectory(): PickerResult {
        return suspendCoroutine { continuation ->
            directoryResult = { continuation.resume(it) }
            pickDirectoryLauncher.launch(null)
        }
    }

    private fun resolvePathFromUri(context: Context, uri: Uri): String? {
        // For content:// URIs, we copy the file to app storage
        // For file:// URIs, we can use the path directly
        return when (uri.scheme) {
            "file" -> uri.path
            "content" -> {
                // Copy to app's cache directory
                val cacheFile = File(context.cacheDir, "epub_import_${System.currentTimeMillis()}.epub")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                cacheFile.absolutePath
            }
            else -> null
        }
    }
}
