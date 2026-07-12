package com.example.ds9reader.epub

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.ds9reader.domain.EpubBlock
import com.example.ds9reader.domain.EpubChapter
import com.example.ds9reader.domain.EpubDocument
import com.example.ds9reader.domain.EpubImage
import com.example.ds9reader.domain.LibraryError
import okio.Buffer
import kotlin.math.min

/**
 * Minimal pure-Kotlin EPUB parser (EPUB 2/3 best-effort).
 * Extracts chapter text blocks and referenced images for inline rendering.
 */
object EpubParser {
    fun parse(bytes: ByteArray): Either<LibraryError, EpubDocument> = runCatching {
        val zip = ZipReader(bytes)
        val container = zip.readText("META-INF/container.xml")
            ?: error("Missing META-INF/container.xml")
        val opfPath = Regex("""full-path\s*=\s*"([^"]+)"""")
            .find(container)
            ?.groupValues
            ?.get(1)
            ?: error("Unable to locate OPF package path")
        val opf = zip.readText(opfPath) ?: error("Missing OPF: $opfPath")
        val opfDir = opfPath.substringBeforeLast('/', missingDelimiterValue = "").let {
            if (it.isEmpty()) "" else "$it/"
        }

        val title = Regex("""<dc:title[^>]*>(.*?)</dc:title>""", RegexOption.IGNORE_CASE)
            .find(opf)?.groupValues?.get(1)?.unescapeXml()?.ifBlank { "Untitled" } ?: "Untitled"
        val author = Regex("""<dc:creator[^>]*>(.*?)</dc:creator>""", RegexOption.IGNORE_CASE)
            .find(opf)?.groupValues?.get(1)?.unescapeXml().orEmpty()

        val manifest = mutableMapOf<String, String>()
        Regex(
            """<item\b[^>]*\bid\s*=\s*"([^"]+)"[^>]*\bhref\s*=\s*"([^"]+)"[^>]*/?>""",
            setOf(RegexOption.IGNORE_CASE),
        ).findAll(opf).forEach { m ->
            manifest[m.groupValues[1]] = m.groupValues[2]
        }
        Regex(
            """<item\b[^>]*\bhref\s*=\s*"([^"]+)"[^>]*\bid\s*=\s*"([^"]+)"[^>]*/?>""",
            setOf(RegexOption.IGNORE_CASE),
        ).findAll(opf).forEach { m ->
            manifest[m.groupValues[2]] = m.groupValues[1]
        }

        val spineIds = Regex("""<itemref\b[^>]*\bidref\s*=\s*"([^"]+)"[^>]*/?>""", RegexOption.IGNORE_CASE)
            .findAll(opf)
            .map { it.groupValues[1] }
            .toList()

        val navHref = manifest.entries.firstOrNull { (id, href) ->
            id.contains("nav", true) || href.contains("nav", true) || href.endsWith(".ncx", true)
        }?.value

        val titlesByHref = mutableMapOf<String, String>()
        if (navHref != null) {
            val navXml = zip.readText(resolve(opfDir, navHref))
            if (navXml != null) {
                Regex(
                    """<a\b[^>]*\bhref\s*=\s*"([^"]+)"[^>]*>(.*?)</a>""",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                ).findAll(navXml).forEach { m ->
                    val href = m.groupValues[1].substringBefore('#').trim()
                    val label = m.groupValues[2].replace(Regex("<[^>]+>"), "").unescapeXml().trim()
                    if (href.isNotBlank() && label.isNotBlank()) {
                        titlesByHref[href] = label
                    }
                }
                Regex("""<content\b[^>]*\bsrc\s*=\s*"([^"]+)"[^>]*/?>""", RegexOption.IGNORE_CASE)
                    .findAll(navXml)
                    .forEachIndexed { idx, m ->
                        val href = m.groupValues[1].substringBefore('#').trim()
                        if (href.isNotBlank() && href !in titlesByHref) {
                            titlesByHref[href] = "Chapter ${idx + 1}"
                        }
                    }
            }
        }

        val images = linkedMapOf<String, EpubImage>()
        val chapters = spineIds.mapIndexedNotNull { index, id ->
            val href = manifest[id] ?: return@mapIndexedNotNull null
            val fullPath = resolve(opfDir, href)
            val html = zip.readText(fullPath) ?: return@mapIndexedNotNull null
            val chapterDir = fullPath.substringBeforeLast('/', missingDelimiterValue = "").let {
                if (it.isEmpty()) "" else "$it/"
            }
            val chapterTitle = titlesByHref[href]
                ?: titlesByHref[href.substringAfterLast('/')]
                ?: Regex("""<title[^>]*>(.*?)</title>""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.get(1)?.unescapeXml()?.ifBlank { null }
                ?: "Chapter ${index + 1}"

            val blocks = htmlToBlocks(
                html = html,
                chapterDir = chapterDir,
                zip = zip,
                images = images,
            )
            val plain = blocks.filterIsInstance<EpubBlock.Text>()
                .joinToString("\n\n") { it.text }
                .ifBlank { htmlToReadableText(html) }

            EpubChapter(
                index = index,
                href = href,
                title = chapterTitle,
                html = plain,
                blocks = blocks.ifEmpty { listOf(EpubBlock.Text(plain)) },
            )
        }

        if (chapters.isEmpty()) error("No readable chapters found in EPUB")
        EpubDocument(
            title = title,
            author = author,
            chapters = chapters,
            images = images,
        )
    }.fold(
        onSuccess = { it.right() },
        onFailure = { LibraryError.Parse(it.message ?: "Failed to parse EPUB").left() },
    )

    private fun htmlToBlocks(
        html: String,
        chapterDir: String,
        zip: ZipReader,
        images: MutableMap<String, EpubImage>,
    ): List<EpubBlock> {
        var cleaned = html
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), "")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), "")
            .replace(Regex("(?is)<!--.*?-->"), "")

        // Normalize common block boundaries before image extraction.
        cleaned = cleaned
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)</h[1-6]>"), "\n\n")
            .replace(Regex("(?i)<li[^>]*>"), "• ")
            .replace(Regex("(?i)</li>"), "\n")

        val imgRegex = Regex(
            """(?is)<img\b[^>]*?>""",
        )
        val parts = mutableListOf<EpubBlock>()
        var last = 0
        imgRegex.findAll(cleaned).forEach { match ->
            val before = cleaned.substring(last, match.range.first)
            appendTextBlocks(before, parts)

            val tag = match.value
            val src = Regex("""(?i)\bsrc\s*=\s*["']([^"']+)["']""")
                .find(tag)?.groupValues?.get(1)
                ?.unescapeXml()
                ?.trim()
                .orEmpty()
            val alt = Regex("""(?i)\balt\s*=\s*["']([^"']*)["']""")
                .find(tag)?.groupValues?.get(1)
                ?.unescapeXml()
                ?.trim()
                .orEmpty()

            if (src.isNotBlank() && !src.startsWith("data:", ignoreCase = true)) {
                val imagePath = resolve(chapterDir, src)
                val imageId = imagePath
                if (imageId !in images) {
                    val bytes = zip.readBytes(imagePath)
                    if (bytes != null && bytes.isNotEmpty()) {
                        images[imageId] = EpubImage(
                            id = imageId,
                            path = imagePath,
                            bytes = bytes,
                            mimeType = guessMime(imagePath),
                        )
                    }
                }
                if (imageId in images) {
                    parts += EpubBlock.Image(imageId = imageId, alt = alt)
                } else if (alt.isNotBlank()) {
                    parts += EpubBlock.Text("[Image: $alt]")
                }
            } else if (alt.isNotBlank()) {
                parts += EpubBlock.Text("[Image: $alt]")
            }
            last = match.range.last + 1
        }
        appendTextBlocks(cleaned.substring(last), parts)

        // Collapse consecutive empty text blocks.
        return parts.filterNot { block ->
            block is EpubBlock.Text && block.text.isBlank()
        }
    }

    private fun appendTextBlocks(rawHtmlChunk: String, out: MutableList<EpubBlock>) {
        val text = rawHtmlChunk
            .replace(Regex("<[^>]+>"), "")
            .unescapeXml()
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        if (text.isBlank()) return

        // Split into paragraphs so pagination can keep image/text blocks coherent.
        text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }.forEach { para ->
            out += EpubBlock.Text(para)
        }
    }

    private fun guessMime(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".bmp") -> "image/bmp"
            else -> "image/*"
        }
    }

    private fun resolve(baseDir: String, href: String): String {
        val cleaned = href.substringBefore('#').replace('\\', '/')
        if (cleaned.startsWith("/")) return cleaned.trimStart('/')
        if (baseDir.isBlank()) return cleaned
        val joined = (baseDir + cleaned).split('/')
        val out = ArrayDeque<String>()
        for (part in joined) {
            when {
                part.isEmpty() || part == "." -> Unit
                part == ".." -> if (out.isNotEmpty()) out.removeLast()
                else -> out.addLast(part)
            }
        }
        return out.joinToString("/")
    }

    private fun htmlToReadableText(html: String): String {
        var text = html
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), "")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), "")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)</h[1-6]>"), "\n\n")
            .replace(Regex("(?i)<li[^>]*>"), "• ")
            .replace(Regex("(?i)</li>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .unescapeXml()
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex(" *\\n *"), "\n")
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        return text.trim()
    }

    private fun String.unescapeXml(): String =
        this.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
}

/**
 * Minimal ZIP reader for stored/deflated entries.
 */
internal class ZipReader(private val bytes: ByteArray) {
    private val entries: Map<String, ZipEntry>

    init {
        entries = parseCentralDirectory()
    }

    fun readText(path: String): String? {
        val data = readBytes(path) ?: return null
        return data.decodeToString()
    }

    fun readBytes(path: String): ByteArray? {
        val normalized = path.trimStart('/')
        val entry = entries[normalized] ?: entries.entries.firstOrNull {
            it.key.equals(normalized, ignoreCase = true)
        }?.value ?: return null
        return readEntry(entry)
    }

    private fun readEntry(entry: ZipEntry): ByteArray? {
        val source = Buffer().write(bytes, entry.localHeaderOffset, bytes.size - entry.localHeaderOffset)
        val sig = source.readIntLe()
        if (sig != 0x04034b50) return null
        source.skip(22) // version..crc
        val nameLen = source.readShortLe().toInt() and 0xffff
        val extraLen = source.readShortLe().toInt() and 0xffff
        source.skip((nameLen + extraLen).toLong())
        val compressed = source.readByteArray(entry.compressedSize)
        return when (entry.compression) {
            0 -> compressed
            8 -> inflate(compressed, entry.uncompressedSize)
            else -> null
        }
    }

    private fun parseCentralDirectory(): Map<String, ZipEntry> {
        var eocd = -1
        val min = maxOf(0, bytes.size - 66_000)
        for (i in bytes.size - 22 downTo min) {
            if (bytes.readIntLe(i) == 0x06054b50) {
                eocd = i
                break
            }
        }
        if (eocd < 0) error("Invalid EPUB/ZIP: EOCD not found")
        val cdSize = bytes.readIntLe(eocd + 12)
        val cdOffset = bytes.readIntLe(eocd + 16)
        val map = linkedMapOf<String, ZipEntry>()
        var pos = cdOffset
        val end = cdOffset + cdSize
        while (pos + 46 <= end && pos + 4 <= bytes.size) {
            if (bytes.readIntLe(pos) != 0x02014b50) break
            val compression = bytes.readShortLe(pos + 10).toInt() and 0xffff
            val compressedSize = bytes.readIntLe(pos + 20).toLong() and 0xffffffffL
            val uncompressedSize = bytes.readIntLe(pos + 24).toLong() and 0xffffffffL
            val nameLen = bytes.readShortLe(pos + 28).toInt() and 0xffff
            val extraLen = bytes.readShortLe(pos + 30).toInt() and 0xffff
            val commentLen = bytes.readShortLe(pos + 32).toInt() and 0xffff
            val localHeaderOffset = bytes.readIntLe(pos + 42)
            val nameStart = pos + 46
            val name = bytes.decodeToString(nameStart, nameStart + nameLen)
            map[name] = ZipEntry(
                name = name,
                compression = compression,
                compressedSize = compressedSize,
                uncompressedSize = uncompressedSize,
                localHeaderOffset = localHeaderOffset,
            )
            pos = nameStart + nameLen + extraLen + commentLen
        }
        return map
    }

    private fun inflate(data: ByteArray, uncompressedSize: Long): ByteArray {
        return PlatformInflater.inflate(data, uncompressedSize.toInt().coerceAtLeast(data.size))
    }

    private data class ZipEntry(
        val name: String,
        val compression: Int,
        val compressedSize: Long,
        val uncompressedSize: Long,
        val localHeaderOffset: Int,
    )
}

internal expect object PlatformInflater {
    fun inflate(data: ByteArray, uncompressedSizeHint: Int): ByteArray
}

private fun ByteArray.readIntLe(index: Int): Int {
    return (this[index].toInt() and 0xff) or
        ((this[index + 1].toInt() and 0xff) shl 8) or
        ((this[index + 2].toInt() and 0xff) shl 16) or
        ((this[index + 3].toInt() and 0xff) shl 24)
}

private fun ByteArray.readShortLe(index: Int): Short {
    val v = (this[index].toInt() and 0xff) or ((this[index + 1].toInt() and 0xff) shl 8)
    return v.toShort()
}

private fun Buffer.readIntLe(): Int {
    val b0 = readByte().toInt() and 0xff
    val b1 = readByte().toInt() and 0xff
    val b2 = readByte().toInt() and 0xff
    val b3 = readByte().toInt() and 0xff
    return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
}

private fun Buffer.readShortLe(): Short {
    val b0 = readByte().toInt() and 0xff
    val b1 = readByte().toInt() and 0xff
    return (b0 or (b1 shl 8)).toShort()
}

private fun Buffer.write(bytes: ByteArray, offset: Int, count: Int): Buffer {
    write(bytes, offset, min(count, bytes.size - offset))
    return this
}
