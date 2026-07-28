package com.blissless.oni.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.oni.data.MangaTrack
import com.blissless.oni.ui.components.rememberCinematicAnimation
import com.blissless.oni.ui.theme.StatusColors
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

enum class MangaSortOption(val label: String, val icon: ImageVector) {
    ALPHABETICAL_A_Z("A-Z", Icons.AutoMirrored.Filled.Sort),
    ALPHABETICAL_Z_A("Z-A", Icons.AutoMirrored.Filled.Sort),
    CHAPTERS_MOST("Chapters \u2193", Icons.Default.PlayArrow),
    CHAPTERS_LEAST("Chapters \u2191", Icons.Default.PlayArrow),
    RECENTLY_UPDATED("Recently Updated", Icons.Default.DateRange),
    OLDEST_UPDATED("Oldest Updated", Icons.Default.DateRange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusListScreen(
    title: String,
    icon: ImageVector,
    mangaList: List<MangaTrack>,
    listType: String,
    onMangaClick: (MangaTrack) -> Unit = {},
    onBackClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val iconTint = StatusColors[listType] ?: MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onBackClick)

    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "offsetY"
    )

    var selectedSort by remember { mutableStateOf(MangaSortOption.ALPHABETICAL_A_Z) }
    var showSortSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val displayList = remember(mangaList, searchQuery, selectedSort) {
        val filtered = if (searchQuery.isBlank()) mangaList
        else mangaList.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
        when (selectedSort) {
            MangaSortOption.ALPHABETICAL_A_Z -> filtered.sortedBy { it.title.lowercase() }
            MangaSortOption.ALPHABETICAL_Z_A -> filtered.sortedByDescending { it.title.lowercase() }
            MangaSortOption.CHAPTERS_MOST -> filtered.sortedByDescending {
                if (it.totalChapters > 0) it.totalChapters else it.currentChapterNumber.toInt()
            }
            MangaSortOption.CHAPTERS_LEAST -> filtered.sortedBy {
                if (it.totalChapters > 0) it.totalChapters else it.currentChapterNumber.toInt()
            }
            MangaSortOption.RECENTLY_UPDATED -> filtered.sortedByDescending { it.lastReadTimestamp }
            MangaSortOption.OLDEST_UPDATED -> filtered.sortedBy { it.lastReadTimestamp }
        }
    }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val scrollToTop = {
        scope.launch { gridState.animateScrollToItem(0) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .graphicsLayer { translationY = animatedOffset }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) onDismiss()
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { scrollToTop() }
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${mangaList.size} manga",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { focusManager.clearFocus(); showSortSheet = true }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search in this list...") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (mangaList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = icon, contentDescription = null, tint = iconTint.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "No manga in this list", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                val density = LocalDensity.current
                val translationYOffset = with(density) { (-30).dp.toPx() }
                val isScrolling by remember { derivedStateOf { gridState.isScrollInProgress } }
                val cinematicProgress = rememberCinematicAnimation("statusList_$listType", isVisible = true, playOncePerSession = true)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(items = displayList, key = { _, track -> "${listType}_${track.mangaId}" }) { index, track ->
                        val staggerDelay = minOf(index, 20) * 30f
                        val staggerMs = staggerDelay / 1000f
                        val rawProgress = ((cinematicProgress - staggerMs) / (1f - staggerMs))
                        val easedProgress = easeOutCubic(rawProgress.coerceAtLeast(0f).coerceAtMost(1f))

                        val introScale = 0.3f + easedProgress * 0.7f
                        val introAlpha = easedProgress.coerceAtLeast(0f)
                        val introTranslationY = translationYOffset * (1f - easedProgress)

                        val centerOffset by remember {
                            derivedStateOf {
                                val layoutInfo = gridState.layoutInfo
                                val visibleItems = layoutInfo.visibleItemsInfo
                                val itemInfo = visibleItems.find { it.index == index }
                                if (itemInfo != null) {
                                    val itemCenter = itemInfo.offset.y + itemInfo.size.height / 2
                                    val screenCenter = (layoutInfo.viewportSize.height / 2).toFloat()
                                    (itemCenter - screenCenter) / screenCenter
                                } else 0f
                            }
                        }

                        val animatedCenterOffset by animateFloatAsState(
                            targetValue = if (isScrolling) centerOffset.coerceIn(-2f, 2f) else 0f,
                            animationSpec = if (isScrolling) {
                                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                            } else {
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                            },
                            label = "centerOffset"
                        )

                        val scrollScale = 1f - (animatedCenterOffset.absoluteValue * 0.15f).coerceAtMost(0.15f)
                        val scrollAlpha = 1f - (animatedCenterOffset.absoluteValue * 0.3f).coerceAtMost(0.4f)
                        val scrollParallax = animatedCenterOffset * 20f

                        val finalScale = scrollScale * introScale
                        val finalAlpha = (scrollAlpha * introAlpha).coerceIn(0f, 1f)
                        val finalTranslationY = scrollParallax + introTranslationY

                        Box(
                            modifier = Modifier
                                .animateItem()
                                .graphicsLayer {
                                    scaleX = finalScale
                                    scaleY = finalScale
                                    alpha = finalAlpha
                                    translationY = finalTranslationY
                                }
                        ) {
                            StatusListMangaCard(
                                track = track,
                                listType = listType,
                                onClick = { onMangaClick(track) }
                            )
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        "Sort by",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    HorizontalDivider()
                    MangaSortOption.entries.forEach { option ->
                        val isSelected = option == selectedSort
                        Surface(
                            onClick = { focusManager.clearFocus(); selectedSort = option; showSortSheet = false; scrollToTop() },
                            color = if (isSelected) iconTint.copy(alpha = 0.12f) else Color.Transparent,
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = iconTint))
                                Icon(imageVector = option.icon, contentDescription = null, tint = if (isSelected) iconTint else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun easeOutCubic(t: Float): Float {
    val t1 = t - 1
    return t1 * t1 * t1 + 1
}

@Composable
private fun StatusListMangaCard(
    track: MangaTrack,
    listType: String,
    onClick: () -> Unit
) {
    val statusColor = StatusColors[listType] ?: MaterialTheme.colorScheme.primary
    val total = track.totalChapters
    val progressText = when (listType) {
        "CURRENT" -> {
            when {
                total > 0 -> "Ch. ${track.currentChapterNumber} / $total"
                else -> "Ch. ${track.currentChapterNumber}"
            }
        }
        "COMPLETED" -> if (total > 0) "$total ch" else "${track.currentChapterNumber.toInt()} ch"
        else -> {
            when {
                total > 0 -> "Ch. ${track.currentChapterNumber} / $total"
                else -> "??"
            }
        }
    }

    Column(modifier = Modifier.width(160.dp)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (track.coverUrl != null) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            track.title.take(2).uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .padding(top = 52.dp)
                        .background(statusColor)
                )
            }
        }

        Box(modifier = Modifier.width(160.dp).height(40.dp).padding(top = 6.dp)) {
            Text(
                text = track.title,
                maxLines = 2,
                style = MaterialTheme.typography.labelMedium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
