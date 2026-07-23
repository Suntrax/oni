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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.oni.data.AniListSearchResult
import com.blissless.oni.data.ExploreSection
import com.blissless.oni.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun ExploreScreen(
    viewModel: MainViewModel,
    onMangaSelected: (AniListSearchResult) -> Unit,
    onSearchClick: () -> Unit = {}
) {
    val sections by viewModel.exploreSections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        if (sections.isEmpty()) {
            viewModel.loadExplorePage()
        }
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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                val trending = sections.firstOrNull { it.key == "trending" }

                if (trending != null) {
                    item {
                        FeaturedCarousel(
                            section = trending,
                            onMangaClick = onMangaSelected
                        )
                    }
                }

                sections.filter { it.key != "trending" }.forEach { section ->
                    item {
                        SectionRow(
                            section = section,
                            onMangaClick = onMangaSelected
                        )
                    }
                }
            }

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(44.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun FeaturedCarousel(
    section: ExploreSection,
    onMangaClick: (AniListSearchResult) -> Unit
) {
    val actualCount = section.items.size
    val pagerState = rememberPagerState(
        initialPage = actualCount * 100,
        pageCount = { actualCount * 200 }
    )

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }
            .collect {
                delay(4500)
                try {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } catch (_: Exception) {}
            }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 0
            ) { page ->
                val manga = section.items[page % actualCount]
                FeaturedCard(
                    manga = manga,
                    onClick = { onMangaClick(manga) },
                    isActive = page % actualCount == pagerState.currentPage % actualCount
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(section.items.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage % actualCount == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
fun FeaturedCard(
    manga: AniListSearchResult,
    onClick: () -> Unit,
    isActive: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (manga.coverUrl != null) {
                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = manga.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                0f to MaterialTheme.colorScheme.surface,
                                1f to MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = manga.title.take(2).uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 120f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Trending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    if (manga.meanScore != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFfbbf24),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${(manga.meanScore!! / 10f)}",
                                color = Color(0xFFfbbf24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val subtitle = buildString {
                    manga.format?.let { append(formatLabel(it)) }
                    if (manga.genres != null && manga.genres!!.isNotEmpty()) {
                        if (isNotEmpty()) append("  ·  ")
                        append(manga.genres!!.take(3).joinToString(", "))
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionRow(
    section: ExploreSection,
    onMangaClick: (AniListSearchResult) -> Unit
) {
    Column {
        ExploreSectionTitle(title = section.title, count = section.items.size)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.items) { manga ->
                MangaSmallCard(
                    manga = manga,
                    onClick = { onMangaClick(manga) }
                )
            }
        }
    }
}

@Composable
fun ExploreSectionTitle(
    title: String,
    count: Int,
    iconTint: Color = MaterialTheme.colorScheme.primary
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
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
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
fun MangaSmallCard(
    manga: AniListSearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (manga.coverUrl != null) {
                    AsyncImage(
                        model = manga.coverUrl,
                        contentDescription = manga.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = manga.title.take(2).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )

                if (manga.meanScore != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFfbbf24), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${(manga.meanScore!! / 10f)}",
                            color = Color(0xFFfbbf24),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = manga.title,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

private fun formatLabel(format: String?): String = when (format?.uppercase()) {
    "MANGA" -> "Manga"
    "NOVEL" -> "Light Novel"
    "ONE_SHOT" -> "One-Shot"
    "DOUJIN" -> "Doujin"
    else -> format ?: ""
}
