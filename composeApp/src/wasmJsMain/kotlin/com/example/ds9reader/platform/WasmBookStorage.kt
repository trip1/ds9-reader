package com.example.ds9reader.platform

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.domain.BookStorage
import com.example.ds9reader.domain.LibraryError
import kotlinx.browser.localStorage
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

class WasmBookStorage : BookStorage {
    override suspend fun saveEpub(bookId: String, bytes: ByteArray): Either<LibraryError, String> {
        return runCatching {
            val key = "ds9-epub-$bookId"
            // localStorage can't hold large binaries well; store base64 for small demos.
            // For production wasm, pair with OPFS. This keeps the architecture working.
            val b64 = bytes.toBase64()
            localStorage.setItem(key, b64)
            key
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unable to store EPUB in browser").left() },
        )
    }

    override suspend fun loadEpub(path: String): Either<LibraryError, ByteArray> {
        return runCatching {
            val b64 = localStorage.getItem(path) ?: error("Missing stored EPUB: $path")
            b64.fromBase64()
        }.fold(
            onSuccess = { it.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unable to load EPUB").left() },
        )
    }


    override suspend fun deleteEpub(path: String): Either<LibraryError, Unit> {
        return runCatching {
            localStorage.removeItem(path)
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = { LibraryError.Storage(it.message ?: "Unable to delete EPUB").left() },
        )
    }

    override suspend fun listLocalEpubs(directoryHint: String?): Either<LibraryError, List<String>> =
        emptyList<String>().right()
}

private fun ByteArray.toBase64(): String {
    val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val out = StringBuilder((size * 4 / 3) + 4)
    var i = 0
    while (i < size) {
        val b0 = this[i].toInt() and 0xff
        val b1 = if (i + 1 < size) this[i + 1].toInt() and 0xff else 0
        val b2 = if (i + 2 < size) this[i + 2].toInt() and 0xff else 0
        val n = (b0 shl 16) or (b1 shl 8) or b2
        out.append(table[(n shr 18) and 63])
        out.append(table[(n shr 12) and 63])
        out.append(if (i + 1 < size) table[(n shr 6) and 63] else '=')
        out.append(if (i + 2 < size) table[n and 63] else '=')
        i += 3
    }
    return out.toString()
}

private fun String.fromBase64(): ByteArray {
    val clean = filterNot { it == '=' || it.isWhitespace() }
    val table = IntArray(128) { -1 }
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    chars.forEachIndexed { idx, c -> table[c.code] = idx }
    val out = ArrayList<Byte>()
    var i = 0
    while (i + 3 < clean.length) {
        val n = (table[clean[i].code] shl 18) or
            (table[clean[i + 1].code] shl 12) or
            (table[clean[i + 2].code] shl 6) or
            table[clean[i + 3].code]
        out.add(((n shr 16) and 0xff).toByte())
        out.add(((n shr 8) and 0xff).toByte())
        out.add((n and 0xff).toByte())
        i += 4
    }
    return out.toByteArray()
}
