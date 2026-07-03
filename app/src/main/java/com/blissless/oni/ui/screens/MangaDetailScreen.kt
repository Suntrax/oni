package com.blissless.oni.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.oni.data.ReadingStatus
import com.blissless.oni.ui.theme.BlueAccent
import com.blissless.oni.ui.theme.BlueLight
import com.blissless.oni.ui.theme.DarkBackground
import com.blissless.oni.ui.theme.DarkCard
import com.blissless.oni.ui.theme.DarkSurface
import com.blissless.oni.ui.theme.DarkSurfaceVariant
import com.blissless.oni.ui.theme.GlassStroke
import com.blissless.oni.ui.theme.GradientBlue
import com.blissless.oni.ui.theme.GradientPurple
import com.blissless.oni.ui.theme.ReadGreen
import com.blissless.oni.ui.theme.SilverDark
import com.blissless.oni.ui.theme.SilverLight
import com.blissless.oni.ui.theme.StatusCompleted
import com.blissless.oni.ui.theme.StatusDropped
import com.blissless.oni.ui.theme.StatusPaused
import com.blissless.oni.ui.theme.StatusPlanning
import com.blissless.oni.viewmodel.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MangaDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStartReading: () -> Unit,
    onOpenReader: () -> Unit,
    onOpenReaderDirect: () -> Unit,
    onOpenChapterSelect: () -> Unit
) {
    val mangaDetail by viewModel.mangaDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val readChapterIndices by viewModel.readChapterIndices.collectAsState()
    val detail = mangaDetail

    var currentStatus by remember(detail?.id) { mutableStateOf<ReadingStatus?>(null) }
    var currentChapter by remember(detail?.id) { mutableIntStateOf(1) }
    var showChapterSelect by remember(detail?.id) { mutableStateOf(false) }
    var fallbackCoverUrl by remember(detail?.id) {
        mutableStateOf(detail?.coverUrl?.takeIf { it.isNotBlank() } ?: viewModel.getCurrentMangaCoverUrl())
    }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showChapterDialog by remember { mutableStateOf(false) }

    fun refreshTracking() {
        detail?.id?.let { mangaId ->
            viewModel.refreshTrackingLists()
            val tracking = viewModel.getMangaTracking(mangaId)
            currentStatus = tracking?.status
            currentChapter = if (tracking != null && tracking.currentChapterNumber > 0) {
                tracking.currentChapterNumber
            } else {
                1
            }
            val trackingCover = tracking?.coverUrl ?: viewModel.getCurrentMangaCoverUrl()
            if (trackingCover != null) {
                fallbackCoverUrl = trackingCover
            }
        }
    }

    LaunchedEffect(detail?.id) { refreshTracking() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        refreshTracking()
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when {
            isLoading && detail == null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BlueAccent)
                }
            }

            detail != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val coverModel = fallbackCoverUrl ?: detail.coverUrl?.takeIf { it.isNotBlank() } ?: viewModel.getCurrentMangaCoverUrl()

                        if (coverModel != null) {
                            AsyncImage(
                                model = coverModel,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                                contentScale = ContentScale.FillWidth
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .background(
                                        Brush.linearGradient(
                                            0f to DarkSurface,
                                            1f to BlueAccent.copy(alpha = 0.15f)
                                        )
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            DarkBackground.copy(alpha = 0.4f),
                                            DarkBackground.copy(alpha = 0.7f),
                                            DarkBackground
                                        )
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            AsyncImage(
                                model = coverModel,
                                contentDescription = detail.title,
                                modifier = Modifier
                                    .width(120.dp)
                                    .aspectRatio(0.7f)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (detail.englishTitle != null && detail.englishTitle.isNotBlank() && detail.englishTitle != detail.title) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = detail.englishTitle,
                                        color = SilverDark,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = detail.type,
                                        fontSize = 11.sp,
                                        color = BlueAccent,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BlueAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = detail.status,
                                        fontSize = 11.sp,
                                        color = if (detail.status == "Ongoing") Color(0xFF10B981) else SilverDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(0.5.dp, GlassStroke)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (detail.avgRating > 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFfbbf24), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = String.format("%.1f", detail.avgRating),
                                            color = Color(0xFFfbbf24),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text("Rating", color = SilverDark, fontSize = 10.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium)
                                }
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(GlassStroke))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${detail.totalChapterCount}",
                                    color = SilverLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text("Chapters", color = SilverDark, fontSize = 10.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium)
                            }

                            if (detail.authors.isNotEmpty()) {
                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(GlassStroke))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = detail.authors.first(),
                                        color = SilverLight,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 100.dp)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text("Author", color = SilverDark, fontSize = 10.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(0.5.dp, GlassStroke)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = {
                                    viewModel.continueFromCurrentManga { onOpenReaderDirect() }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (currentStatus == ReadingStatus.READING) "Ch. $currentChapter" else "Start Reading",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.3.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                        .clickable { showStatusMenu = true },
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        when (currentStatus) {
                                            ReadingStatus.READING -> Icons.Default.Bookmark
                                            ReadingStatus.PLANNING -> Icons.Default.CalendarMonth
                                            else -> Icons.Default.BookmarkBorder
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = when (currentStatus) {
                                            ReadingStatus.READING -> BlueAccent
                                            ReadingStatus.PLANNING -> StatusPlanning
                                            ReadingStatus.COMPLETED -> StatusCompleted
                                            else -> SilverDark
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = when (currentStatus) {
                                            ReadingStatus.READING -> "Reading"
                                            ReadingStatus.PLANNING -> "Planned"
                                            ReadingStatus.COMPLETED -> "Completed"
                                            ReadingStatus.ON_HOLD -> "Paused"
                                            ReadingStatus.DROPPED -> "Dropped"
                                            null -> "Track"
                                        },
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = when (currentStatus) {
                                            ReadingStatus.READING -> BlueAccent
                                            ReadingStatus.PLANNING -> StatusPlanning
                                            ReadingStatus.COMPLETED -> StatusCompleted
                                            else -> SilverLight
                                        }
                                    )
                                }
                                DropdownMenu(
                                    expanded = showStatusMenu,
                                    onDismissRequest = { showStatusMenu = false }
                                ) {
                                    val statuses = listOf(
                                        ReadingStatus.READING,
                                        ReadingStatus.PLANNING,
                                        ReadingStatus.COMPLETED,
                                        ReadingStatus.ON_HOLD,
                                        ReadingStatus.DROPPED
                                    )
                                    statuses.forEach { status ->
                                        val label = when (status) {
                                            ReadingStatus.READING -> "Reading"
                                            ReadingStatus.PLANNING -> "Plan to Read"
                                            ReadingStatus.COMPLETED -> "Completed"
                                            ReadingStatus.ON_HOLD -> "On Hold"
                                            ReadingStatus.DROPPED -> "Dropped"
                                        }
                                        val statusColor = when (status) {
                                            ReadingStatus.READING -> BlueAccent
                                            ReadingStatus.PLANNING -> StatusPlanning
                                            ReadingStatus.COMPLETED -> StatusCompleted
                                            ReadingStatus.ON_HOLD -> StatusPaused
                                            ReadingStatus.DROPPED -> StatusDropped
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(statusColor)
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(label, fontSize = 14.sp, color = SilverLight)
                                                }
                                            },
                                            onClick = {
                                                showStatusMenu = false
                                                if (currentStatus != status) {
                                                    viewModel.updateTrackingStatus(detail.id, status)
                                                    currentStatus = status
                                                }
                                            }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                        .clickable { showChapterDialog = true },
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = BlueLight)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Set Progress", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = SilverLight)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        showChapterSelect = !showChapterSelect
                                        if (showChapterSelect) {
                                            viewModel.showChapterListOnly()
                                            onOpenReader()
                                        }
                                    },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp), tint = BlueAccent)
                                Spacer(Modifier.width(6.dp))
                                Text("All Chapters", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = SilverLight, letterSpacing = 0.3.sp)
                            }
                        }
                    }

                    if (chapters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(0.5.dp, GlassStroke)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Quick Jump",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.3.sp
                                    )
                                    Text(
                                        "Ch. $currentChapter of ${chapters.size}",
                                        color = SilverDark,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                val totalCh = chapters.size
                                val maxVisible = minOf(totalCh, 7)
                                val startIdx = (currentChapter - 3).coerceAtLeast(0)
                                val endIdx = (startIdx + maxVisible).coerceAtMost(totalCh)
                                val visibleChapters = chapters.subList(startIdx, endIdx)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    visibleChapters.forEachIndexed { idx, ch ->
                                        val chNum = startIdx + idx + 1
                                        val isCurrent = chNum == currentChapter
                                        val isRead = (startIdx + idx) in readChapterIndices
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                viewModel.selectChapter(startIdx + idx)
                                                onOpenReaderDirect()
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isCurrent -> BlueAccent
                                                            isRead -> ReadGreen.copy(alpha = 0.2f)
                                                            else -> DarkSurfaceVariant.copy(alpha = 0.5f)
                                                        }
                                                    )
                                                    .then(
                                                        if (isCurrent) Modifier.border(2.dp, BlueLight.copy(alpha = 0.5f), CircleShape)
                                                        else Modifier
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$chNum",
                                                    color = when {
                                                        isCurrent -> Color.White
                                                        isRead -> ReadGreen
                                                        else -> SilverDark
                                                    },
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (isCurrent) "Now" else if (isRead) "Read" else "",
                                                color = if (isCurrent) BlueLight else ReadGreen.copy(alpha = 0.7f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (detail.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = GlassStroke, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                "Genres",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                detail.genres.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                            .border(0.5.dp, GlassStroke, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            genre,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SilverLight.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (detail.otherNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = GlassStroke, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                "Also Known As",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = detail.otherNames.joinToString("  ·  "),
                                color = SilverDark,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    if (detail.synopsis.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = GlassStroke, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                "Synopsis",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = detail.synopsis,
                                color = SilverLight.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            else -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to load manga details", color = Color.White)
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        if (showChapterDialog) {
            ChapterProgressDialog(
                currentChapter = currentChapter,
                totalChapters = detail?.totalChapterCount ?: 0,
                onSet = { chapter ->
                    viewModel.setManualChapterProgress(chapter)
                    currentChapter = chapter + 1
                    showChapterDialog = false
                },
                onDismiss = { showChapterDialog = false }
            )
        }
    }
}

@Composable
fun ChapterProgressDialog(
    currentChapter: Int,
    totalChapters: Int,
    onSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(currentChapter.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Set Chapter Progress", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Enter the chapter you're currently on (1-$totalChapters):",
                    color = SilverDark,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = BlueAccent,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chapter = input.toIntOrNull()
                    if (chapter != null && chapter in 1..totalChapters.coerceAtLeast(1)) {
                        onSet(chapter)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SilverDark)
            }
        }
    )
}
