package com.blissless.oni.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.oni.data.MangaSearchResult
import com.blissless.oni.data.MangaTrack
import com.blissless.oni.viewmodel.MainViewModel
import com.blissless.oni.ui.theme.GradientBlue
import com.blissless.oni.ui.theme.GradientPurple

private object HomeStatusColors {
    fun getColor(status: String): Color = when (status) {
        "CURRENT" -> Color(0xFF4CAF50)
        "PLANNING" -> Color(0xFF5C6BC0)
        "COMPLETED" -> Color(0xFF66BB6A)
        "PAUSED" -> Color(0xFFFFB74D)
        "DROPPED" -> Color(0xFFEF5350)
        else -> Color(0xFF6750A4)
    }
}

@Composable
fun HomeSectionHeader(
    title: String,
    icon: ImageVector,
    count: Int,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(iconTint, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.12f)
            ) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTint,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun MangaHorizontalList(
    tracks: List<MangaTrack>,
    listType: String,
    onMangaClick: (MangaTrack) -> Unit,
    onRemoveClick: ((MangaTrack) -> Unit)? = null
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tracks, key = { "${listType}_${it.mangaId}" }) { track ->
            MangaHomeCard(
                track = track,
                listType = listType,
                onClick = { onMangaClick(track) },
                onRemoveClick = onRemoveClick?.let { { it(track) } }
            )
        }
    }
}

@Composable
fun MangaHomeCard(
    track: MangaTrack,
    listType: String,
    onClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null
) {
    val total = track.totalChapters
    val progressText = when (listType) {
        "CURRENT" -> {
            when {
                total > 0 -> "Ch. ${track.currentChapterNumber} / $total"
                else -> "Ch. ${track.currentChapterNumber}"
            }
        }
        "COMPLETED" -> if (total > 0) "$total ch" else "${track.currentChapterNumber} ch"
        else -> {
            when {
                total > 0 -> "Ch. ${track.currentChapterNumber} / $total"
                else -> "??"
            }
        }
    }

    Column(modifier = Modifier.width(140.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .height(195.dp)
                .clip(RoundedCornerShape(14.dp))
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
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(50.dp)
                        .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))))
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(80.dp)
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))

                Row(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (track.scrollProgress > 0f) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "${(track.scrollProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (onRemoveClick != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .clickable(onClick = onRemoveClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.width(140.dp).height(40.dp)) {
            Text(
                text = track.title,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 2,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ResumeMangaCard(
    track: MangaTrack,
    onClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(240.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
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
                    Text(track.title.take(2).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, bottom = 14.dp, top = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Ch. ${maxOf(track.currentChapterNumber, 0) + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (onRemoveClick != null) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .clickable(onClick = onRemoveClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Column {
                    val progress = track.scrollProgress.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onMangaSelected: (MangaSearchResult) -> Unit,
    onContinueReading: (MangaTrack) -> Unit,
    onResumeReading: (MangaTrack) -> Unit = onContinueReading,
    onRemoveResumeTracking: (MangaTrack) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val continueReading by viewModel.continueReading.collectAsState()
    val resumeReading by viewModel.resumeReading.collectAsState()
    val planningToRead by viewModel.planningToRead.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshTrackingLists()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Oni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            val totalTracked = continueReading.size + planningToRead.size
                            Text("$totalTracked manga tracked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp,
                    onClick = onSearchClick
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        if (resumeReading.isNotEmpty()) {
            item {
                Column {
                    HomeSectionHeader(
                        title = "Resume Reading",
                        icon = Icons.Default.PlayArrow,
                        count = resumeReading.size,
                        iconTint = HomeStatusColors.getColor("CURRENT")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(resumeReading) { track ->
                            ResumeMangaCard(
                                track = track,
                                onClick = { onResumeReading(track) },
                                onRemoveClick = { onRemoveResumeTracking(track) }
                            )
                        }
                    }
                }
            }
        }

        if (continueReading.isNotEmpty()) {
            item {
                Column {
                    HomeSectionHeader(
                        title = "Continue Reading",
                        icon = Icons.Default.PlayArrow,
                        count = continueReading.size,
                        iconTint = HomeStatusColors.getColor("CURRENT")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MangaHorizontalList(
                        tracks = continueReading,
                        listType = "CURRENT",
                        onMangaClick = { track ->
                            val manga = MangaSearchResult(
                                title = track.title,
                                url = track.mangaUrl,
                                coverUrl = track.coverUrl,
                                mangaId = track.mangaId
                            )
                            onMangaSelected(manga)
                        }
                    )
                }
            }
        }

        if (planningToRead.isNotEmpty()) {
            item {
                Column {
                    HomeSectionHeader(
                        title = "Planning to Read",
                        icon = Icons.Default.Bookmark,
                        count = planningToRead.size,
                        iconTint = HomeStatusColors.getColor("PLANNING")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MangaHorizontalList(
                        tracks = planningToRead,
                        listType = "PLANNING",
                        onMangaClick = { track ->
                            val manga = MangaSearchResult(
                                title = track.title,
                                url = track.mangaUrl,
                                coverUrl = track.coverUrl,
                                mangaId = track.mangaId
                            )
                            onMangaSelected(manga)
                        }
                    )
                }
            }
        }

        if (continueReading.isEmpty() && planningToRead.isEmpty() && resumeReading.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Your lists are empty", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text("Check out the Explore tab to discover manga!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
