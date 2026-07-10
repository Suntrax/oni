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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.blissless.oni.ui.theme.BlueAccent
import com.blissless.oni.ui.theme.BlueLight
import com.blissless.oni.ui.theme.DarkBackground
import com.blissless.oni.ui.theme.DarkCard
import com.blissless.oni.ui.theme.DarkSurface
import com.blissless.oni.ui.theme.DarkSurfaceVariant
import com.blissless.oni.ui.theme.GlassStroke
import com.blissless.oni.ui.theme.SilverDark
import com.blissless.oni.ui.theme.SilverLight
import com.blissless.oni.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun ExploreScreen(
    viewModel: MainViewModel,
    onMangaSelected: (AniListSearchResult) -> Unit
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
            CircularProgressIndicator(color = BlueAccent)
        }
    } else if (sections.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No manga found", color = SilverDark, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Check your internet connection", color = SilverDark.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        }
    } else {
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
                            if (pagerState.currentPage % actualCount == index) BlueAccent
                            else SilverDark.copy(alpha = 0.4f)
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
                .background(DarkSurfaceVariant)
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
                                0f to DarkSurface,
                                1f to BlueAccent.copy(alpha = 0.3f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = manga.title.take(2).uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = BlueAccent.copy(alpha = 0.5f),
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
                        color = BlueAccent,
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
                        color = SilverLight.copy(alpha = 0.8f)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            Text(
                text = "${section.items.size} titles",
                fontSize = 12.sp,
                color = SilverDark,
                letterSpacing = 0.2.sp
            )
        }

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
fun MangaSmallCard(
    manga: AniListSearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, GlassStroke)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(DarkSurfaceVariant),
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
                        color = BlueAccent.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, DarkCard.copy(alpha = 0.85f))
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
