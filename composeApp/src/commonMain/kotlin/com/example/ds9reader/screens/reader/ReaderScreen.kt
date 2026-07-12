package com.example.ds9reader.screens.reader

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.layout.ContentScale
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
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.example.ds9reader.domain.Book
import com.example.ds9reader.domain.EpubBlock
import com.example.ds9reader.domain.EpubDocument
import com.example.ds9reader.domain.EpubImage
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
            val chapterCount = doc.chapters.size.coerceAtLeast(1)
            val safePageCount = pageCount.coerceAtLeast(1)
            val safePageIndex = pageIndex.coerceIn(0, safePageCount - 1)
            val chapterProgress = (safePageIndex + 1).toFloat() / safePageCount.toFloat()
            val progress = ((chapterIndex + chapterProgress) / chapterCount.toFloat()).coerceIn(0f, 1f)
            repository.updateProgress(
                bookId = b.id,
                progress = progress,
                spineIndex = chapterIndex,
                anchor = "page:$safePageIndex",
            )
            delay(1200)
            runCatching { sync.pushDirtyProgress() }
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
            onBack = { navigator.pop() },
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
                    val maxChapter = (document?.chapters?.lastIndex ?: 0)
                    if (chapterIndex < maxChapter) {
                        chapterIndex += 1
                        pageIndex = 0
                        preferLastPage = false
                    }
                }
            },
            onOpenToc = { showToc = true },
            onCloseToc = { showToc = false },
            onSelectChapter = { idx ->
                chapterIndex = idx
                pageIndex = 0
                preferLastPage = false
                showToc = false
            },
            onOpenFont = { showFont = true },
            onCloseFont = { showFont = false },
            onFontScale = {
                fontScale = it
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
                    val blocks = chapter?.blocks.orEmpty().ifEmpty {
                        listOf(EpubBlock.Text(chapter?.html.orEmpty()))
                    }
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
                            val imageMaxHeightPx = (usableHeight * 0.62f).toInt().coerceAtLeast(1)
                            val blockGapPx = with(density) { 12.dp.toPx() }.toInt()

                            val pages = remember(
                                blocks,
                                fontScale,
                                usableWidth,
                                usableHeight,
                                textStyle.fontSize,
                                textStyle.lineHeight,
                                document.images.keys,
                            ) {
                                paginateBlocks(
                                    blocks = blocks,
                                    style = textStyle,
                                    maxWidthPx = usableWidth,
                                    maxHeightPx = usableHeight,
                                    imageMaxHeightPx = imageMaxHeightPx,
                                    blockGapPx = blockGapPx,
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
                            val pageBlocks = pages.getOrElse(safeIndex) { emptyList() }

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
                                    .pointerInput(chapterIndex, safeIndex, computedCount, swipeThresholdPx) {
                                        var totalDrag = 0f
                                        detectHorizontalDragGestures(
                                            onDragStart = { totalDrag = 0f },
                                            onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                                            onDragEnd = {
                                                when {
                                                    totalDrag <= -swipeThresholdPx -> onNextPage()
                                                    totalDrag >= swipeThresholdPx -> onPrevPage()
                                                }
                                            },
                                            onDragCancel = { totalDrag = 0f },
                                        )
                                    },
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Top,
                                ) {
                                    pageBlocks.forEach { item ->
                                        when (item) {
                                            is PageItem.Text -> {
                                                Text(
                                                    text = item.text,
                                                    style = textStyle,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 12.dp),
                                                )
                                            }
                                            is PageItem.Image -> {
                                                val image = document.images[item.imageId]
                                                if (image != null) {
                                                    EpubInlineImage(
                                                        image = image,
                                                        alt = item.alt,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = with(density) { imageMaxHeightPx.toDp() })
                                                            .padding(bottom = 12.dp),
                                                    )
                                                } else if (item.alt.isNotBlank()) {
                                                    Text(
                                                        text = "[Image: ${item.alt}]",
                                                        style = textStyle,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(bottom = 12.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (controlsVisible) {
                            Surface(tonalElevation = 1.dp) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TextButton(onClick = onPrevPage, enabled = canPrev) { Text("Previous") }
                                    Text(
                                        "Ch ${chapterIndex + 1}/$chapterCount  •  Page ${pageIndex + 1}/$pageCount",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    TextButton(onClick = onNextPage, enabled = canNext) { Text("Next") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpubInlineImage(
    image: EpubImage,
    alt: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val request = remember(image.id, image.bytes.size) {
        ImageRequest.Builder(context)
            .data(image.bytes)
            .memoryCacheKey(image.id)
            .diskCacheKey(image.id)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = alt.ifBlank { "Illustration" },
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

private sealed interface PageItem {
    data class Text(val text: String) : PageItem
    data class Image(val imageId: String, val alt: String) : PageItem
}

private fun paginateBlocks(
    blocks: List<EpubBlock>,
    style: TextStyle,
    maxWidthPx: Int,
    maxHeightPx: Int,
    imageMaxHeightPx: Int,
    blockGapPx: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): List<List<PageItem>> {
    if (blocks.isEmpty()) return listOf(listOf(PageItem.Text("")))

    val pages = mutableListOf<MutableList<PageItem>>()
    var current = mutableListOf<PageItem>()
    var usedHeight = 0

    fun commitPage() {
        if (current.isNotEmpty()) {
            pages += current
            current = mutableListOf()
            usedHeight = 0
        }
    }

    fun remaining(): Int = (maxHeightPx - usedHeight).coerceAtLeast(0)

    fun addWithGap(height: Int) {
        if (current.isNotEmpty()) usedHeight += blockGapPx
        usedHeight += height
    }

    for (block in blocks) {
        when (block) {
            is EpubBlock.Image -> {
                val needed = imageMaxHeightPx + if (current.isNotEmpty()) blockGapPx else 0
                if (current.isNotEmpty() && needed > remaining()) {
                    commitPage()
                }
                // Image gets its own room; if still too tall for empty page, put it alone anyway.
                if (imageMaxHeightPx > maxHeightPx && current.isNotEmpty()) {
                    commitPage()
                }
                current += PageItem.Image(block.imageId, block.alt)
                addWithGap(imageMaxHeightPx.coerceAtMost(maxHeightPx))
                // Keep images from crowding text too tightly on same page when they consume most space.
                if (usedHeight >= (maxHeightPx * 0.78f).toInt()) {
                    commitPage()
                }
            }
            is EpubBlock.Text -> {
                var remainingText = block.text.trim()
                if (remainingText.isEmpty()) continue

                while (remainingText.isNotEmpty()) {
                    val avail = remaining()
                    if (avail <= 0) {
                        commitPage()
                        continue
                    }
                    val fit = fitTextToHeight(
                        text = remainingText,
                        style = style,
                        maxWidthPx = maxWidthPx,
                        maxHeightPx = avail,
                        textMeasurer = textMeasurer,
                    )
                    if (fit.consumedChars <= 0) {
                        // Nothing fits in remaining space; new page.
                        if (current.isEmpty()) {
                            // Force at least one character to avoid infinite loop on tiny screens.
                            val forced = remainingText.take(1)
                            current += PageItem.Text(forced)
                            remainingText = remainingText.drop(1).trimStart()
                            commitPage()
                        } else {
                            commitPage()
                        }
                        continue
                    }
                    val chunk = remainingText.substring(0, fit.consumedChars).trimEnd()
                    if (chunk.isNotEmpty()) {
                        current += PageItem.Text(chunk)
                        addWithGap(fit.heightPx)
                    }
                    remainingText = remainingText.substring(fit.consumedChars).trimStart()
                    if (remainingText.isNotEmpty()) {
                        commitPage()
                    }
                }
            }
        }
    }
    commitPage()
    return pages.ifEmpty { listOf(mutableListOf(PageItem.Text(""))) }
}

private data class FitResult(
    val consumedChars: Int,
    val heightPx: Int,
)

private fun fitTextToHeight(
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
    maxHeightPx: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): FitResult {
    if (text.isEmpty() || maxHeightPx <= 0) return FitResult(0, 0)
    var low = 0
    var high = text.length
    var best = 0
    var bestHeight = 0
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (mid <= 0) {
            low = 1
            continue
        }
        // Prefer breaking near whitespace when shrinking.
        var end = mid
        if (end < text.length) {
            val slice = text.substring(0, end)
            val ws = max(
                slice.lastIndexOf(' '),
                max(slice.lastIndexOf('\n'), slice.lastIndexOf('\t')),
            )
            if (ws >= (end * 0.6f).toInt()) {
                end = ws
            }
        }
        if (end <= 0) {
            high = mid - 1
            continue
        }
        val candidate = text.substring(0, end).trimEnd()
        val layout = textMeasurer.measure(
            text = candidate,
            style = style,
            constraints = Constraints(maxWidth = maxWidthPx),
        )
        if (layout.size.height <= maxHeightPx) {
            best = end
            bestHeight = layout.size.height
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    if (best <= 0) return FitResult(0, 0)
    // Consume following whitespace so next page starts cleanly.
    var consumed = best
    while (consumed < text.length && text[consumed].isWhitespace()) {
        consumed++
    }
    return FitResult(consumedChars = consumed, heightPx = bestHeight)
}
