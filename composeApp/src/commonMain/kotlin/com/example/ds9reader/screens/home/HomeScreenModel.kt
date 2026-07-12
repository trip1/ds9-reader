package com.example.ds9reader.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.CalibreConfig
import com.example.ds9reader.domain.LibraryError
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.SyncWithCalibreUseCase
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
    val statusMessage: String? = null,
    val error: String? = null,
    val isSyncing: Boolean = false,
    val downloadingIds: Set<String> = emptySet(),
) {
    val sortedBooks: List<Book>
        get() = when (sort) {
            LibrarySort.Title -> books.sortedWith { a, b ->
                val titleCmp = a.title.ifBlank { "Untitled" }.compareTo(b.title.ifBlank { "Untitled" }, ignoreCase = true)
                if (titleCmp != 0) titleCmp
                else a.author.compareTo(b.author, ignoreCase = true)
            }
            LibrarySort.Author -> books.sortedWith { a, b ->
                val authorCmp = a.author.ifBlank { "Unknown author" }
                    .compareTo(b.author.ifBlank { "Unknown author" }, ignoreCase = true)
                if (authorCmp != 0) authorCmp
                else a.title.compareTo(b.title, ignoreCase = true)
            }
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            books = booksResult.value,
                            continueReading = cont,
                            config = config,
                        )
                    }
                }
            }
        }
    }

    fun setSort(sort: LibrarySort) {
        _uiState.update { it.copy(sort = sort) }
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
