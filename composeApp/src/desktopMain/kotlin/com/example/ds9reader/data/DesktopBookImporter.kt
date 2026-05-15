package com.example.ds9reader.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.LibraryError
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Desktop implementation of BookImporter.
 * 
 * Uses Java's built-in ZIP support to extract EPUB metadata.
 * EPUB files are ZIP archives with an OCF (Open Container Format) structure.
 * We parse the OPF file to extract metadata.
 */
class DesktopBookImporter(
    private val libraryPath: String
) : BookImporter {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun import(filePath: String): Either<LibraryError, ImportResult> {
        val file = File(filePath)
        if (!file.exists()) {
            return LibraryError.FileNotFound.left()
        }

        if (!file.name.endsWith(".epub", ignoreCase = true)) {
            return LibraryError.Parse("Not an EPUB file: ${file.name}").left()
        }

        return try {
            // Extract metadata from EPUB using ZIP parsing
            val metadata = extractEpubMetadata(file)

            // Copy file to library directory
            val libraryDir = File(libraryPath)
            if (!libraryDir.exists()) {
                libraryDir.mkdirs()
            }

            val destFileName = "${Uuid.random()}.epub"
            val destFile = File(libraryDir, destFileName)
            file.copyTo(destFile, overwrite = true)

            val book = Book(
                id = Uuid.random().toString(),
                title = metadata.title ?: file.nameWithoutExtension,
                author = metadata.author ?: "Unknown",
                coverUrl = null, // TODO: extract cover image
                filePath = destFile.absolutePath,
                fileSize = file.length(),
                addedAt = "",
                lastOpenedAt = null,
                progress = 0f,
                currentChapter = 0,
                totalChapters = 0, // TODO: extract chapter count
            )

            ImportResult.Success(book).right()
        } catch (e: Exception) {
            LibraryError.Parse("Failed to import EPUB: ${e.message}").left()
        }
    }

    private data class EpubMetadata(
        val title: String? = null,
        val author: String? = null,
        val language: String? = null,
        val identifier: String? = null,
    )

    /**
     * Extract basic metadata from an EPUB file.
     * EPUBs are ZIP files containing:
     * - mimetype
     * - META-INF/container.xml
     * - OCF file pointing to the OPF metadata file
     */
    private fun extractEpubMetadata(file: File): EpubMetadata {
        java.util.zip.ZipFile(file).use { zip ->
            // Find the OPF file by reading container.xml
            val containerEntry = zip.getEntry("META-INF/container.xml")
                ?: return EpubMetadata()

            val containerXml = zip.getInputStream(containerEntry).bufferedReader().readText()

            // Parse the OPF path from container.xml
            val opfPathRegex = """rootfile[^>]*full-path="([^"]+)"""".toRegex()
            val opfPathMatch = opfPathRegex.find(containerXml)
            val opfPath = opfPathMatch?.groups?.get(1)?.value ?: return EpubMetadata()

            // Read the OPF file
            val opfEntry = zip.getEntry(opfPath)
                ?: return EpubMetadata()

            val opfXml = zip.getInputStream(opfEntry).bufferedReader().readText()

            // Extract metadata from OPF
            val titleRegex = """<dc:title[^>]*>([^<]+)</dc:title>""".toRegex()
            val authorRegex = """<dc:creator[^>]*>([^<]+)</dc:creator>""".toRegex()

            val title = titleRegex.find(opfXml)?.groups?.get(1)?.value
            val author = authorRegex.find(opfXml)?.groups?.get(1)?.value

            return EpubMetadata(title = title, author = author)
        }
    }
}
