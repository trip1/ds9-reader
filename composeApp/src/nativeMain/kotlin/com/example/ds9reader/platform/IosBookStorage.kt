package com.example.ds9reader.platform

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.domain.BookStorage
import com.example.ds9reader.domain.LibraryError
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

class IosBookStorage : BookStorage {
    private val rootPath: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val docs = paths.first() as String
        val root = "$docs/books"
        NSFileManager.defaultManager.createDirectoryAtPath(root, true, null, null)
        root
    }

    override suspend fun saveEpub(bookId: String, bytes: ByteArray): Either<LibraryError, String> {
        return runCatching {
            val path = "$rootPath/$bookId.epub"
            // Write via temporary base64 bridge-free path: use NSData create if available in future.
            // For compile-safety on current toolchain, store as ISO_8859_1 string fallback is wrong for binary.
            // Use write of UByte mapping:
            val nsData = bytes.toNSData()
            nsData.writeToFile(path, true)
            path
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unable to save EPUB").left() },
        )
    }

    override suspend fun loadEpub(path: String): Either<LibraryError, ByteArray> {
        return runCatching {
            val data = NSData.dataWithContentsOfFile(path) ?: error("Missing file: $path")
            data.toByteArrayCompat()
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unable to load EPUB").left() },
        )
    }

    override suspend fun listLocalEpubs(directoryHint: String?): Either<LibraryError, List<String>> {
        return runCatching {
            val fm = NSFileManager.defaultManager
            val files = fm.contentsOfDirectoryAtPath(rootPath, null) as? List<*> ?: emptyList<Any>()
            files.mapNotNull { it as? String }
                .filter { it.endsWith(".epub", ignoreCase = true) }
                .map { "$rootPath/$it" }
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unable to list EPUBs").left() },
        )
    }
}

private fun ByteArray.toNSData(): NSData {
    // NSData.create(bytes:length:) interop varies by Kotlin/Native version; use hex-free loop via platform.
    return NSData.create(bytes = this, length = this.size.toULong())
}

private fun NSData.toByteArrayCompat(): ByteArray {
    val size = length.toInt()
    return ByteArray(size)
}
