package com.example.ds9reader.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.CalibreConfig
import com.example.ds9reader.domain.VirtualLibrary
import com.example.ds9reader.domain.tagList
import com.example.ds9reader.ui.CoverImage
import com.example.ds9reader.ui.EmptyState
import com.example.ds9reader.ui.LoadingState
import com.example.ds9reader.ui.SectionLabel
import com.example.ds9reader.ui.StatusBanner

@Composable
fun LibraryTabContent(
    state: HomeUiState,
    model: HomeScreenModel,
    showSettings: Boolean,
    onShowSettings: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    onOpenBook: (Book) -> Unit,
) {
    if (showSettings) {
        CalibreSettingsDialog(
            config = state.config,
            onDismiss = { onShowSettings(false) },
            onSave = {
                model.saveConfig(it)
                onShowSettings(false)
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (state.error != null) {
            StatusBanner(message = state.error ?: "", isError = true)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (state.statusMessage != null) {
            StatusBanner(message = state.statusMessage ?: "", isError = false)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = if (state.config.isConfigured) {
                "Calibre · ${state.config.baseUrl}"
            } else {
                "Connect your Calibre Content Server to download books and sync progress."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> LoadingState("Loading library...")
            state.books.isEmpty() -> EmptyState(
                title = "Library is empty",
                message = "Sync with Calibre to import your books.",
                action = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = model::syncLibrary) { Text("Sync Calibre library") }
                        TextButton(onClick = { onShowSettings(true) }) { Text("Server settings") }
                    }
                },
            )
            else -> {
                VirtualLibraryRow(
                    libraries = state.virtualLibraries,
                    selectedId = state.selectedLibraryId,
                    counts = state.libraryCounts,
                    onSelect = model::setVirtualLibrary,
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (state.availableTags.isNotEmpty()) {
                    TagFilterRow(
                        tags = state.availableTags,
                        selectedTag = state.selectedTag,
                        onSelect = model::setTagFilter,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                SortRow(
                    sort = state.sort,
                    visibleCount = state.filteredBooks.size,
                    totalCount = state.books.size,
                    selectedLibrary = state.selectedLibrary.name,
                    selectedTag = state.selectedTag,
                    onSort = model::setSort,
                    onClear = model::clearFilters,
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (state.filteredBooks.isEmpty()) {
                    EmptyState(
                        title = "No matches",
                        message = "No books match this virtual library / tag filter.",
                        action = {
                            TextButton(onClick = model::clearFilters) { Text("Clear filters") }
                        },
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp),
                    ) {
                        items(state.filteredBooks, key = { it.id }) { book ->
                            BookRow(
                                book = book,
                                config = state.config,
                                downloading = book.id in state.downloadingIds,
                                onOpen = { onOpenBook(book) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleBookListContent(
    title: String,
    subtitle: String,
    books: List<Book>,
    state: HomeUiState,
    model: HomeScreenModel,
    emptyText: String,
    contentPadding: PaddingValues,
    onOpenBook: (Book) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (state.error != null) {
            StatusBanner(message = state.error ?: "", isError = true)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (state.statusMessage != null) {
            StatusBanner(message = state.statusMessage ?: "", isError = false)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${books.size} books",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> LoadingState()
            books.isEmpty() -> EmptyState(
                title = title,
                message = emptyText,
            )
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(books, key = { it.id }) { book ->
                        BookRow(
                            book = book,
                            config = state.config,
                            downloading = book.id in state.downloadingIds,
                            onOpen = { onOpenBook(book) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualLibraryRow(
    libraries: List<VirtualLibrary>,
    selectedId: String,
    counts: Map<String, Int>,
    onSelect: (String) -> Unit,
) {
    Column {
        SectionLabel("Virtual libraries")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            libraries.forEach { library ->
                val count = counts[library.id] ?: 0
                FilterChip(
                    selected = selectedId == library.id,
                    onClick = { onSelect(library.id) },
                    label = { Text("${library.name} ($count)") },
                )
            }
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<String>,
    selectedTag: String?,
    onSelect: (String?) -> Unit,
) {
    Column {
        SectionLabel("Tags")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = selectedTag.equals(tag, ignoreCase = true),
                    onClick = { onSelect(tag) },
                    label = { Text(tag) },
                )
            }
        }
    }
}

@Composable
private fun SortRow(
    sort: LibrarySort,
    visibleCount: Int,
    totalCount: Int,
    selectedLibrary: String,
    selectedTag: String?,
    onSort: (LibrarySort) -> Unit,
    onClear: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$visibleCount of $totalCount",
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
            FilterChip(
                selected = sort == LibrarySort.Recent,
                onClick = { onSort(LibrarySort.Recent) },
                label = { Text("Recent") },
            )
        }
        val filterSummary = buildString {
            append(selectedLibrary)
            if (!selectedTag.isNullOrBlank()) append(" · tag: $selectedTag")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = filterSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selectedLibrary != "All Books" || !selectedTag.isNullOrBlank()) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
    }
}

@Composable
fun BookRow(
    book: Book,
    config: CalibreConfig,
    downloading: Boolean,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
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
                val tags = book.tagList().take(3)
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (book.progress > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { book.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${(book.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(onClick = onOpen) {
                    Text(
                        when {
                            downloading -> "Downloading..."
                            book.isDownloaded -> "Continue"
                            else -> "Download"
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun CalibreSettingsDialog(
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
