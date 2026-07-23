package com.blissless.oni.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.oni.data.AniListSearchResult
import com.blissless.oni.data.ExploreSection
import com.blissless.oni.ui.components.easeOutCubic
import com.blissless.oni.ui.components.rememberCinematicAnimation
import com.blissless.oni.ui.theme.StatusColors
import com.blissless.oni.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun ExploreScreen(
    viewModel: MainViewModel,
    onMangaSelected: (AniListSearchResult) -> Unit,
    onSearchClick: () -> Unit = {},
    onReadNow: (AniListSearchResult) -> Unit = {}
) {
    val sections by viewModel.exploreSections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedMangaForStatus by remember { mutableStateOf<AniListSearchResult?>(null) }

    LaunchedEffect(Unit) {
        if (sections.isEmpty()) {
            viewModel.loadExplorePage()
        }
    }

    if (showStatusDialog && selectedMangaForStatus != null) {
        val manga = selectedMangaForStatus!!
        val mangaId = "anilist_${manga.id}"
        val tracking = viewModel.resolveMangaTracking(mangaId)
        MangaStatusDialog(
            title = manga.title,
            coverUrl = manga.coverUrl,
            currentStatus = when (tracking?.status) {
                com.blissless.oni.data.ReadingStatus.READING -> "CURRENT"
                com.blissless.oni.data.ReadingStatus.PLANNING -> "PLANNING"
                com.blissless.oni.data.ReadingStatus.COMPLETED -> "COMPLETED"
                com.blissless.oni.data.ReadingStatus.ON_HOLD -> "PAUSED"
                com.blissless.oni.data.ReadingStatus.DROPPED -> "DROPPED"
                else -> ""
            },
            currentChapterNumber = tracking?.currentChapterNumber ?: 0,
            totalChapters = manga.chapters ?: 0,
            onDismiss = { showStatusDialog = false; selectedMangaForStatus = null },
            onRemove = {
                viewModel.removeFromAnilist(mangaId)
                showStatusDialog = false
                selectedMangaForStatus = null
            },
            onUpdate = { status, progress ->
                viewModel.updateTrackingStatus(mangaId, status)
                showStatusDialog = false
                selectedMangaForStatus = null
            }
        )
    }

    if (isLoading && sections.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (sections.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No manga found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Check your internet connection", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        }
    } else {
        val scrollState = rememberScrollState()
        val trending = sections.firstOrNull { it.key == "trending" }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp)
        ) {
            if (trending != null) {
                FeaturedCarousel(
                    section = trending,
                    onMangaClick = onMangaSelected,
                    onSearchClick = onSearchClick,
                    onReadNow = onReadNow,
                    onBookmarkClick = { manga ->
                        selectedMangaForStatus = manga
                        showStatusDialog = true
                    }
                )
            }

            sections.filter { it.key != "trending" }.forEachIndexed { sectionIndex, section ->
                SectionRow(
                    section = section,
                    onMangaClick = onMangaSelected,
                    sectionIndex = sectionIndex
                )
            }
        }
    }
}

@Composable
fun FeaturedCarousel(
    section: ExploreSection,
    onMangaClick: (AniListSearchResult) -> Unit,
    onSearchClick: () -> Unit = {},
    onReadNow: (AniListSearchResult) -> Unit = {},
    onBookmarkClick: (AniListSearchResult) -> Unit = {}
) {
    val actualCount = section.items.size
    val pagerState = rememberPagerState(
        initialPage = actualCount * 100,
        pageCount = { actualCount * 200 }
    )

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val scope = rememberCoroutineScope()
    var headerVisible by remember { mutableStateOf(true) }
    var pageWhenScrollStarted by remember { mutableIntStateOf(pagerState.currentPage) }
    var timerResetSignal by remember { mutableIntStateOf(0) }
    val currentPageOffsetFraction by remember { derivedStateOf { pagerState.currentPageOffsetFraction } }

    LaunchedEffect(pagerState.isScrollInProgress, currentPageOffsetFraction) {
        if (pagerState.isScrollInProgress) {
            pageWhenScrollStarted = pagerState.currentPage
        } else if (pagerState.currentPage != pageWhenScrollStarted) {
            headerVisible = false
            delay(80)
            headerVisible = true
            pageWhenScrollStarted = pagerState.currentPage
        }
    }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            timerResetSignal++
        }
    }

    LaunchedEffect(timerResetSignal) {
        while (true) {
            delay(4500)
            headerVisible = false
            delay(80)
            headerVisible = true
            scope.launch {
                try {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } catch (_: Exception) {}
            }
            delay(300)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(560.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp,
            userScrollEnabled = true,
            beyondViewportPageCount = 0
        ) { page ->
            val manga = section.items[page % actualCount]
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                if (manga.coverUrl != null) {
                    AsyncImage(
                        model = manga.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    0f to MaterialTheme.colorScheme.surface,
                                    1f to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            )
                    ) {
                        Text(
                            text = manga.title.take(2).uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 32.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = com.blissless.oni.R.mipmap.ic_launcher_round,
                                contentDescription = "App",
                                modifier = Modifier.size(28.dp).clip(CircleShape)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentPage = pagerState.currentPage % actualCount
                        repeat(actualCount) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == currentPage) 16.dp else 5.dp, 5.dp)
                                    .background(
                                        if (index == currentPage) Color.White
                                        else Color.White.copy(alpha = 0.4f),
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp),
                        onClick = onSearchClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).align(Alignment.BottomCenter),
                contentAlignment = Alignment.BottomCenter
            ) {
                val currentManga by remember {
                    derivedStateOf { section.items[pagerState.currentPage % actualCount] }
                }

                AnimatedVisibility(
                    visible = headerVisible,
                    enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                animationSpec = tween(400, easing = FastOutSlowInEasing),
                                initialOffsetY = { it / 2 }
                            ),
                    exit = fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing)) +
                            slideOutVertically(
                                animationSpec = tween(150, easing = FastOutSlowInEasing),
                                targetOffsetY = { it / 2 }
                            )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentManga.title,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val formatText = formatLabel(currentManga.format)
                            if (formatText.isNotBlank()) {
                                Text(text = formatText, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                            }
                            if (currentManga.meanScore != null) {
                                if (formatText.isNotBlank()) {
                                    Text(text = " \u00B7 ", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium)
                                }
                                val scoreValue = currentManga.meanScore!! / 10.0
                                Text(
                                    text = "\u2605 ${String.format("%.1f", scoreValue)}",
                                    color = Color(0xFFFFD700),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onBookmarkClick(currentManga) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.BookmarkBorder,
                                            contentDescription = "Track",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            FilledTonalIconButton(
                                onClick = { onReadNow(currentManga) },
                                modifier = Modifier.height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Read Now",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Read Now", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }

                            IconButton(
                                onClick = { onMangaClick(currentManga) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = "Info",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
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

@Composable
private fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        )
    ) {
        content()
    }
}

@Composable
fun SectionRow(
    section: ExploreSection,
    onMangaClick: (AniListSearchResult) -> Unit,
    sectionIndex: Int = 0
) {
    val cinematicProgress = rememberCinematicAnimation("explore", true, true)
    val staggerDelay = sectionIndex * 50f
    val effectiveProgress = ((cinematicProgress * 1000f - staggerDelay) / 1000f).coerceIn(0f, 1f)
    val easedProgress = easeOutCubic(effectiveProgress)

    Column(
        modifier = Modifier.graphicsLayer {
            alpha = easedProgress
            translationY = (1f - easedProgress) * 40f
        }
    ) {
        SectionTitle(title = section.title, count = section.items.size)

        AnimatedHorizontalList(
            items = section.items,
            onMangaClick = onMangaClick
        )
    }
}

@Composable
fun AnimatedHorizontalList(
    items: List<AniListSearchResult>,
    onMangaClick: (AniListSearchResult) -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val density = LocalDensity.current
    val cameraDistancePx = with(density) { 12.dp.toPx() }
    val translationYOffset = with(density) { (-40).dp.toPx() }

    val isScrolling by remember {
        derivedStateOf { listState.isScrollInProgress }
    }

    val cinematicProgress = rememberCinematicAnimation("explore", true, true)
    val easedProgress = easeOutCubic(cinematicProgress.coerceIn(0f, 1f))

    androidx.compose.foundation.lazy.LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items.size, key = { items[it].id }) { index ->
            val manga = items[index]
            val layoutInfo by remember { derivedStateOf { listState.layoutInfo } }
            val visibleItems = layoutInfo.visibleItemsInfo
            val itemInfo = visibleItems.find { it.index == index }

            val centerOffset = if (itemInfo != null) {
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val screenCenter = (layoutInfo.viewportSize.width / 2).toFloat()
                (itemCenter - screenCenter) / screenCenter
            } else {
                0f
            }

            val animatedOffset by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isScrolling) centerOffset.coerceIn(-1.5f, 1.5f) else 0f,
                animationSpec = if (isScrolling) {
                    androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    )
                } else {
                    androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                },
                label = "centerOffset"
            )

            val baseScale = 1f - (animatedOffset.absoluteValue * 0.25f).coerceAtMost(0.25f)
            val baseAlpha = 1f - (animatedOffset.absoluteValue * 0.4f).coerceAtMost(0.6f)
            val translationXVal = animatedOffset * -20f
            val rotationYVal = (animatedOffset * 15f).coerceIn(-15f, 15f)

            val introScale = 0.3f + easedProgress * 0.7f
            val introTranslationY = translationYOffset * (1f - easedProgress)

            val finalScale = baseScale * introScale
            val finalAlpha = baseAlpha * easedProgress

            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = finalScale
                    scaleY = finalScale
                    alpha = finalAlpha
                    translationX = translationXVal
                    translationY = introTranslationY
                    rotationY = rotationYVal
                    cameraDistance = cameraDistancePx
                }
            ) {
                ExploreMangaCard(
                    manga = manga,
                    onClick = { onMangaClick(manga) }
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, count: Int? = null) {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        count?.let {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    "$it",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun ExploreMangaCard(
    manga: AniListSearchResult,
    onClick: () -> Unit
) {
    val displayTitle = when {
        !manga.englishTitle.isNullOrEmpty() -> manga.englishTitle
        manga.title.isNotEmpty() -> manga.title
        else -> "Unknown"
    }

    val chapterText = remember(manga.chapters) {
        when {
            manga.chapters != null && manga.chapters > 0 -> "${manga.chapters} ch"
            else -> ""
        }
    }

    val displayScore = remember(manga.meanScore) {
        manga.meanScore?.let { it / 10.0 }
    }

    Column(modifier = Modifier.width(120.dp)) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .height(170.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (manga.coverUrl != null) {
                    AsyncImage(
                        model = manga.coverUrl,
                        contentDescription = manga.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = manga.title.take(2).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                displayScore?.let { score ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            "\u2605 ${String.format("%.1f", score)}",
                            color = Color(0xFFFFD700),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (chapterText.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            chapterText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = displayTitle,
            modifier = Modifier.padding(top = 8.dp).height(36.dp),
            maxLines = 2,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatLabel(format: String?): String = when (format?.uppercase()) {
    "MANGA" -> "Manga"
    "NOVEL" -> "Light Novel"
    "ONE_SHOT" -> "One-Shot"
    "DOUJIN" -> "Doujin"
    else -> format ?: ""
}
