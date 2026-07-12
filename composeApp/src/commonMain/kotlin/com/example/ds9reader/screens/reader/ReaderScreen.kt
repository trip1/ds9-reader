package com.example.ds9reader.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.EpubDocument
import com.example.ds9reader.domain.LibraryError
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.OpenBookUseCase
import com.example.ds9reader.domain.SyncWithCalibreUseCase
import com.example.ds9reader.ui.EmptyState
import com.example.ds9reader.ui.LoadingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import arrow.core.Either
import kotlin.math.abs
import kotlin.math.max

data class ReaderScreen(
    val bookId: String,
) : Screen {
    override val key: String = "reader-$bookId"

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
        var pageIndex by remember { mutableIntStateOf(0) }
        var pageCount by remember { mutableIntStateOf(1) }
        var preferLastPage by remember { mutableStateOf(false) }
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
                    pageIndex = 0
                    preferLastPage = false
                    loading = false
                }
            }
        }

        LaunchedEffect(chapterIndex, pageIndex, pageCount, document, book) {
            val b = book ?: return@LaunchedEffect
            val doc = document ?: return@LaunchedEffect
            if (doc.chapters.isEmpty()) return@LaunchedEffect
            val chapterCount = doc.chapters.size
            val safePageCount = pageCount.coerceAtLeast(1)
            val safePageIndex = pageIndex.coerceIn(0, safePageCount - 1)
            val chapterProgress = (safePageIndex + 1).toFloat() / safePageCount.toFloat()
            val progress = if (chapterCount <= 1) {
                chapterProgress
            } else {
                val base = chapterIndex.toFloat() / chapterCount.toFloat()
                val span = 1f / chapterCount.toFloat()
                base + span * chapterProgress
            }
            val chapter = doc.chapters[chapterIndex.coerceIn(0, doc.chapters.lastIndex)]
            repository.updateProgress(
                bookId = b.id,
                progress = progress.coerceIn(0f, 1f),
                spineIndex = chapterIndex,
                anchor = "${chapter.href}#p$safePageIndex",
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
            pageIndex = pageIndex,
            pageCount = pageCount,
            preferLastPage = preferLastPage,
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
            onPrevPage = {
                if (pageIndex > 0) {
                    pageIndex -= 1
                    preferLastPage = false
                } else if (chapterIndex > 0) {
                    chapterIndex -= 1
                    preferLastPage = true
                }
            },
            onNextPage = {
                if (pageIndex < pageCount - 1) {
                    pageIndex += 1
                    preferLastPage = false
                } else {
                    val maxChapter = document?.chapters?.lastIndex ?: 0
                    if (chapterIndex < maxChapter) {
                        chapterIndex += 1
                        pageIndex = 0
                        preferLastPage = false
                    }
                }
            },
            onOpenToc = { showToc = true },
            onCloseToc = { showToc = false },
            onSelectChapter = {
                chapterIndex = it
                pageIndex = 0
                preferLastPage = false
                showToc = false
            },
            onOpenFont = { showFont = true },
            onCloseFont = { showFont = false },
            onFontScale = {
                fontScale = it
                pageIndex = 0
                preferLastPage = false
            },
            onPaginationChanged = { newPageCount, desiredIndex ->
                pageCount = newPageCount
                pageIndex = desiredIndex
            },
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
    pageIndex: Int,
    pageCount: Int,
    preferLastPage: Boolean,
    fontScale: Float,
    controlsVisible: Boolean,
    showToc: Boolean,
    showFont: Boolean,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenToc: () -> Unit,
    onCloseToc: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onOpenFont: () -> Unit,
    onCloseFont: () -> Unit,
    onFontScale: (Float) -> Unit,
    onPaginationChanged: (pageCount: Int, pageIndex: Int) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (controlsVisible) {
                TopAppBar(
                    title = {
                        Column {
                            Text(book?.title ?: "Reading", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val chapterTitle = document?.chapters?.getOrNull(chapterIndex)?.title
                            if (chapterTitle != null) {
                                Text(
                                    chapterTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                loading -> LoadingState("Opening book...")
                error != null -> EmptyState(
                    title = "Unable to open",
                    message = error,
                    action = {
                        TextButton(onClick = onBack) { Text("Go back") }
                    },
                )
                document != null -> {
                    val chapterCount = document.chapters.size.coerceAtLeast(1)
                    val chapter = document.chapters.getOrNull(chapterIndex)
                    val chapterText = chapter?.html.orEmpty()
                    val density = LocalDensity.current
                    val swipeThresholdPx = with(density) { 56.dp.toPx() }
                    val textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (18f * fontScale).sp,
                        lineHeight = (28f * fontScale).sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val textMeasurer = rememberTextMeasurer()
                    val canPrev = chapterIndex > 0 || pageIndex > 0
                    val canNext = chapterIndex < chapterCount - 1 || pageIndex < pageCount - 1
                    val overallProgress = if (chapterCount <= 0) {
                        0f
                    } else {
                        val safePageCount = pageCount.coerceAtLeast(1)
                        val safePageIndex = pageIndex.coerceIn(0, safePageCount - 1)
                        val chapterProgress = (safePageIndex + 1).toFloat() / safePageCount.toFloat()
                        val base = chapterIndex.toFloat() / chapterCount.toFloat()
                        val span = 1f / chapterCount.toFloat()
                        base + span * chapterProgress
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (controlsVisible) {
                            LinearProgressIndicator(
                                progress = { overallProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            val contentWidthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
                            val contentHeightPx = with(density) { maxHeight.toPx() }.toInt().coerceAtLeast(1)
                            val horizontalPadPx = with(density) { 22.dp.toPx() }.toInt()
                            val verticalPadPx = with(density) { 18.dp.toPx() }.toInt()
                            val usableWidth = (contentWidthPx - horizontalPadPx * 2).coerceAtLeast(1)
                            val usableHeight = (contentHeightPx - verticalPadPx * 2).coerceAtLeast(1)

                            val pages = remember(
                                chapterText,
                                fontScale,
                                usableWidth,
                                usableHeight,
                                textStyle.fontSize,
                                textStyle.lineHeight,
                            ) {
                                paginateText(
                                    text = chapterText,
                                    style = textStyle,
                                    maxWidthPx = usableWidth,
                                    maxHeightPx = usableHeight,
                                    textMeasurer = textMeasurer,
                                )
                            }
                            val computedCount = pages.size.coerceAtLeast(1)
                            val desiredIndex = if (preferLastPage) {
                                computedCount - 1
                            } else {
                                pageIndex.coerceIn(0, computedCount - 1)
                            }

                            LaunchedEffect(computedCount, desiredIndex, chapterIndex, fontScale, preferLastPage) {
                                if (computedCount != pageCount || desiredIndex != pageIndex) {
                                    onPaginationChanged(computedCount, desiredIndex)
                                }
                            }

                            val safeIndex = pageIndex.coerceIn(0, computedCount - 1)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 22.dp, vertical = 18.dp)
                                    .pointerInput(chapterIndex, safeIndex, computedCount) {
                                        detectTapGestures { offset ->
                                            val third = size.width / 3f
                                            when {
                                                offset.x < third -> onPrevPage()
                                                offset.x > size.width - third -> onNextPage()
                                                else -> onToggleControls()
                                            }
                                        }
                                    }
                                    .pointerInput(chapterIndex, safeIndex, computedCount) {
                                        var totalDrag = 0f
                                        detectHorizontalDragGestures(
                                            onDragStart = { totalDrag = 0f },
                                            onHorizontalDrag = { change, dragAmount ->
                                                totalDrag += dragAmount
                                                change.consume()
                                            },
                                            onDragEnd = {
                                                if (abs(totalDrag) >= swipeThresholdPx) {
                                                    if (totalDrag < 0f) onNextPage() else onPrevPage()
                                                }
                                                totalDrag = 0f
                                            },
                                            onDragCancel = { totalDrag = 0f },
                                        )
                                    },
                            ) {
                                Text(
                                    text = pages.getOrElse(safeIndex) { "" },
                                    style = textStyle,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        if (controlsVisible) {
                            Surface(tonalElevation = 2.dp) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TextButton(onClick = onPrevPage, enabled = canPrev) {
                                        Text("Previous")
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Ch ${chapterIndex + 1} / $chapterCount",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = "Page ${pageIndex + 1} / ${pageCount.coerceAtLeast(1)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(onClick = onNextPage, enabled = canNext) {
                                        Text("Next")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Split plain chapter text into fixed screen pages that fit without scrolling.
 */
private fun paginateText(
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
    maxHeightPx: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): List<String> {
    val cleaned = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim()
    if (cleaned.isEmpty()) return listOf("")

    val sample = textMeasurer.measure(
        text = "Ag",
        style = style,
        constraints = Constraints(maxWidth = maxWidthPx),
    )
    val lineHeight = sample.size.height.coerceAtLeast(1)
    val maxLines = max(1, maxHeightPx / lineHeight)

    val paragraphs = cleaned.split('\n')
    val lines = mutableListOf<String>()
    for (paragraph in paragraphs) {
        if (paragraph.isBlank()) {
            lines += ""
            continue
        }
        val words = paragraph.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) {
            lines += ""
            continue
        }
        var current = words.first()
        for (i in 1 until words.size) {
            val candidate = "$current ${words[i]}"
            val measured = textMeasurer.measure(
                text = candidate,
                style = style,
                constraints = Constraints(maxWidth = maxWidthPx),
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )
            if (measured.lineCount > 1) {
                lines += current
                current = words[i]
            } else {
                current = candidate
            }
        }
        lines += current
    }

    if (lines.isEmpty()) return listOf("")

    val pages = mutableListOf<String>()
    var index = 0
    while (index < lines.size) {
        var packEnd = (index + maxLines).coerceAtMost(lines.size)
        while (packEnd > index) {
            val pageText = lines.subList(index, packEnd).joinToString("\n")
            val measured = textMeasurer.measure(
                text = pageText,
                style = style,
                constraints = Constraints(maxWidth = maxWidthPx),
            )
            if (measured.size.height <= maxHeightPx || packEnd == index + 1) {
                pages += pageText
                index = packEnd
                break
            }
            packEnd -= 1
        }
        if (packEnd == index) {
            pages += lines[index]
            index += 1
        }
    }
    return pages.ifEmpty { listOf("") }
}
