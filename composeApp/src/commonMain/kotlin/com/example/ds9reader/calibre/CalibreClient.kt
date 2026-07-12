package com.example.ds9reader.calibre

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.domain.CalibreConfig
import com.example.ds9reader.domain.CalibreRemoteBook
import com.example.ds9reader.domain.LibraryError
import com.example.ds9reader.domain.ReadingPosition
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.toByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Calibre Content Server client.
 *
 * Uses:
 * - /ajax/books  (library listing)
 * - /ajax/book/<id> (metadata)
 * - /get/EPUB/<id> (download)
 * - /book-get-last-read-position/<library>/<id>
 * - /book-set-last-read-position/<library>/<id>
 *
 * Auth: HTTP Basic (recommended with calibre-server --auth-mode=basic)
 */
class CalibreClient(
    private val httpClientFactory: (CalibreConfig) -> HttpClient = ::defaultHttpClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    suspend fun testConnection(config: CalibreConfig): Either<LibraryError, String> {
        if (!config.isConfigured) return LibraryError.Config("Calibre base URL is required").left()
        return runCatching {
            val client = httpClientFactory(config)
            val response = client.get(config.normalizedBaseUrl())
            if (!response.status.isSuccess()) {
                error("HTTP ${response.status.value}")
            }
            "Connected to Calibre Content Server"
        }.fold(
            onSuccess = { it.right() },
            onFailure = { mapError(it).left() },
        )
    }

    suspend fun listBooks(config: CalibreConfig): Either<LibraryError, List<CalibreRemoteBook>> {
        if (!config.isConfigured) return LibraryError.Config("Calibre base URL is required").left()
        return runCatching {
            val client = httpClientFactory(config)
            val base = config.normalizedBaseUrl()
            val library = config.libraryId.ifBlank { null }
            val response = client.get("$base/ajax/books") {
                if (library != null) parameter("library_id", library)
            }
            if (response.status == HttpStatusCode.Unauthorized) {
                error("AUTH: Unauthorized")
            }
            if (!response.status.isSuccess()) {
                error("HTTP ${response.status.value}: ${response.bodyAsText().take(200)}")
            }
            val root = json.parseToJsonElement(response.bodyAsText())
            parseBooks(root, base, library)
        }.fold(
            onSuccess = { it.right() },
            onFailure = { mapError(it).left() },
        )
    }

    suspend fun downloadEpub(
        config: CalibreConfig,
        calibreId: Long,
    ): Either<LibraryError, ByteArray> {
        if (!config.isConfigured) return LibraryError.Config("Calibre base URL is required").left()
        return runCatching {
            val client = httpClientFactory(config)
            val base = config.normalizedBaseUrl()
            val library = config.libraryId.ifBlank { null }
            val response = client.get("$base/get/EPUB/$calibreId") {
                if (library != null) parameter("library_id", library)
            }
            if (response.status == HttpStatusCode.Unauthorized) error("AUTH: Unauthorized")
            if (response.status == HttpStatusCode.NotFound) error("EPUB format not available for book $calibreId")
            if (!response.status.isSuccess()) error("HTTP ${response.status.value}")
            response.bodyAsChannel().toByteArray()
        }.fold(
            onSuccess = { it.right() },
            onFailure = { mapError(it).left() },
        )
    }

    suspend fun getReadingPosition(
        config: CalibreConfig,
        calibreId: Long,
    ): Either<LibraryError, ReadingPosition?> {
        if (!config.isConfigured) return LibraryError.Config("Calibre base URL is required").left()
        return runCatching {
            val client = httpClientFactory(config)
            val base = config.normalizedBaseUrl()
            val library = config.libraryId.ifBlank { "Calibre_Library" }
            val device = config.deviceName.ifBlank { "DS9-Reader" }
            val response = client.get("$base/book-get-last-read-position/$library/$calibreId") {
                parameter("device", device)
            }
            if (response.status == HttpStatusCode.NotFound) return@runCatching null
            if (response.status == HttpStatusCode.Unauthorized) error("AUTH: Unauthorized")
            if (!response.status.isSuccess()) {
                // Older servers may not expose this endpoint.
                return@runCatching null
            }
            val text = response.bodyAsText()
            if (text.isBlank() || text == "null" || text == "{}") return@runCatching null
            val obj = json.parseToJsonElement(text).jsonObject
            val pos = obj["pos"]?.jsonPrimitive?.contentOrNull
                ?: obj["position"]?.jsonPrimitive?.contentOrNull
                ?: obj["cfi"]?.jsonPrimitive?.contentOrNull
                ?: ""
            val progress = obj["pos_frac"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
                ?: obj["progress"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
                ?: 0f
            val spine = obj["spine"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                ?: obj["spine_index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                ?: 0
            ReadingPosition(
                bookId = calibreId.toString(),
                progress = progress.coerceIn(0f, 1f),
                spineIndex = spine,
                anchor = pos,
                updatedAtEpochMs = 0L,
            )
        }.fold(
            onSuccess = { it.right() },
            onFailure = { mapError(it).left() },
        )
    }

    suspend fun setReadingPosition(
        config: CalibreConfig,
        calibreId: Long,
        position: ReadingPosition,
    ): Either<LibraryError, Unit> {
        if (!config.isConfigured) return LibraryError.Config("Calibre base URL is required").left()
        return runCatching {
            val client = httpClientFactory(config)
            val base = config.normalizedBaseUrl()
            val library = config.libraryId.ifBlank { "Calibre_Library" }
            val device = config.deviceName.ifBlank { "DS9-Reader" }
            val payload = LastReadPayload(
                device = device,
                cfi = position.anchor,
                pos = position.anchor,
                posFrac = position.progress.toDouble(),
                spine = position.spineIndex,
            )
            val response = client.post("$base/book-set-last-read-position/$library/$calibreId") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(LastReadPayload.serializer(), payload))
            }
            if (response.status == HttpStatusCode.Unauthorized) error("AUTH: Unauthorized")
            if (!response.status.isSuccess() && response.status != HttpStatusCode.NotFound) {
                error("HTTP ${response.status.value}: ${response.bodyAsText().take(200)}")
            }
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = { mapError(it).left() },
        )
    }

    private fun parseBooks(
        root: JsonElement,
        base: String,
        library: String?,
    ): List<CalibreRemoteBook> {
        val books = mutableListOf<CalibreRemoteBook>()
        when (root) {
            is JsonObject -> {
                // Shape A: map of id -> metadata
                if (root.values.firstOrNull() is JsonObject) {
                    root.forEach { (key, value) ->
                        val id = key.toLongOrNull() ?: value.jsonObject["application_id"]?.jsonPrimitive?.longOrNull
                        if (id != null && value is JsonObject) {
                            books += value.toRemoteBook(id, base, library)
                        }
                    }
                } else {
                    // Shape B: { "books": [ ... ] } or similar
                    root["books"]?.jsonArray?.forEach { el ->
                        val obj = el.jsonObject
                        val id = obj["application_id"]?.jsonPrimitive?.longOrNull
                            ?: obj["id"]?.jsonPrimitive?.longOrNull
                            ?: return@forEach
                        books += obj.toRemoteBook(id, base, library)
                    }
                }
            }
            is JsonArray -> {
                root.forEach { el ->
                    val obj = el.jsonObject
                    val id = obj["application_id"]?.jsonPrimitive?.longOrNull
                        ?: obj["id"]?.jsonPrimitive?.longOrNull
                        ?: return@forEach
                    books += obj.toRemoteBook(id, base, library)
                }
            }
            else -> Unit
        }
        return books.sortedBy { it.title.lowercase() }
    }

    private fun JsonObject.toRemoteBook(
        id: Long,
        base: String,
        library: String?,
    ): CalibreRemoteBook {
        val title = stringField("title") ?: "Untitled"
        val authors = when (val a = this["authors"]) {
            is JsonArray -> a.mapNotNull { it.jsonPrimitive.contentOrNull }
            is JsonPrimitive -> listOfNotNull(a.contentOrNull)
            else -> emptyList()
        }
        val formats = when (val f = this["formats"]) {
            is JsonArray -> f.mapNotNull { it.jsonPrimitive.contentOrNull?.uppercase() }
            is JsonPrimitive -> listOfNotNull(f.contentOrNull?.uppercase())
            is JsonObject -> f.keys.map { it.uppercase() }
            else -> emptyList()
        }
        val coverPath = stringField("cover")
        val coverUrl = when {
            coverPath.isNullOrBlank() -> "$base/get/cover/$id" + (library?.let { "?library_id=$it" } ?: "")
            coverPath.startsWith("http") -> coverPath
            else -> "$base/${coverPath.trimStart('/')}"
        }
        return CalibreRemoteBook(
            calibreId = id,
            uuid = stringField("uuid") ?: stringField("book_uuid"),
            title = title,
            authors = authors,
            coverUrl = coverUrl,
            description = stringField("comments") ?: stringField("description") ?: "",
            series = stringField("series") ?: "",
            tags = when (val t = this["tags"]) {
                is JsonArray -> t.mapNotNull { it.jsonPrimitive.contentOrNull }
                is JsonPrimitive -> listOfNotNull(t.contentOrNull)
                else -> emptyList()
            },
            hasEpub = formats.any { it.contains("EPUB") },
            formats = formats,
        )
    }

    private fun JsonObject.stringField(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun CalibreConfig.normalizedBaseUrl(): String =
        baseUrl.trim().trimEnd('/')

    private fun mapError(t: Throwable): LibraryError {
        val msg = t.message.orEmpty()
        return when {
            msg.startsWith("AUTH:") -> LibraryError.Auth("Calibre authentication failed. Check username/password.")
            msg.contains("Failed to connect", ignoreCase = true) -> LibraryError.Network(msg)
            msg.contains("Connection", ignoreCase = true) -> LibraryError.Network(msg)
            else -> LibraryError.Network(msg.ifBlank { "Calibre request failed" })
        }
    }

    companion object {
        fun defaultHttpClient(config: CalibreConfig): HttpClient {
            return HttpClient {
                expectSuccess = false
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        },
                    )
                }
                install(Logging) {
                    level = LogLevel.INFO
                }
                if (config.username.isNotBlank()) {
                    install(Auth) {
                        basic {
                            credentials {
                                BasicAuthCredentials(
                                    username = config.username,
                                    password = config.password,
                                )
                            }
                            sendWithoutRequest { true }
                        }
                    }
                }
                defaultRequest {
                    header(HttpHeaders.UserAgent, "DS9-Reader/0.2")
                    header(HttpHeaders.Accept, "application/json, application/epub+zip, */*")
                }
            }
        }
    }
}

@Serializable
private data class LastReadPayload(
    val device: String,
    val cfi: String = "",
    val pos: String = "",
    @SerialName("pos_frac") val posFrac: Double = 0.0,
    val spine: Int = 0,
)
