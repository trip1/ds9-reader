package com.example.ds9reader.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.CalibreConfig
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.SyncWithCalibreUseCase
import com.example.ds9reader.screens.reader.ReaderScreen
import com.example.ds9reader.ui.CoverImage
import org.koin.compose.koinInject

object HomeScreen : Screen {
    override val key: String = "home"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val repository = koinInject<LibraryRepository>()
        val syncUseCase = koinInject<SyncWithCalibreUseCase>()
        val model = rememberScreenModel { HomeScreenModel(repository, syncUseCase) }
        val state by model.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        var showSettings by remember { mutableStateOf(false) }

        if (showSettings) {
            CalibreSettingsDialog(
                config = state.config,
                onDismiss = { showSettings = false },
                onSave = {
                    model.saveConfig(it)
                    showSettings = false
                },
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("DS9 Reader") },
                    actions = {
                        TextButton(onClick = model::syncLibrary, enabled = !state.isSyncing) {
                            Text(if (state.isSyncing) "Syncing..." else "Sync")
                        }
                        TextButton(onClick = { showSettings = true }) { Text("Calibre") }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (state.error != null) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (state.statusMessage != null) {
                    Text(state.statusMessage ?: "")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    if (state.config.isConfigured) {
                        "Calibre: ${state.config.baseUrl}"
                    } else {
                        "Connect your Calibre Content Server to download books and sync progress."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.isLoading) {
                    Text("Loading library...")
                } else if (state.books.isEmpty()) {
                    Text("Library is empty")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = model::syncLibrary) { Text("Sync Calibre library") }
                    TextButton(onClick = { showSettings = true }) { Text("Server settings") }
                } else {
                    SortRow(
                        sort = state.sort,
                        bookCount = state.books.size,
                        onSort = model::setSort,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.sortedBooks, key = { it.id }) { book ->
                            BookRow(
                                book = book,
                                config = state.config,
                                downloading = book.id in state.downloadingIds,
                                onOpen = {
                                    if (book.isDownloaded) {
                                        navigator.push(ReaderScreen(book.id))
                                    } else {
                                        model.download(book.id)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortRow(
    sort: LibrarySort,
    bookCount: Int,
    onSort: (LibrarySort) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$bookCount books",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        FilterChip(
            selected = sort == LibrarySort.Title,
            onClick = { onSort(LibrarySort.Title) },
            label = { Text("Title") },
        )
        FilterChip(
            selected = sort == LibrarySort.Author,
            onClick = { onSort(LibrarySort.Author) },
            label = { Text("Author") },
        )
    }
}

@Composable
private fun BookRow(
    book: Book,
    config: CalibreConfig,
    downloading: Boolean,
    onOpen: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverImage(
                coverUrl = book.coverUrl,
                config = config,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = book.author.ifBlank { "Unknown author" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (book.progress > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { book.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${(book.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onOpen) {
                    Text(
                        when {
                            downloading -> "Downloading..."
                            book.isDownloaded -> "Continue reading"
                            else -> "Download"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibreSettingsDialog(
    config: CalibreConfig,
    onDismiss: () -> Unit,
    onSave: (CalibreConfig) -> Unit,
) {
    var baseUrl by remember(config) { mutableStateOf(config.baseUrl) }
    var username by remember(config) { mutableStateOf(config.username) }
    var password by remember(config) { mutableStateOf(config.password) }
    var libraryId by remember(config) { mutableStateOf(config.libraryId) }
    var deviceName by remember(config) { mutableStateOf(config.deviceName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calibre Content Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = libraryId,
                    onValueChange = { libraryId = it },
                    label = { Text("Library ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        CalibreConfig(
                            baseUrl = baseUrl.trim(),
                            username = username.trim(),
                            password = password,
                            libraryId = libraryId.trim(),
                            deviceName = deviceName.ifBlank { "DS9-Reader" },
                            lastSyncAt = config.lastSyncAt,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
