package com.example.ds9reader.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.ImportEpubsUseCase
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.PickerResult
import com.example.ds9reader.screens.reader.ReaderScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

object HomeScreen : Screen {

    override val key = uniqueScreenKey

    @Composable
    override fun Content() {
        val repository = koinInject<LibraryRepository>()
        val importUseCase = koinInject<ImportEpubsUseCase>()
        val filePicker = koinInject<FilePicker>()

        val screenModel = rememberScreenModel { HomeScreenModel(repository, importUseCase) }
        val uiState by screenModel.uiState.collectAsState()
        val scope = rememberCoroutineScope()

        var showPickerMenu by remember { mutableStateOf(false) }
        var isPicking by remember { mutableStateOf(false) }

        HomeScreenContent(
            uiState = uiState,
            onRefresh = screenModel::loadBooks,
            onDismissError = screenModel::dismissError,
            onDismissImportProgress = screenModel::dismissImportProgress,
            showPickerMenu = showPickerMenu,
            onShowPickerMenu = { showPickerMenu = true },
            onHidePickerMenu = { showPickerMenu = false },
            onFilePick = {
                showPickerMenu = false
                scope.launch {
                    isPicking = true
                    val result = filePicker.pickEpubFile()
                    isPicking = false
                    if (result is PickerResult.Success) {
                        screenModel.importFiles(result.paths)
                    }
                }
            },
            onFolderPick = {
                showPickerMenu = false
                scope.launch {
                    isPicking = true
                    val result = filePicker.pickEpubDirectory()
                    isPicking = false
                    if (result is PickerResult.Success) {
                        screenModel.importFiles(result.paths)
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onDismissImportProgress: () -> Unit,
    showPickerMenu: Boolean,
    onShowPickerMenu: () -> Unit,
    onHidePickerMenu: () -> Unit,
    onFilePick: () -> Unit,
    onFolderPick: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DS-9 Reader") },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: open drawer */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onShowPickerMenu,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add EPUB") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MainContent(
                uiState = uiState,
                onRefresh = onRefresh,
                onDismissError = onDismissError,
                onDismissImportProgress = onDismissImportProgress,
                onBookClick = { book -> navigator.push(ReaderScreen(book.id)) },
            )

            if (showPickerMenu) {
                FilePickerDropdown(
                    onDismiss = onHidePickerMenu,
                    onFilePick = onFilePick,
                    onFolderPick = onFolderPick,
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onDismissImportProgress: () -> Unit,
    onBookClick: (Book) -> Unit,
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.importProgress != null -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (uiState.importProgress.isRunning) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.importProgress.message ?: "Importing...")
                } else {
                    Text(
                        text = uiState.importProgress.message ?: "Import complete",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismissImportProgress) {
                        Text("Done")
                    }
                }
            }
        }

        uiState.error != null -> {
            ErrorBanner(
                message = uiState.error,
                onDismiss = onDismissError,
                onRetry = onRefresh,
            )
        }

        uiState.books.isEmpty() -> {
            EmptyLibrary()
        }

        else -> {
            BookGrid(
                books = uiState.books,
                onBookClick = onBookClick,
            )
        }
    }
}

@Composable
private fun FilePickerDropdown(
    onDismiss: () -> Unit,
    onFilePick: () -> Unit,
    onFolderPick: () -> Unit,
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text("Select EPUB file") },
            onClick = onFilePick,
            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) }
        )
        Divider()
        DropdownMenuItem(
            text = { Text("Select folder of EPUBs") },
            onClick = onFolderPick,
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )
    }
}

@Composable
private fun BookGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        state = gridState,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(
                book = book,
                onClick = { onBookClick(book) }
            )
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            // Placeholder cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.67f)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = book.author.ifEmpty { "Unknown" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (book.progress > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "📚",
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your library is empty",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to add your first EPUB",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Snackbar(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        action = {
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Text(message)
    }
}
