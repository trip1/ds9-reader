package com.example.ds9reader.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.ImportEpubsUseCase
import com.example.ds9reader.domain.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the home screen.
 */
data class HomeUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val importProgress: ImportProgress? = null,
)

/**
 * Tracks the progress of an import operation.
 */
data class ImportProgress(
    val isRunning: Boolean = false,
    val message: String? = null,
)

/**
 * ScreenModel for the home/library screen.
 * Manages loading, observing the book library, and importing EPUBs.
 */
class HomeScreenModel(
    private val repository: LibraryRepository,
    private val importUseCase: ImportEpubsUseCase,
) : ScreenModel {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    fun loadBooks() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getAllBooks().fold(
                ifRight = { books ->
                    _uiState.update {
                        it.copy(books = books, isLoading = false)
                    }
                },
                ifLeft = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load library: ${error}"
                        )
                    }
                }
            )
        }
    }

    fun importFiles(filePaths: List<String>) {
        if (filePaths.isEmpty()) return

        screenModelScope.launch {
            _uiState.update {
                it.copy(
                    importProgress = ImportProgress(isRunning = true, message = "Importing ${filePaths.size} file(s)..."),
                    error = null
                )
            }

            val summary = importUseCase.importFiles(filePaths)

            _uiState.update {
                it.copy(
                    importProgress = if (summary.hasErrors) {
                        ImportProgress(
                            isRunning = false,
                            message = "Imported ${summary.succeeded}, failed ${summary.failed}"
                        )
                    } else null
                )
            }

            if (summary.hasErrors) {
                _uiState.update { state ->
                    state.copy(error = summary.errors.joinToString("\n"))
                }
            }

            // Reload books after import
            loadBooks()
        }
    }

    fun dismissImportProgress() {
        _uiState.update { it.copy(importProgress = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
