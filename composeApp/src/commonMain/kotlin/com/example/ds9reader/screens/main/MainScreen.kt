package com.example.ds9reader.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.SyncWithCalibreUseCase
import com.example.ds9reader.domain.ThemeMode
import com.example.ds9reader.screens.home.HomeScreenModel
import com.example.ds9reader.screens.home.LibraryTabContent
import com.example.ds9reader.screens.home.SimpleBookListContent
import com.example.ds9reader.screens.reader.ReaderScreen
import org.koin.compose.koinInject

enum class MainTab {
    Library,
    Reading,
    Downloaded,
}

data class MainScreen(
    val themeMode: ThemeMode,
    val onCycleTheme: () -> Unit,
) : Screen {
    override val key: String = "main"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val repository = koinInject<LibraryRepository>()
        val syncUseCase = koinInject<SyncWithCalibreUseCase>()
        val model = rememberScreenModel { HomeScreenModel(repository, syncUseCase) }
        val state by model.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        var tab by remember { mutableStateOf(MainTab.Library) }
        var showSettings by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (tab) {
                                MainTab.Library -> "Library"
                                MainTab.Reading -> "Currently Reading"
                                MainTab.Downloaded -> "Downloaded"
                            },
                        )
                    },
                    actions = {
                        IconButton(onClick = onCycleTheme) {
                            Icon(
                                imageVector = when (themeMode) {
                                    ThemeMode.System -> Icons.Outlined.SettingsBrightness
                                    ThemeMode.Light -> Icons.Outlined.LightMode
                                    ThemeMode.Dark -> Icons.Outlined.DarkMode
                                },
                                contentDescription = "Theme: ${themeMode.name}",
                            )
                        }
                        TextButton(onClick = model::syncLibrary, enabled = !state.isSyncing) {
                            Text(if (state.isSyncing) "Syncing..." else "Sync")
                        }
                        if (tab == MainTab.Library) {
                            TextButton(onClick = { showSettings = true }) {
                                Text("Calibre")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == MainTab.Library,
                        onClick = { tab = MainTab.Library },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Library") },
                        label = { Text("Library") },
                    )
                    NavigationBarItem(
                        selected = tab == MainTab.Reading,
                        onClick = { tab = MainTab.Reading },
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Reading") },
                        label = {
                            val count = state.readingBooks.size
                            Text(if (count > 0) "Reading ($count)" else "Reading")
                        },
                    )
                    NavigationBarItem(
                        selected = tab == MainTab.Downloaded,
                        onClick = { tab = MainTab.Downloaded },
                        icon = { Icon(Icons.Default.Download, contentDescription = "Downloaded") },
                        label = {
                            val count = state.downloadedBooks.size
                            Text(if (count > 0) "Downloaded ($count)" else "Downloaded")
                        },
                    )
                }
            },
        ) { padding ->
            when (tab) {
                MainTab.Library -> {
                    LibraryTabContent(
                        state = state,
                        model = model,
                        showSettings = showSettings,
                        onShowSettings = { showSettings = it },
                        contentPadding = padding,
                        onOpenBook = { book ->
                            if (book.isDownloaded) {
                                navigator.push(ReaderScreen(book.id))
                            } else {
                                model.download(book.id)
                            }
                        },
                    )
                }
                MainTab.Reading -> {
                    SimpleBookListContent(
                        title = "Currently Reading",
                        subtitle = "Books in progress on this device",
                        books = state.readingBooks,
                        state = state,
                        model = model,
                        emptyText = "Nothing in progress yet. Open a downloaded book to start reading.",
                        contentPadding = padding,
                        onOpenBook = { book ->
                            if (book.isDownloaded) {
                                navigator.push(ReaderScreen(book.id))
                            } else {
                                model.download(book.id)
                            }
                        },
                    )
                }
                MainTab.Downloaded -> {
                    SimpleBookListContent(
                        title = "Downloaded",
                        subtitle = "Books available offline",
                        books = state.downloadedBooks,
                        state = state,
                        model = model,
                        emptyText = "No downloaded books yet. Sync your library and download titles from Library.",
                        contentPadding = padding,
                        onOpenBook = { book ->
                            navigator.push(ReaderScreen(book.id))
                        },
                    )
                }
            }
        }
    }
}
