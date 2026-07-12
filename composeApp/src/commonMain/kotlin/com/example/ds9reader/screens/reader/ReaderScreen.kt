package com.example.ds9reader.screens.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.EpubDocument
import com.example.ds9reader.domain.LibraryError
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.OpenBookUseCase
import com.example.ds9reader.domain.SyncWithCalibreUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import arrow.core.Either


data class ReaderScreen(
    val bookId: String,
) : Screen {
    override val key = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val openBook = koinInject<OpenBookUseCase>()
        val repository = koinInject<LibraryRepository>()
        val sync = koinInject<SyncWithCalibreUseCase>()
        val scope = rememberCoroutineScope()

        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var book by remember { mutableStateOf<Book?>(null) }
        var document by remember { mutableStateOf<EpubDocument?>(null) }
        var chapterIndex by remember { mutableIntStateOf(0) }
        var fontScale by remember { mutableFloatStateOf(1f) }
        var showToc by remember { mutableStateOf(false) }
        var showFont by remember { mutableStateOf(false) }
        var controlsVisible by remember { mutableStateOf(true) }

        LaunchedEffect(bookId) {
            loading = true
            error = null
            when (val result = openBook.loadDocument(bookId)) {
                is Either.Left -> {
                    error = when (result.value) {
                        LibraryError.FileNotFound ->
                            "Book not downloaded yet. Go back and download from Calibre."
                        else -> result.value.toString()
                    }
                    loading = false
                }
                is Either.Right -> {
                    val (b, doc) = result.value
                    book = b
                    document = doc
                    chapterIndex = b.currentSpineIndex.coerceIn(0, (doc.chapters.size - 1).coerceAtLeast(0))
                    loading = false
                }
            }
        }

        LaunchedEffect(chapterIndex, document, book) {
            val b = book ?: return@LaunchedEffect
            val doc = document ?: return@LaunchedEffect
            if (doc.chapters.isEmpty()) return@LaunchedEffect
            val progress = if (doc.chapters.size <= 1) {
                0f
            } else {
                chapterIndex.toFloat() / (doc.chapters.size - 1).toFloat()
            }
            val chapter = doc.chapters[chapterIndex.coerceIn(0, doc.chapters.lastIndex)]
            repository.updateProgress(
                bookId = b.id,
                progress = progress.coerceIn(0f, 1f),
                spineIndex = chapterIndex,
                anchor = chapter.href,
            )
            delay(1200)
            sync.pushProgress(b.id)
        }

        ReaderContent(
            loading = loading,
            error = error,
            book = book,
            document = document,
            chapterIndex = chapterIndex,
            fontScale = fontScale,
            controlsVisible = controlsVisible,
            showToc = showToc,
            showFont = showFont,
            onBack = {
                scope.launch {
                    book?.let { sync.pushProgress(it.id) }
                    navigator.pop()
                }
            },
            onToggleControls = { controlsVisible = !controlsVisible },
            onPrev = { if (chapterIndex > 0) chapterIndex -= 1 },
            onNext = {
                val max = document?.chapters?.lastIndex ?: 0
                if (chapterIndex < max) chapterIndex += 1
            },
            onOpenToc = { showToc = true },
            onCloseToc = { showToc = false },
            onSelectChapter = {
                chapterIndex = it
                showToc = false
            },
            onOpenFont = { showFont = true },
            onCloseFont = { showFont = false },
            onFontScale = { fontScale = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    loading: Boolean,
    error: String?,
    book: Book?,
    document: EpubDocument?,
    chapterIndex: Int,
    fontScale: Float,
    controlsVisible: Boolean,
    showToc: Boolean,
    showFont: Boolean,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onOpenToc: () -> Unit,
    onCloseToc: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onOpenFont: () -> Unit,
    onCloseFont: () -> Unit,
    onFontScale: (Float) -> Unit,
) {
    Scaffold(
        topBar = {
            if (controlsVisible) {
                TopAppBar(
                    title = {
                        Column {
                            Text(book?.title ?: "Reading", maxLines = 1)
                            val chapterTitle = document?.chapters?.getOrNull(chapterIndex)?.title
                            if (chapterTitle != null) {
                                Text(chapterTitle, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenToc) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Table of contents")
                        }
                        IconButton(onClick = onOpenFont) {
                            Icon(Icons.Default.TextFields, contentDescription = "Text size")
                        }
                        DropdownMenu(expanded = showToc, onDismissRequest = onCloseToc) {
                            document?.chapters.orEmpty().forEachIndexed { idx, chapter ->
                                DropdownMenuItem(
                                    text = { Text("${idx + 1}. ${chapter.title}") },
                                    onClick = { onSelectChapter(idx) },
                                )
                            }
                        }
                        DropdownMenu(expanded = showFont, onDismissRequest = onCloseFont) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                            ) {
                                Text("Text size")
                                Slider(
                                    value = fontScale,
                                    onValueChange = onFontScale,
                                    valueRange = 0.85f..1.6f,
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (loading) {
                LoadingCenter()
            } else if (error != null) {
                ErrorCenter(error)
            } else if (document != null) {
                val chapter = document.chapters.getOrNull(chapterIndex)
                val progress = if (document.chapters.size <= 1) {
                    if (chapter == null) 0f else 1f
                } else {
                    chapterIndex.toFloat() / (document.chapters.size - 1).toFloat()
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (controlsVisible) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(chapterIndex) {
                                detectTapGestures { offset ->
                                    val third = size.width / 3f
                                    when {
                                        offset.x < third -> onPrev()
                                        offset.x > size.width - third -> onNext()
                                        else -> onToggleControls()
                                    }
                                }
                            }
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = chapter?.html.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = (18f * fontScale).sp,
                                lineHeight = (28f * fontScale).sp,
                            ),
                        )
                    }

                    if (controlsVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = onPrev, enabled = chapterIndex > 0) { Text("Previous") }
                            Text(
                                text = "${chapterIndex + 1} / ${document.chapters.size}",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            TextButton(
                                onClick = onNext,
                                enabled = chapterIndex < document.chapters.lastIndex,
                            ) { Text("Next") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCenter() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorCenter(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(24.dp),
        )
    }
}
