package com.blissless.oni.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blissless.oni.data.ChapterInfo
import com.blissless.oni.data.ReaderMode
import com.blissless.oni.viewmodel.MainViewModel
import com.blissless.oni.viewmodel.UiState
import com.blissless.oni.ui.theme.BlueAccent
import com.blissless.oni.ui.theme.BlueLight
import com.blissless.oni.ui.theme.ChapterCounterBg
import com.blissless.oni.ui.theme.CurrentBlueGlow
import com.blissless.oni.ui.theme.DarkBackground
import com.blissless.oni.ui.theme.DarkCard
import com.blissless.oni.ui.theme.DarkSurfaceVariant
import com.blissless.oni.ui.theme.GlassStroke
import com.blissless.oni.ui.theme.GlassStrokeFocused
import com.blissless.oni.ui.theme.GradientBlue
import com.blissless.oni.ui.theme.GradientPurple
import com.blissless.oni.ui.theme.ProgressTrackBg
import com.blissless.oni.ui.theme.ReadGreen
import com.blissless.oni.ui.theme.SearchBarBg
import com.blissless.oni.ui.theme.SilverDark
import com.blissless.oni.ui.theme.SilverLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val chapters by viewModel.chapters.collectAsState()
    val selectedIndex by viewModel.selectedChapterIndex.collectAsState()
    val chapterImages by viewModel.chapterImages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isChapterRead by viewModel.isChapterRead.collectAsState()
    val readChapterIndices by viewModel.readChapterIndices.collectAsState()
    val nextChapterToRead by viewModel.nextChapterToRead.collectAsState()
    val syncThreshold by viewModel.anilistSyncThreshold.collectAsState()
    val resumeScrollProgress by viewModel.resumeScrollProgress.collectAsState()
    val readerMode by viewModel.readerMode.collectAsState()
    val selectedExtensionAuthority by viewModel.selectedExtensionAuthority.collectAsState()

    val context = LocalContext.current

    // Tracks the page index the user is currently on while in a paged reader
    // mode. Updated by the PagedMangaReader's onPageChanged callback. Used to
    // render the "Page X / Y" indicator in the header and to feed scroll
    // progress back to the ViewModel for AniList sync.
    var currentPageIndex by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()
    var isShowingChapterList by remember { mutableStateOf(selectedIndex < 0) }
    var scrollProgress by remember { mutableStateOf(0f) }

    // Unified reading progress (0..1), preserved across mode switches so the
    // user doesn't lose their place when they switch between vertical scroll
    // and paged modes. Both modes write to this; the mode being switched INTO
    // reads from it to compute its initial position.
    var unifiedProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(selectedIndex) {
        isShowingChapterList = selectedIndex < 0
        if (selectedIndex < 0) {
            viewModel.refreshTrackingLists()
        }
        if (selectedIndex >= 0 && resumeScrollProgress < 0f) {
            listState.scrollToItem(0)
        }
        // Reset the paged-mode page index whenever the chapter changes, so
        // the new chapter starts at page 1 and the header indicator is correct.
        currentPageIndex = 0
        unifiedProgress = 0f
    }
    
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@snapshotFlow 0f
            if (totalItems <= 1) return@snapshotFlow 1f

            val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return@snapshotFlow 0f
            val itemSize = firstVisibleItem.size
            val viewportHeight = layoutInfo.viewportSize.height

            val currentScroll = listState.firstVisibleItemIndex.toFloat() * itemSize.toFloat() + listState.firstVisibleItemScrollOffset.toFloat()
            val maxScroll = (totalItems.toFloat() * itemSize.toFloat() - viewportHeight.toFloat()).coerceAtLeast(0f)
            if (maxScroll <= 0f) 0f else (currentScroll / maxScroll).coerceIn(0f, 1f)
        }.collect { progress: Float ->
            scrollProgress = progress
            unifiedProgress = progress
            viewModel.onChapterScrollProgress(progress)
        }
    }

    // Track the current page index in vertical mode by watching the
    // LazyColumn's firstVisibleItemIndex. This is the PAGE the user is
    // currently looking at (not a pixel-based progress), which is what we
    // need when switching to paged mode — pixel-based progress doesn't map
    // cleanly to page indices when pages have unequal heights.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> currentPageIndex = index }
    }

    // Scroll to saved resume position after images load
    LaunchedEffect(resumeScrollProgress, chapterImages) {
        if (resumeScrollProgress >= 0f && chapterImages is UiState.Success) {
            val images = (chapterImages as UiState.Success).data.images
            if (images.isNotEmpty()) {
                val targetIndex = (resumeScrollProgress * images.size).toInt()
                    .coerceIn(0, images.size - 1)
                delay(200)
                listState.scrollToItem(targetIndex)
                viewModel.clearResumeScrollProgress()
            }
        }
    }

    // When the reader mode changes mid-chapter, restore the user's reading
    // position using `currentPageIndex` — the actual page the user was
    // looking at, tracked by both modes (vertical via firstVisibleItemIndex,
    // paged via onPageChanged).
    //
    // We deliberately do NOT use `unifiedProgress` (pixel-based) here because
    // it doesn't map cleanly to page indices when manga pages have unequal
    // heights — e.g. being 50% scrolled through a chapter with 10 pages of
    // varying heights doesn't mean you're on page 5.
    LaunchedEffect(readerMode) {
        if (selectedIndex < 0) return@LaunchedEffect
        val images = (chapterImages as? UiState.Success)?.data?.images ?: return@LaunchedEffect
        if (images.isEmpty()) return@LaunchedEffect

        // Clamp to valid range for the new mode's content.
        val targetIndex = currentPageIndex.coerceIn(0, images.lastIndex)
        when (readerMode) {
            ReaderMode.VERTICAL_SCROLL -> {
                // Give the LazyColumn a frame to recompose with the new
                // content before we attempt to scroll.
                delay(50)
                if (targetIndex < listState.layoutInfo.totalItemsCount) {
                    listState.scrollToItem(targetIndex)
                }
            }
            ReaderMode.LEFT_TO_RIGHT, ReaderMode.RIGHT_TO_LEFT -> {
                // PagedMangaReader reads `initialPage` on first composition,
                // so updating currentPageIndex here makes the pager start at
                // the right page when it appears.
                currentPageIndex = targetIndex
            }
        }
    }
    
    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = Modifier.background(Color.Black),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Oni Reader",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selectedIndex >= 0 && selectedIndex < chapters.size) {
                                val pagePart = when {
                                    chapterImages is UiState.Success -> {
                                        val total = (chapterImages as UiState.Success).data.images.size
                                        if (total > 0) "  ·  Page ${currentPageIndex + 1}/$total" else ""
                                    }
                                    else -> ""
                                }
                                Text(
                                    text = (chapters[selectedIndex].title ?: "Chapter ${selectedIndex + 1}") + pagePart,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SilverDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                        if (!isShowingChapterList && selectedIndex >= 0) {
                            // Segmented reader-mode indicator. Three mini icons
                            // in a row; the active mode is highlighted with the
                            // app accent color, the others are dimmed. Tapping
                            // any of the three directly selects that mode — no
                            // more cycling through one button at a time.
                            //
                            // Icons chosen to match each mode's mental model:
                            //  - ViewAgenda      : stacked horizontal bars = vertical scroll
                            //  - ArrowForward    : rightward arrow = LTR (swipe left for next)
                            //  - ArrowBack       : leftward arrow = RTL (swipe right for next)
                            ReaderModeSegmentedToggle(
                                currentMode = readerMode,
                                onSelect = { viewModel.setReaderMode(it) }
                            )
                            IconButton(
                                onClick = {
                                    if (selectedIndex > 0) {
                                        viewModel.selectChapter(selectedIndex - 1)
                                    }
                                },
                                enabled = selectedIndex > 0
                            ) {
                                Icon(
                                    Icons.Default.SkipPrevious, 
                                    contentDescription = "Previous Chapter", 
                                    tint = if (selectedIndex > 0) Color.White else Color.White.copy(alpha = 0.3f)
                                )
                            }
                            IconButton(
                                onClick = { 
                                    if (selectedIndex < chapters.size - 1) {
                                        viewModel.selectChapter(selectedIndex + 1)
                                    }
                                },
                                enabled = selectedIndex < chapters.size - 1
                            ) {
                                Icon(
                                    Icons.Default.SkipNext, 
                                    contentDescription = "Next Chapter", 
                                    tint = if (selectedIndex < chapters.size - 1) Color.White else Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.95f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
                if (!isShowingChapterList && selectedIndex >= 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(scrollProgress)
                                .height(3.dp)
                                .background(BlueAccent)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(syncThreshold / 100f)
                                .fillMaxHeight()
                                .background(Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BlueAccent)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading...", color = Color.White)
                    }
                }
            }

            isShowingChapterList || selectedIndex < 0 -> {
                ChapterListWithGroups(
                    chapters = chapters,
                    selectedIndex = selectedIndex,
                    readChapterIndices = readChapterIndices,
                    nextChapterToRead = nextChapterToRead,
                    onChapterClick = { 
                        viewModel.selectChapter(it)
                        isShowingChapterList = false
                    },
                    onContinueReading = {
                        if (selectedExtensionAuthority == null) {
                            Toast.makeText(context, "Select a default extension in Settings first", Toast.LENGTH_SHORT).show()
                        } else {
                            nextChapterToRead?.let { idx ->
                                viewModel.selectChapter(idx)
                                isShowingChapterList = false
                            }
                        }
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            chapterImages is UiState.Success -> {
                val images = (chapterImages as UiState.Success).data.images
                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No images found", color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.selectChapter(selectedIndex) }) {
                                Text("Retry")
                            }
                        }
                    }
                } else when (readerMode) {
                    // ---------------- Vertical scroll (webtoon) ----------------
                    // Original behaviour: continuous LazyColumn, one page per row.
                    // Pages use MihonZoomableImage so the user can pinch-zoom a
                    // specific page even in vertical mode, while one-finger drag
                    // at 1× still scrolls the list.
                    ReaderMode.VERTICAL_SCROLL -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(images, key = { index, _ -> "page_$index" }) { index, imageUrl ->
                                MihonZoomableImage(
                                    imageUrl = imageUrl,
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp),
                                    fillWidth = true,
                                    gesturesEnabled = false
                                )
                            }
                        }
                    }

                    // ---------------- Paged (LTR / RTL) ----------------
                    // One page per screen, swipe horizontally to navigate.
                    // Single tap = next page (with a small back-edge zone),
                    // double tap = zoom in/out at the tap point.
                    ReaderMode.LEFT_TO_RIGHT, ReaderMode.RIGHT_TO_LEFT -> {
                        PagedMangaReader(
                            images = images,
                            initialPage = currentPageIndex.coerceIn(0, images.lastIndex),
                            mode = readerMode,
                            chapterTitle = if (selectedIndex in chapters.indices)
                                chapters[selectedIndex].title else null,
                            onPageChanged = { page ->
                                currentPageIndex = page
                                // Feed the pager's position into the same
                                // scroll-progress machinery the vertical list
                                // uses, so AniList "read" sync still fires at
                                // the configured threshold.
                                val progress = if (images.size > 1) {
                                    page.toFloat() / (images.size - 1).toFloat()
                                } else 1f
                                scrollProgress = progress
                                unifiedProgress = progress
                                viewModel.onChapterScrollProgress(progress)
                            },
                            onChapterBoundary = { direction ->
                                val target = selectedIndex + direction
                                if (target in chapters.indices) {
                                    viewModel.selectChapter(target)
                                    // Reset page index for the new chapter so
                                    // the header indicator starts at 1/total.
                                    currentPageIndex = if (direction > 0) 0 else 0
                                }
                            },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }

            chapterImages is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Error: ${(chapterImages as UiState.Error).message}",
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.selectChapter(selectedIndex) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                ChapterListWithGroups(
                    chapters = chapters,
                    selectedIndex = selectedIndex,
                    readChapterIndices = readChapterIndices,
                    nextChapterToRead = nextChapterToRead,
                    onChapterClick = { 
                        viewModel.selectChapter(it)
                        isShowingChapterList = false
                    },
                    onContinueReading = {
                        if (selectedExtensionAuthority == null) {
                            Toast.makeText(context, "Select a default extension in Settings first", Toast.LENGTH_SHORT).show()
                        } else {
                            nextChapterToRead?.let { idx ->
                                viewModel.selectChapter(idx)
                                isShowingChapterList = false
                            }
                        }
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun ChapterListWithGroups(
    chapters: List<ChapterInfo>,
    selectedIndex: Int,
    readChapterIndices: Set<Int>,
    nextChapterToRead: Int?,
    onChapterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onContinueReading: (() -> Unit)? = null
) {
    if (chapters.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No chapters found",
                    color = SilverDark,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "They may not be available yet",
                    color = SilverDark.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
        }
    } else {
        var searchQuery by remember { mutableStateOf("") }
        val groupedChapters = remember(chapters) { groupChaptersByMainChapter(chapters) }

        val filteredGroups = remember(groupedChapters, searchQuery) {
            if (searchQuery.isBlank()) groupedChapters
            else groupedChapters.mapNotNull { (key, list) ->
                val filtered = list.filter { (_, chapter) ->
                    chapter.title?.contains(searchQuery, ignoreCase = true) == true
                }
                if (filtered.isNotEmpty()) key to filtered else null
            }
        }

        val listState = rememberLazyListState()
        val chapterToExpand = if (selectedIndex >= 0) selectedIndex else nextChapterToRead
        // Only count integer chapters (not sub-chapters like 346.1, 346.2) toward
        // the total so the progress bar and "X / Y" label are accurate.
        val integerChapterCount = chapters.count { ch ->
            val num = ch.title?.removePrefix("Chapter ")?.trim()?.toFloatOrNull()
            num != null && num == num.toInt().toFloat()
        }
        val readCount = readChapterIndices.size
        val totalCount = integerChapterCount.coerceAtLeast(chapters.size)
        val progress = if (totalCount > 0) readCount.toFloat() / totalCount else 0f

        LaunchedEffect(chapters, chapterToExpand) {
            if (chapterToExpand != null && chapterToExpand >= 0) {
                val targetGroupIndex = filteredGroups.indexOfFirst { (_, groupList) ->
                    groupList.any { it.first == chapterToExpand }
                }
                if (targetGroupIndex >= 0) {
                    delay(100)
                    listState.animateScrollToItem(maxOf(0, targetGroupIndex + 2))
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().background(DarkBackground),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                ChapterListHeader(
                    readCount = readCount,
                    totalCount = totalCount,
                    progress = progress,
                    nextChapterToRead = nextChapterToRead,
                    onContinueReading = onContinueReading ?: {
                        if (nextChapterToRead != null) onChapterClick(nextChapterToRead!!)
                    }
                )
            }

            item(key = "search") {
                ChapterSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            if (filteredGroups.isEmpty() && searchQuery.isNotBlank()) {
                item(key = "no_results") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No chapters match \"$searchQuery\"",
                            color = SilverDark,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                filteredGroups.forEachIndexed { index, (groupKey, groupList) ->
                    val containsTarget = groupList.any { it.first == chapterToExpand }
                    item(key = "chapter_group_$index") {
                        ChapterGroup(
                            groupKey = groupKey,
                            groupChapters = groupList,
                            selectedIndex = selectedIndex,
                            readChapterIndices = readChapterIndices,
                            nextChapterToRead = nextChapterToRead,
                            initiallyExpanded = containsTarget,
                            onChapterClick = onChapterClick
                        )
                    }
                }
            }
        }
    }
}

private fun groupChaptersByMainChapter(chapters: List<ChapterInfo>): List<Pair<String, List<Pair<Int, ChapterInfo>>>> {
    val indexedChapters = chapters.mapIndexed { index, chapter -> index to chapter }

    val byMainChapter = indexedChapters.groupBy { (_, chapter) ->
        val title = chapter.title ?: ""
        extractMainChapterNumber(title)
    }.toSortedMap()

    val result = mutableListOf<Pair<String, List<Pair<Int, ChapterInfo>>>>()
    val hasChapterZero = byMainChapter.containsKey(0)

    byMainChapter.forEach { (mainChapter, items) ->
        val rangeStart = if (hasChapterZero && mainChapter == 0) {
            0
        } else if (hasChapterZero) {
            ((mainChapter - 1) / 20) * 20 + 1
        } else {
            ((mainChapter - 1) / 20) * 20 + 1
        }

        val existingGroup = result.find { (key, _) ->
            val existingRangeStart = key.substringAfter("Ch. ").substringBefore(" - ").toIntOrNull() ?: -1
            existingRangeStart == rangeStart
        }

        if (existingGroup != null) {
            val (existingKey, existingItems) = existingGroup
            val index = result.indexOf(existingGroup)
            val updatedItems = existingItems + items
            val maxChapter = updatedItems.maxOfOrNull { extractMainChapterNumber(it.second.title ?: "") } ?: rangeStart
            val displayStart = existingKey.substringAfter("Ch. ").substringBefore(" - ").toIntOrNull() ?: rangeStart
            val displayEnd = if (displayStart == 0) maxChapter.coerceAtMost(20) else maxChapter
            val displayKey = "Ch. $displayStart - $displayEnd"
            result[index] = displayKey to updatedItems
        } else {
            val displayStart = if (hasChapterZero && mainChapter == 0) 0 else rangeStart
            val displayEnd = if (displayStart == 0) 0 else rangeStart + 19
            val displayKey = "Ch. $displayStart - $displayEnd"
            result.add(displayKey to items)
        }
    }

    return result.sortedBy {
        it.first.substringAfter("Ch. ").substringBefore(" - ").toIntOrNull() ?: 0
    }
}

private fun extractMainChapterNumber(title: String): Int {
    val patterns = listOf(
        Regex("Chapter\\s*(\\d+)"),
        Regex("Ch\\s*\\.\\s*(\\d+)"),
        Regex("^(\\d+)"),
        Regex("(\\d+)(?:\\.\\d+)?")
    )

    for (pattern in patterns) {
        val match = pattern.find(title)
        if (match != null) {
            val numStr = match.groupValues[1]
            return numStr.toIntOrNull() ?: 0
        }
    }
    return 0
}

private fun extractChapterNum(title: String): String {
    val patterns = listOf(
        Regex("Chapter\\s*(\\d+(?:\\.\\d+)?)"),
        Regex("Ch\\s*\\.\\s*(\\d+(?:\\.\\d+)?)"),
        Regex("^(\\d+(?:\\.\\d+)?)"),
        Regex("(\\d+(?:\\.\\d+)?)")
    )
    for (pattern in patterns) {
        val match = pattern.find(title)
        if (match != null) {
            val numStr = match.groupValues[1].trimEnd('.')
            if (numStr.isNotBlank()) return numStr
        }
    }
    return "?"
}

@Composable
private fun ChapterListHeader(
    readCount: Int,
    totalCount: Int,
    progress: Float,
    nextChapterToRead: Int?,
    onContinueReading: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chapters",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$readCount of $totalCount read",
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverDark,
                    letterSpacing = 0.2.sp
                )
            }

            if (progress > 0f) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = BlueLight,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ProgressTrackBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GradientBlue, GradientPurple)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }

        if (nextChapterToRead != null && nextChapterToRead < totalCount) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onContinueReading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue Reading",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "· Ch. ${nextChapterToRead + 1}",
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ChapterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SearchBarBg)
            .then(
                if (query.isNotEmpty()) Modifier.border(1.dp, GlassStrokeFocused, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, GlassStroke, RoundedCornerShape(12.dp))
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = SilverDark,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search chapters...",
                                color = SilverDark,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = SilverDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterGroup(
    groupKey: String,
    groupChapters: List<Pair<Int, ChapterInfo>>,
    selectedIndex: Int,
    readChapterIndices: Set<Int>,
    nextChapterToRead: Int?,
    initiallyExpanded: Boolean = false,
    onChapterClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    LaunchedEffect(initiallyExpanded) {
        if (initiallyExpanded) expanded = true
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "rotation"
    )

    val readInGroup = groupChapters.count { (index, _) -> index in readChapterIndices }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, GlassStroke)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupKey,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${groupChapters.size} chapters",
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverDark
                        )
                        if (readInGroup > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "· $readInGroup read",
                                style = MaterialTheme.typography.bodySmall,
                                color = ReadGreen.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(ChapterCounterBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${groupChapters.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = SilverDark,
                    modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                HorizontalDivider(
                    color = GlassStroke,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    groupChapters.forEach { (absoluteIndex, chapter) ->
                        ChapterRow(
                            chapter = chapter,
                            isSelected = absoluteIndex == selectedIndex,
                            isRead = absoluteIndex in readChapterIndices,
                            isNextToRead = absoluteIndex == nextChapterToRead,
                            onClick = { onChapterClick(absoluteIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterRow(
    chapter: ChapterInfo,
    isSelected: Boolean,
    isRead: Boolean,
    isNextToRead: Boolean,
    onClick: () -> Unit
) {
    val showAsNext = isNextToRead && !isRead
    // Chapters that neither MangaDex nor the extension can provide are rendered
    // greyed out. Clicking still shows the error so the user knows why.
    val isUnavailable = chapter.url.startsWith("mangadex:unavailable:")
    val accentColor = when {
        isUnavailable -> Color(0xFF3A3A3A)
        isSelected && !showAsNext -> BlueAccent
        showAsNext -> BlueLight
        isRead -> ReadGreen
        isSelected -> BlueAccent
        else -> Color.Transparent
    }

    val bgColor = when {
        isSelected && !showAsNext -> CurrentBlueGlow
        else -> Color.Transparent
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .background(bgColor)
                .padding(start = 0.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            val chNum = extractChapterNum(chapter.title ?: "")
            Text(
                text = "Ch. $chNum",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isUnavailable -> SilverDark.copy(alpha = 0.4f)
                    showAsNext -> BlueLight
                    isSelected -> BlueAccent
                    isRead -> ReadGreen.copy(alpha = 0.8f)
                    else -> SilverLight
                },
                fontWeight = if (isSelected || isNextToRead) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.width(64.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = chapter.title ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isUnavailable -> SilverDark.copy(alpha = 0.5f)
                    showAsNext -> Color.White
                    isSelected -> Color.White
                    isNextToRead -> Color.White
                    isRead -> SilverDark
                    else -> SilverLight
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            when {
                isSelected && !showAsNext -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BlueAccent, CircleShape)
                    )
                }
                showAsNext -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BlueLight, CircleShape)
                    )
                }
                isRead -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ReadGreen.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }
        }

        if (isSelected && !showAsNext) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                BlueAccent.copy(alpha = 0.6f),
                                BlueAccent.copy(alpha = 0f)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Segmented 3-mode toggle for the reader layout. Shows all three options
 * simultaneously so the user can see the current mode at a glance (instead
 * of having to tap a single button to cycle through).
 *
 * Layout: a pill-shaped Row with three equal-width segments. The active
 * segment gets the accent color background; inactive segments are dimmed.
 *
 * Icons:
 *  - VERTICAL_SCROLL → ViewAgenda (stacked horizontal bars, like webtoon panels)
 *  - LEFT_TO_RIGHT   → ArrowForward (rightward arrow, "next is to the right")
 *  - RIGHT_TO_LEFT   → ArrowBack (leftward arrow, "next is to the left")
 *
 * The whole control is intentionally compact (~110dp wide) so it fits in the
 * TopAppBar actions without pushing the prev/next chapter buttons off-screen.
 */
@Composable
private fun ReaderModeSegmentedToggle(
    currentMode: ReaderMode,
    onSelect: (ReaderMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(2.dp)
    ) {
        SegmentedToggleItem(
            icon = Icons.Default.ViewAgenda,
            contentDescription = "Vertical scroll mode",
            selected = currentMode == ReaderMode.VERTICAL_SCROLL,
            onClick = { onSelect(ReaderMode.VERTICAL_SCROLL) }
        )
        SegmentedToggleItem(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Left to right paged mode",
            selected = currentMode == ReaderMode.LEFT_TO_RIGHT,
            onClick = { onSelect(ReaderMode.LEFT_TO_RIGHT) }
        )
        SegmentedToggleItem(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Right to left paged mode",
            selected = currentMode == ReaderMode.RIGHT_TO_LEFT,
            onClick = { onSelect(ReaderMode.RIGHT_TO_LEFT) }
        )
    }
}

@Composable
private fun SegmentedToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) BlueAccent else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
