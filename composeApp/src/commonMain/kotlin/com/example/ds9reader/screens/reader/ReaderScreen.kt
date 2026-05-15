package com.example.ds9reader.screens.reader

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.platform.openReader
import org.koin.compose.koinInject

/**
 * Reader screen — launches the platform-specific EPUB reader.
 * On Android, this opens the ReadingActivity with the Readium navigator.
 * On Desktop, this shows a placeholder until a custom reader is implemented.
 */
data class ReaderScreen(
    val bookId: String,
) : Screen {

    override val key = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<LibraryRepository>()
        
        var filePath by remember { mutableStateOf<String?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var launched by remember { mutableStateOf(false) }

        LaunchedEffect(bookId) {
            repository.getBook(bookId).fold(
                ifRight = { book ->
                    filePath = book.filePath
                },
                ifLeft = { err ->
                    error = "Failed to load book: $err"
                }
            )
        }

        LaunchedEffect(filePath) {
            if (filePath != null && !launched) {
                launched = true
                openReader(this@ReaderScreen, filePath!!)
            }
        }

        ReaderScreenContent(
            isLoading = filePath == null && error == null,
            error = error,
            onBack = { navigator.pop() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreenContent(
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: bookmarks */ }) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "Bookmark"
                        )
                    }
                    IconButton(onClick = { /* TODO: settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "❌",
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
