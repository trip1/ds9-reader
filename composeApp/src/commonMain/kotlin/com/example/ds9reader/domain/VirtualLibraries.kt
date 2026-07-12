package com.example.ds9reader.domain

/**
 * Built-in Virtual Library definitions for DS9 Reader.
 * These mirror the Calibre VL plan (tags + title/author/series heuristics).
 */
data class VirtualLibrary(
    val id: String,
    val name: String,
    val description: String,
    val matcher: (Book) -> Boolean,
)

object VirtualLibraries {
    val all = listOf(
        VirtualLibrary(
            id = "all",
            name = "All Books",
            description = "Entire library",
            matcher = { true },
        ),
        VirtualLibrary(
            id = "to-read",
            name = "To Read",
            description = "Queued books",
            matcher = { book ->
                book.hasTag("Status:To Read") ||
                    book.hasTag("To Read") ||
                    book.hasTag("to-read")
            },
        ),
        VirtualLibrary(
            id = "reading",
            name = "Reading",
            description = "Currently reading",
            matcher = { book ->
                book.hasTag("Status:Reading") ||
                    book.hasTag("Reading") ||
                    (book.progress > 0f && book.progress < 0.99f)
            },
        ),
        VirtualLibrary(
            id = "finished",
            name = "Finished",
            description = "Completed books",
            matcher = { book ->
                book.hasTag("Status:Finished") ||
                    book.hasTag("Finished") ||
                    book.progress >= 0.99f
            },
        ),
        VirtualLibrary(
            id = "scifi",
            name = "SciFi",
            description = "Science fiction",
            matcher = { book ->
                book.hasAnyTag(
                    "Genre:Science Fiction",
                    "Science Fiction",
                    "Science Fiction/Fantasy",
                    "Sci-Fi",
                    "SciFi",
                    "SF",
                ) || book.titleContains("science fiction") ||
                    book.authorContains("Asimov") ||
                    book.authorContains("William Gibson") ||
                    book.authorContains("Orson Scott Card") ||
                    book.authorContains("David Weber") ||
                    book.authorContains("John Ringo") ||
                    book.authorContains("Anne McCaffrey")
            },
        ),
        VirtualLibrary(
            id = "fantasy",
            name = "Fantasy",
            description = "Fantasy",
            matcher = { book ->
                book.hasAnyTag("Genre:Fantasy", "Fantasy", "Science Fiction/Fantasy") ||
                    book.authorContains("Piers Anthony") ||
                    book.authorContains("Terry Brooks") ||
                    book.authorContains("Mercedes Lackey") ||
                    book.authorContains("David Eddings") ||
                    book.authorContains("Glen Cook") ||
                    book.authorContains("Robert Asprin") ||
                    book.authorContains("Aspirin, Robert")
            },
        ),
        VirtualLibrary(
            id = "american-classics",
            name = "American Classics",
            description = "US classics",
            matcher = { book ->
                book.hasAnyTag("Genre:American Classics", "American Classics") ||
                    book.authorContains("Mark Twain") ||
                    book.authorContains("Herman Melville") ||
                    book.authorContains("Nathaniel Hawthorne") ||
                    book.authorContains("Edgar Allan Poe") ||
                    book.authorContains("Walt Whitman") ||
                    book.authorContains("Emily Dickinson") ||
                    book.authorContains("F. Scott Fitzgerald") ||
                    book.authorContains("Fitzgerald") ||
                    book.authorContains("Ernest Hemingway") ||
                    book.authorContains("John Steinbeck") ||
                    book.authorContains("Jack London") ||
                    book.authorContains("Louisa May Alcott") ||
                    book.authorContains("Ralph Waldo Emerson") ||
                    book.authorContains("Henry David Thoreau") ||
                    book.authorContains("Harriet Beecher Stowe") ||
                    book.authorContains("Willa Cather") ||
                    book.authorContains("Edith Wharton") ||
                    book.authorContains("Henry James") ||
                    book.authorContains("Harper Lee")
            },
        ),
        VirtualLibrary(
            id = "star-trek",
            name = "Star Trek",
            description = "Star Trek franchise",
            matcher = { book ->
                book.hasAnyTag("Franchise:Star Trek", "Star Trek") ||
                    book.titleContains("star trek") ||
                    book.seriesContains("star trek")
            },
        ),
        VirtualLibrary(
            id = "business",
            name = "Business",
            description = "Business / career",
            matcher = { book ->
                book.hasAnyTag("Genre:Business", "Business") ||
                    book.titleContains("business") ||
                    book.titleContains("management") ||
                    book.titleContains("leadership") ||
                    book.titleContains("marketing") ||
                    book.titleContains("invest") ||
                    book.titleContains("startup") ||
                    book.titleContains("entrepreneur") ||
                    book.titleContains("finance") ||
                    book.titleContains("economics")
            },
        ),
        VirtualLibrary(
            id = "classics",
            name = "Classics",
            description = "Broad classics",
            matcher = { book ->
                book.hasAnyTag(
                    "Genre:Classics",
                    "Classics",
                    "Delphi Classics",
                    "Ancient Classics",
                    "Literature - Classics",
                ) || book.authorContains("Delphi Classics")
            },
        ),
        VirtualLibrary(
            id = "poetry",
            name = "Poetry",
            description = "Poetry",
            matcher = { book ->
                book.hasAnyTag("Genre:Poetry", "Poetry", "Poetry Series", "Literature/Poetry")
            },
        ),
        VirtualLibrary(
            id = "romance",
            name = "Romance",
            description = "Romance",
            matcher = { book ->
                book.hasAnyTag("Genre:Romance", "Romance", "Love stories", "Erotic") ||
                    book.authorContains("Nora Roberts")
            },
        ),
        VirtualLibrary(
            id = "mystery",
            name = "Mystery",
            description = "Mystery / thriller",
            matcher = { book ->
                book.hasAnyTag("Genre:Mystery", "Mystery", "Thriller", "Crime") ||
                    book.titleContains("mystery") ||
                    book.titleContains("thriller") ||
                    book.titleContains("detective")
            },
        ),
        VirtualLibrary(
            id = "asimov",
            name = "Asimov",
            description = "Isaac Asimov",
            matcher = { book -> book.authorContains("Asimov") },
        ),
        VirtualLibrary(
            id = "downloaded",
            name = "On Device",
            description = "Downloaded books",
            matcher = { book -> book.isDownloaded },
        ),
        VirtualLibrary(
            id = "untagged",
            name = "Untagged",
            description = "Books with no tags",
            matcher = { book -> book.tagList().isEmpty() },
        ),
    )

    fun byId(id: String): VirtualLibrary =
        all.firstOrNull { it.id == id } ?: all.first()
}

fun Book.tagList(): List<String> =
    tags.split(',', ';', '|')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

fun Book.hasTag(tag: String): Boolean {
    val needle = tag.trim().lowercase()
    return tagList().any { it.equals(needle, ignoreCase = true) || it.lowercase() == needle }
}

fun Book.hasAnyTag(vararg tags: String): Boolean = tags.any { hasTag(it) }

fun Book.titleContains(value: String): Boolean =
    title.contains(value, ignoreCase = true)

fun Book.authorContains(value: String): Boolean =
    author.contains(value, ignoreCase = true)

fun Book.seriesContains(value: String): Boolean =
    series.contains(value, ignoreCase = true)

/**
 * Prefer cleaner Genre:/Status:/Franchise: tags in the chip row.
 */
fun preferredTagChips(books: List<Book>, limit: Int = 24): List<String> {
    val counts = linkedMapOf<String, Int>()
    for (book in books) {
        for (tag in book.tagList()) {
            counts[tag] = (counts[tag] ?: 0) + 1
        }
    }

    fun rank(tag: String): Int {
        val t = tag.lowercase()
        return when {
            t.startsWith("genre:") -> 0
            t.startsWith("status:") -> 1
            t.startsWith("franchise:") -> 2
            t.startsWith("collection:") -> 3
            t in setOf("science fiction", "fantasy", "classics", "poetry", "romance", "mystery") -> 4
            t.startsWith("series ") -> 90
            else -> 50
        }
    }

    return counts.entries
        .sortedWith(
            compareBy<Map.Entry<String, Int>> { rank(it.key) }
                .thenByDescending { it.value }
                .thenBy { it.key.lowercase() },
        )
        .map { it.key }
        .filterNot { it.startsWith("Series ", ignoreCase = true) }
        .take(limit)
}
