package com.blissless.oni.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.oni.data.ChapterInfo
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

    val listState = rememberLazyListState()
    var isShowingChapterList by remember { mutableStateOf(selectedIndex < 0) }
    var scrollProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(selectedIndex) {
        isShowingChapterList = selectedIndex < 0
        if (selectedIndex < 0) {
            viewModel.refreshTrackingLists()
        }
        if (selectedIndex >= 0 && resumeScrollProgress < 0f) {
            listState.scrollToItem(0)
        }
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
            (currentScroll / maxScroll).coerceIn(0f, 1f)
        }.collect { progress: Float ->
            scrollProgress = progress
            viewModel.onChapterScrollProgress(progress)
        }
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
                                Text(
                                    text = chapters[selectedIndex].title ?: "Chapter ${selectedIndex + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SilverDark
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
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(images, key = { index, _ -> "page_$index" }) { index, imageUrl ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
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
    modifier: Modifier = Modifier
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
        val readCount = readChapterIndices.size
        val totalCount = chapters.size
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
                    onContinueReading = {
                        nextChapterToRead?.let { onChapterClick(it) }
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
    val accentColor = when {
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
