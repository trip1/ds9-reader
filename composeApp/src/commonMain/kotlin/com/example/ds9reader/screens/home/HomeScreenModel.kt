package com.example.ds9reader.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.CalibreConfig
import com.example.ds9reader.domain.LibraryError
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.SyncWithCalibreUseCase
import com.example.ds9reader.domain.VirtualLibraries
import com.example.ds9reader.domain.VirtualLibrary
import com.example.ds9reader.domain.hasTag
import com.example.ds9reader.domain.preferredTagChips
import com.example.ds9reader.domain.tagList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import arrow.core.Either

enum class LibrarySort {
    Title,
    Author,
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val continueReading: List<Book> = emptyList(),
    val config: CalibreConfig = CalibreConfig(),
    val sort: LibrarySort = LibrarySort.Title,
    val selectedLibraryId: String = "all",
    val selectedTag: String? = null,
    val availableTags: List<String> = emptyList(),
    val statusMessage: String? = null,
    val error: String? = null,
    val isSyncing: Boolean = false,
    val downloadingIds: Set<String> = emptySet(),
) {
    val selectedLibrary: VirtualLibrary
        get() = VirtualLibraries.byId(selectedLibraryId)

    val virtualLibraries: List<VirtualLibrary>
        get() = VirtualLibraries.all

    val filteredBooks: List<Book>
        get() {
            var list = books.asSequence()
            val library = selectedLibrary
            if (library.id != "all") {
                list = list.filter(library.matcher)
            }
            val tag = selectedTag
            if (!tag.isNullOrBlank()) {
                list = list.filter { it.hasTag(tag) }
            }
            val sorted = when (sort) {
                LibrarySort.Title -> list.sortedWith { a, b ->
                    val titleCmp = a.title.ifBlank { "Untitled" }
                        .compareTo(b.title.ifBlank { "Untitled" }, ignoreCase = true)
                    if (titleCmp != 0) titleCmp else a.author.compareTo(b.author, ignoreCase = true)
                }
                LibrarySort.Author -> list.sortedWith { a, b ->
                    val authorCmp = a.author.ifBlank { "Unknown author" }
                        .compareTo(b.author.ifBlank { "Unknown author" }, ignoreCase = true)
                    if (authorCmp != 0) authorCmp else a.title.compareTo(b.title, ignoreCase = true)
                }
            }
            return sorted.toList()
        }

    val libraryCounts: Map<String, Int>
        get() = virtualLibraries.associate { vl ->
            vl.id to if (vl.id == "all") books.size else books.count(vl.matcher)
        }
}

class HomeScreenModel(
    private val repository: LibraryRepository,
    private val syncUseCase: SyncWithCalibreUseCase,
) : ScreenModel {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val booksResult = repository.getAllBooks()) {
                is Either.Left -> {
                    _uiState.update { it.copy(isLoading = false, error = booksResult.value.toMessage()) }
                    return@launch
                }
                is Either.Right -> {
                    val cont = when (val c = repository.getContinueReading()) {
                        is Either.Right -> c.value
                        is Either.Left -> emptyList()
                    }
                    val config = when (val c = repository.getCalibreConfig()) {
                        is Either.Right -> c.value
                        is Either.Left -> CalibreConfig()
                    }
                    val books = booksResult.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            books = books,
                            continueReading = cont,
                            config = config,
                            availableTags = preferredTagChips(books),
                        )
                    }
                }
            }
        }
    }

    fun setSort(sort: LibrarySort) {
        _uiState.update { it.copy(sort = sort) }
    }

    fun setVirtualLibrary(libraryId: String) {
        _uiState.update { it.copy(selectedLibraryId = libraryId) }
    }

    fun setTagFilter(tag: String?) {
        _uiState.update { current ->
            val next = if (tag != null && current.selectedTag.equals(tag, ignoreCase = true)) {
                null
            } else {
                tag
            }
            current.copy(selectedTag = next)
        }
    }

    fun clearFilters() {
        _uiState.update { it.copy(selectedLibraryId = "all", selectedTag = null) }
    }

    fun saveConfig(config: CalibreConfig) {
        screenModelScope.launch {
            when (val result = repository.saveCalibreConfig(config)) {
                is Either.Left -> _uiState.update { it.copy(error = result.value.toMessage()) }
                is Either.Right -> _uiState.update {
                    it.copy(config = config, statusMessage = "Calibre settings saved")
                }
            }
        }
    }

    fun syncLibrary() {
        screenModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null, statusMessage = "Syncing with Calibre...") }
            when (val result = syncUseCase.syncLibrary()) {
                is Either.Left -> {
                    _uiState.update {
                        it.copy(isSyncing = false, error = result.value.toMessage(), statusMessage = null)
                    }
                }
                is Either.Right -> {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            statusMessage = "Synced ${result.value} books from Calibre",
                        )
                    }
                    refresh()
                    syncUseCase.pushDirtyProgress()
                }
            }
        }
    }

    fun download(bookId: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(downloadingIds = it.downloadingIds + bookId, error = null) }
            when (val result = syncUseCase.downloadBook(bookId)) {
                is Either.Left -> {
                    _uiState.update {
                        it.copy(
                            downloadingIds = it.downloadingIds - bookId,
                            error = result.value.toMessage(),
                        )
                    }
                }
                is Either.Right -> {
                    _uiState.update {
                        it.copy(
                            downloadingIds = it.downloadingIds - bookId,
                            statusMessage = "Downloaded \"${result.value.title}\"",
                        )
                    }
                    refresh()
                }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(statusMessage = null, error = null) }
    }

    private fun LibraryError.toMessage(): String = when (this) {
        is LibraryError.Auth -> message
        is LibraryError.Config -> message
        is LibraryError.Network -> message
        is LibraryError.NotFound -> "Book not found: $bookId"
        is LibraryError.Parse -> message
        is LibraryError.Storage -> message
        LibraryError.FileNotFound -> "Book file not found. Download it from Calibre first."
    }
}
