package com.blissless.oni.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.oni.data.AniListCharacterEntry
import com.blissless.oni.data.AniListMangaDetail
import com.blissless.oni.data.AniListRankingEntry
import com.blissless.oni.data.AniListRelationEntry
import com.blissless.oni.data.AniListSearchResult
import com.blissless.oni.data.AniListStaffEntry
import com.blissless.oni.data.AniListTag
import com.blissless.oni.data.ChapterInfo
import com.blissless.oni.data.ChapterDownloadResult
import com.blissless.oni.data.ReadingStatus
import com.blissless.oni.ui.theme.GradientBlue
import com.blissless.oni.ui.theme.GradientPurple
import com.blissless.oni.ui.theme.GradientTeal
import com.blissless.oni.ui.theme.ReadGreen
import com.blissless.oni.ui.theme.StatusColors
import com.blissless.oni.viewmodel.MainViewModel
import com.blissless.oni.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MangaDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenReader: () -> Unit,
    onOpenReaderDirect: () -> Unit,
    onOpenChapterSelect: () -> Unit
) {
    val mangaDetail by viewModel.mangaDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val readChapterIndices by viewModel.readChapterIndices.collectAsState()
    val chapterImages by viewModel.chapterImages.collectAsState()
    val isFavorited by viewModel.currentMangaIsFavorited.collectAsState()
    // MangaDex-derived counts - used as fallbacks when AniList doesn't have them.
    val mangaDexChapterCount by viewModel.mangaDexChapterCount.collectAsState()
    val mangaDexVolumeCount by viewModel.mangaDexVolumeCount.collectAsState()
    val detail = mangaDetail

    var currentStatus by remember(detail?.id) { mutableStateOf<ReadingStatus?>(null) }
    var currentChapter by remember(detail?.id) { mutableStateOf(0.0) }
    var showChapterSelect by remember(detail?.id) { mutableStateOf(false) }
    var fallbackCoverUrl by remember(detail?.id) {
        mutableStateOf(detail?.coverExtraLarge?.takeIf { it.isNotBlank() } ?: detail?.coverLarge?.takeIf { it.isNotBlank() } ?: viewModel.getCurrentMangaCoverUrl())
    }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showChapterDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isContinueReading by remember { mutableStateOf(false) }
    val selectedExtensionAuthority by viewModel.selectedExtensionAuthority.collectAsState()
    val openChapterSelectOnLoad by viewModel.openChapterSelectOnLoad.collectAsState()
    val context = LocalContext.current

    // Auto-open chapter select when requested (e.g., from "Read Now" button).
    // Keyed on detail so the effect re-fires once manga data finishes loading.
    LaunchedEffect(openChapterSelectOnLoad, detail, selectedExtensionAuthority) {
        if (openChapterSelectOnLoad && detail != null && selectedExtensionAuthority != null) {
            viewModel.consumeOpenChapterSelect()
            showChapterSelect = true
            viewModel.showChapterListOnly()
            onOpenReader()
        }
    }

    // Navigate to reader once chapter images finish loading after pressing
    // the continue reading button. The loading overlay blocks interaction
    // until this fires. Also reset the flag on error so the overlay dismisses
    // (e.g. no extension selected).
    LaunchedEffect(isContinueReading, isLoading, chapterImages) {
        if (isContinueReading) {
            when (chapterImages) {
                is UiState.Success -> {
                    isContinueReading = false
                    onOpenReaderDirect()
                }
                is UiState.Error -> {
                    isContinueReading = false
                }
                else -> {}
            }
        }
    }

    fun refreshTracking() {
        detail?.let { d ->
            val mangaId = "anilist_${d.id}"
            viewModel.refreshTrackingLists()
            val tracking = viewModel.resolveMangaTracking(mangaId)
            currentStatus = tracking?.status
            currentChapter = tracking?.currentChapterNumber ?: 0.0
            // If we have a loaded chapter list, derive the correct chapter number
            // from the list using the stored index. This is more reliable than
            // currentChapterNumber which may be an AniList number (offset from
            // the extension's numbering when partial chapters exist).
            if (chapters.isNotEmpty() && tracking != null) {
                val idx = tracking.currentChapterIndex.coerceIn(0, chapters.lastIndex)
                val chapter = chapters[idx]
                val parsed = chapter.title
                    ?.removePrefix("Chapter ")
                    ?.removePrefix("Ch. ")
                    ?.substringBefore(":")
                    ?.trim()
                    ?.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    currentChapter = parsed
                }
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
    LaunchedEffect(detail?.id) {
        viewModel.checkCurrentMangaFavorite()
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            detail != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    HeaderSection(detail, fallbackCoverUrl)
                    Spacer(modifier = Modifier.height(20.dp))
                    StatsCard(
                        detail = detail,
                        actualChapters = chapters.size,
                        mangaDexChapterCount = mangaDexChapterCount,
                        mangaDexVolumeCount = mangaDexVolumeCount
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ActionButtonsCard(
                        detail = detail,
                        currentStatus = currentStatus,
                        currentChapter = currentChapter,
                        chapters = chapters,
                        viewModel = viewModel,
                        showStatusMenu = showStatusMenu,
                        onStatusMenuToggle = { showStatusMenu = it },
                        showChapterDialog = showChapterDialog,
                        onChapterDialogToggle = { showChapterDialog = it },
                        onStatusDialogShow = { showStatusDialog = true },
                        onStartReading = {
                            if (selectedExtensionAuthority == null) {
                                Toast.makeText(context, "Select a default extension in Settings first", Toast.LENGTH_SHORT).show()
                            } else {
                                isContinueReading = true
                                viewModel.continueFromCurrentManga()
                            }
                        },
                        onOpenChapterSelect = {
                            if (selectedExtensionAuthority == null) {
                                Toast.makeText(context, "Select a default extension in Settings first", Toast.LENGTH_SHORT).show()
                            } else {
                                showChapterSelect = !showChapterSelect
                                if (showChapterSelect) {
                                    viewModel.showChapterListOnly()
                                    onOpenReader()
                                }
                            }
                        },
                        onStatusChange = { status ->
                            viewModel.updateTrackingStatus("anilist_${detail.id}", status)
                            currentStatus = status
                        },
                        onRemoveAnilist = {
                            viewModel.removeFromAnilist("anilist_${detail.id}")
                            currentStatus = null
                        },
                        onDownloadClick = {
                            if (selectedExtensionAuthority == null) {
                                Toast.makeText(context, "Select a default extension in Settings first", Toast.LENGTH_SHORT).show()
                            } else {
                                showDownloadDialog = true
                            }
                        },
                        isFavorited = isFavorited,
                        onToggleFavorite = { viewModel.toggleCurrentMangaFavorite() }
                    )

                    if (showStatusDialog) {
                        MangaStatusDialog(
                            title = detail.titleRomaji,
                            coverUrl = detail.coverExtraLarge ?: detail.coverLarge ?: fallbackCoverUrl,
                            currentStatus = currentStatus?.let { status ->
                                when (status) {
                                    ReadingStatus.READING -> "CURRENT"
                                    ReadingStatus.PLANNING -> "PLANNING"
                                    ReadingStatus.COMPLETED -> "COMPLETED"
                                    ReadingStatus.ON_HOLD -> "PAUSED"
                                    ReadingStatus.DROPPED -> "DROPPED"
                                    else -> ""
                                }
                            } ?: "",
                            currentChapterNumber = currentChapter,
                            totalChapters = detail.chapters ?: 0,
                            onDismiss = { showStatusDialog = false },
                            onRemove = {
                                viewModel.removeFromAnilist("anilist_${detail.id}")
                                currentStatus = null
                                showStatusDialog = false
                            },
                            onUpdate = { status, progress ->
                                viewModel.updateTrackingStatus("anilist_${detail.id}", status, progress)
                                currentStatus = status
                                if (progress != null) {
                                    currentChapter = progress
                                }
                                showStatusDialog = false
                            }
                        )
                    }

                    if (showDownloadDialog && chapters.isNotEmpty()) {
                        DownloadChapterDialog(
                            mangaTitle = detail.titleRomaji,
                            mangaId = "anilist_${detail.id}",
                            chapters = chapters,
                            readChapterIndices = readChapterIndices,
                            viewModel = viewModel,
                            coverUrl = fallbackCoverUrl,
                            onDismiss = { showDownloadDialog = false }
                        )
                    }

                    if (!detail.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        SynopsisSection(detail.description)
                    }

                    if (detail.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        GenresSection(detail.genres)
                    }

                    if (detail.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        TagsSection(detail.tags)
                    }

                    if (detail.characters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        CharactersSection(detail.characters)
                    }

                    if (detail.relations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        RelationsSection(detail.relations)
                    }

                    if (detail.rankings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        RankingsSection(detail.rankings)
                    }

                    if (detail.recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        RecommendationsSection(detail.recommendations)
                    }

                    if (detail.synonyms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        SynonymsSection(detail.synonyms)
                    }

                    if (detail.staff.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        StaffSection(detail.staff)
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
                .padding(start = 4.dp, top = 8.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        if (detail != null && (isLoading || isContinueReading)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading...", color = Color.White)
                }
            }
        }

        if (showChapterDialog) {
            ChapterProgressDialog(
                currentChapter = currentChapter,
                totalChapters = chapters.size
                    .coerceAtLeast(detail?.chapters ?: 0)
                    .coerceAtLeast(mangaDexChapterCount ?: 0),
                onSet = { chapter ->
                    viewModel.setManualChapterProgress(chapter)
                    currentChapter = chapter
                    showChapterDialog = false
                },
                onDismiss = { showChapterDialog = false }
            )
        }
    }
}

@Composable
private fun HeaderSection(detail: AniListMangaDetail, fallbackCoverUrl: String?) {
    val coverModel = detail.coverExtraLarge ?: detail.coverLarge ?: fallbackCoverUrl

    Box(modifier = Modifier.fillMaxWidth().height(300.dp).clipToBounds()) {
        val bannerHeight = 160.dp
        val coverHeightDp = (120 * 0.7f).dp
        val bannerOffsetY = (300.dp - 16.dp - coverHeightDp / 2 - bannerHeight / 2)

        if (detail.bannerImage != null) {
            AsyncImage(
                model = detail.bannerImage,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
                    .offset(y = bannerOffsetY),
                contentScale = ContentScale.Crop
            )
        } else if (coverModel != null) {
            AsyncImage(
                model = coverModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
                    .offset(y = bannerOffsetY),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
                    .offset(y = bannerOffsetY)
                    .background(
                        Brush.linearGradient(
                            0f to MaterialTheme.colorScheme.surface,
                            1f to MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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
                            MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.background
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
            if (coverModel != null) {
                AsyncImage(
                    model = coverModel,
                    contentDescription = detail.titleRomaji,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(detail.titleRomaji.take(2), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail.titleRomaji,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail.titleEnglish != null && detail.titleEnglish.isNotBlank() && detail.titleEnglish != detail.titleRomaji) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detail.titleEnglish,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val formatLabel = formatLabel(detail.format)
                    if (formatLabel != null) {
                        Text(
                            text = formatLabel,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = formatStatus(detail.status),
                        fontSize = 11.sp,
                        color = if (detail.status == "RELEASING") Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (detail.source != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Source: ${formatSource(detail.source)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                if (detail.startYear != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = buildString {
                                append(formatDate(detail.startYear, detail.startMonth, detail.startDay))
                                val endDate = formatDate(detail.endYear, detail.endMonth, detail.endDay)
                                if (endDate != "?") {
                                    append(" - $endDate")
                                }
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (detail.countryOfOrigin != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = detail.countryOfOrigin!!,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    detail: AniListMangaDetail,
    actualChapters: Int = 0,
    mangaDexChapterCount: Int? = null,
    mangaDexVolumeCount: Int? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Default.Star,
                value = detail.averageScore?.let { "${it / 10f}" } ?: "-",
                label = "Score",
                color = Color(0xFFfbbf24)
            )
            // Chapters: prefer AniList, fall back to MangaDex aggregate, then to
            // the loaded chapter list size. We display whenever ANY source has a
            // positive value, so the user always sees a chapter count.
            val displayChapters = detail.chapters
                ?.takeIf { it > 0 }
                ?: mangaDexChapterCount?.takeIf { it > 0 }
                ?: actualChapters.takeIf { it > 0 }
                ?: 0
            if (displayChapters > 0) {
                DividerDot()
                StatItem(
                    icon = null,
                    value = "$displayChapters",
                    label = "Chapters",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // Volumes: prefer AniList, fall back to MangaDex aggregate volume count.
            val displayVolumes = detail.volumes
                ?.takeIf { it > 0 }
                ?: mangaDexVolumeCount?.takeIf { it > 0 }
            if (displayVolumes != null) {
                DividerDot()
                StatItem(
                    icon = null,
                    value = "$displayVolumes",
                    label = "Volumes",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (detail.popularity != null) {
                DividerDot()
                StatItem(
                    icon = Icons.Default.TrendingUp,
                    value = formatNumber(detail.popularity),
                    label = "Popular",
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }
            if (detail.favourites != null && detail.favourites!! > 0) {
                DividerDot()
                StatItem(
                    icon = Icons.Default.Favorite,
                    value = formatNumber(detail.favourites!!),
                    label = "Fav",
                    color = Color(0xFFec4899)
                )
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector?, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = if (value.length > 4) 13.sp else 15.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DividerDot() {
    Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun ActionButtonsCard(
    detail: AniListMangaDetail,
    currentStatus: ReadingStatus?,
    currentChapter: Double,
    chapters: List<*>,
    viewModel: MainViewModel,
    showStatusMenu: Boolean,
    onStatusMenuToggle: (Boolean) -> Unit,
    showChapterDialog: Boolean,
    onChapterDialogToggle: (Boolean) -> Unit,
    onStatusDialogShow: () -> Unit = {},
    onStartReading: () -> Unit,
    onOpenChapterSelect: () -> Unit,
    onStatusChange: (ReadingStatus) -> Unit,
    onRemoveAnilist: () -> Unit,
    onDownloadClick: () -> Unit = {},
    isFavorited: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val totalAvailable = chapters.size.coerceAtLeast(detail.chapters ?: 0)
            val hasNextChapter = totalAvailable == 0 || currentChapter.toInt() + 1 <= totalAvailable
            if (hasNextChapter) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartReading,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (currentStatus == ReadingStatus.READING) "Read Now" else "Start Reading",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .height(52.dp)
                            .width(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isFavorited) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                    ) {
                        Icon(
                            if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(20.dp),
                            tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onStatusDialogShow),
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
                            ReadingStatus.READING -> StatusColors["READING"] ?: MaterialTheme.colorScheme.primary
                            ReadingStatus.PLANNING -> StatusColors["PLANNING"] ?: MaterialTheme.colorScheme.primary
                            ReadingStatus.COMPLETED -> StatusColors["COMPLETED"] ?: MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                            ReadingStatus.READING -> StatusColors["READING"] ?: MaterialTheme.colorScheme.primary
                            ReadingStatus.PLANNING -> StatusColors["PLANNING"] ?: MaterialTheme.colorScheme.primary
                            ReadingStatus.COMPLETED -> StatusColors["COMPLETED"] ?: MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onOpenChapterSelect),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Chapters", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, letterSpacing = 0.3.sp)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onDownloadClick),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Download", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, letterSpacing = 0.3.sp)
                }
            }

        }
    }
}

@Composable
private fun StatusButton(
    currentStatus: ReadingStatus?,
    showStatusMenu: Boolean,
    onStatusMenuToggle: (Boolean) -> Unit,
    onStatusChange: (ReadingStatus) -> Unit,
    onRemoveAnilist: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onStatusMenuToggle(true) },
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
                    ReadingStatus.READING -> StatusColors["READING"] ?: MaterialTheme.colorScheme.primary
                    ReadingStatus.PLANNING -> StatusColors["PLANNING"] ?: MaterialTheme.colorScheme.primary
                    ReadingStatus.COMPLETED -> StatusColors["COMPLETED"] ?: MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                    ReadingStatus.READING -> StatusColors["READING"] ?: MaterialTheme.colorScheme.primary
                    ReadingStatus.PLANNING -> StatusColors["PLANNING"] ?: MaterialTheme.colorScheme.primary
                    ReadingStatus.COMPLETED -> StatusColors["COMPLETED"] ?: MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        DropdownMenu(
            expanded = showStatusMenu,
            onDismissRequest = { onStatusMenuToggle(false) }
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
                    ReadingStatus.READING -> StatusColors["READING"] ?: MaterialTheme.colorScheme.primary
                    ReadingStatus.PLANNING -> StatusColors["PLANNING"] ?: MaterialTheme.colorScheme.primary
                    ReadingStatus.COMPLETED -> StatusColors["COMPLETED"] ?: MaterialTheme.colorScheme.primary
                    ReadingStatus.ON_HOLD -> StatusColors["PAUSED"] ?: MaterialTheme.colorScheme.primary
                    ReadingStatus.DROPPED -> StatusColors["DROPPED"] ?: MaterialTheme.colorScheme.error
                }
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                            Spacer(Modifier.width(10.dp))
                            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    onClick = {
                        onStatusMenuToggle(false)
                        if (currentStatus != status) {
                            onStatusChange(status)
                        }
                    }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Remove from AniList", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                onClick = {
                    onStatusMenuToggle(false)
                    onRemoveAnilist()
                }
            )
        }
    }
}

@Composable
private fun ProgressButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primaryContainer)
        Spacer(Modifier.width(6.dp))
        Text("Set Progress", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun GenresSection(genres: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Genres")
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            genres.forEach { genre ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(genre, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun TagsSection(tags: List<AniListTag>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Tags")
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            tags.sortedByDescending { it.rank }.take(20).forEach { tag ->
                val tagColor = when {
                    tag.rank >= 80 -> MaterialTheme.colorScheme.primary
                    tag.rank >= 50 -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(tagColor.copy(alpha = 0.12f))
                        .border(0.5.dp, tagColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${tag.name} ${(tag.rank / 10)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = tagColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CharactersSection(characters: List<AniListCharacterEntry>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Characters")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(characters) { char ->
                Card(
                    modifier = Modifier.width(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (char.image != null) {
                            AsyncImage(
                                model = char.image,
                                contentDescription = char.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(char.name.take(2), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = char.name,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = char.role,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationsSection(relations: List<AniListRelationEntry>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Relations")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(relations) { rel ->
                Card(
                    modifier = Modifier.width(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            if (rel.coverUrl != null) {
                                AsyncImage(
                                    model = rel.coverUrl,
                                    contentDescription = rel.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = formatRelationType(rel.relationType),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = rel.title,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingsSection(rankings: List<AniListRankingEntry>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Rankings")
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rankings.take(6).forEach { ranking ->
                val color = when {
                    ranking.rank <= 10 -> Color(0xFFfbbf24)
                    ranking.rank <= 100 -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.1f))
                        .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "#${ranking.rank}",
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = ranking.context + if (ranking.allTime) " (All Time)" else "",
                            color = color.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationsSection(recommendations: List<AniListSearchResult>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Recommendations")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(recommendations) { rec ->
                Card(
                    modifier = Modifier.width(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            if (rec.coverUrl != null) {
                                AsyncImage(
                                    model = rec.coverUrl,
                                    contentDescription = rec.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = rec.title,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (rec.meanScore != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFfbbf24), modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = "${rec.meanScore / 10f}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFfbbf24)
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

@Composable
private fun StaffSection(staff: List<AniListStaffEntry>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Staff")
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            staff.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    if (entry.image != null) {
                        AsyncImage(
                            model = entry.image,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(entry.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.role, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SynonymsSection(synonyms: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Also Known As")
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = synonyms.joinToString("  ·  "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SynopsisSection(description: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Synopsis")
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = description.replace(Regex("<[^>]*>"), ""),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun ExternalLinksSection(links: List<com.blissless.oni.data.AniListExternalLink>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("External Links")
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            links.forEach { link ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(link.site, fontSize = 11.sp, color = MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.8.sp)
}

@Composable
fun ChapterProgressDialog(
    currentChapter: Double,
    totalChapters: Int,
    onSet: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(if (currentChapter % 1.0 == 0.0) currentChapter.toInt().toString() else currentChapter.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Enter the chapter you last read", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Type an exact chapter number (up to $totalChapters):",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() || c == '.' } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
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
                    val chapter = input.toDoubleOrNull()
                    if (chapter != null && chapter >= 1.0) {
                        onSet(chapter)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun DownloadChapterDialog(
    mangaTitle: String,
    mangaId: String,
    chapters: List<ChapterInfo>,
    readChapterIndices: Set<Int>,
    viewModel: MainViewModel,
    coverUrl: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedChapters by remember { mutableStateOf(setOf<Int>()) }
    val allSelected = selectedChapters.size == chapters.size
    val downloadedChapterNums by viewModel.downloadedChapterNumbers.collectAsState()

    LaunchedEffect(mangaTitle) { viewModel.loadDownloadedChapterNumbers(mangaTitle) }

    val missingIndices = remember(chapters, downloadedChapterNums) {
        chapters.mapIndexedNotNull { index, ch ->
            val chNum = ch.title
                ?.removePrefix("Chapter ")
                ?.substringBefore(":")
                ?.trim()
                ?.toDoubleOrNull() ?: (index + 1).toDouble()
            if (chNum !in downloadedChapterNums) index else null
        }.toSet()
    }

    val batchState by viewModel.batchDownloadState.collectAsState()
    val isBatchActive = batchState != null && batchState?.mangaTitle == mangaTitle && !batchState!!.isComplete
    val isBatchComplete = batchState != null && batchState?.mangaTitle == mangaTitle && batchState!!.isComplete

    var isFetchingUrls by remember { mutableStateOf(false) }
    val chapterChNums = remember(chapters) {
        chapters.mapIndexed { index, ch ->
            ch.title
                ?.removePrefix("Chapter ")
                ?.substringBefore(":")
                ?.trim()
                ?.toDoubleOrNull() ?: (index + 1).toDouble()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isFetchingUrls && !isBatchActive) onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                when {
                    isBatchComplete -> "Download Complete"
                    isBatchActive -> "Downloading..."
                    else -> "Download Chapters"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    mangaTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!isFetchingUrls && !isBatchActive && !isBatchComplete) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${selectedChapters.size} of ${chapters.size} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isFetchingUrls) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetching chapter URLs...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (isBatchActive && batchState != null) {
                    val state = batchState!!
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (state.currentIndex + 1).toFloat() / state.totalChapters.coerceAtLeast(1).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val failedText = if (state.failedChapters > 0) ", ${state.failedChapters} failed" else ""
                    Text(
                        "Chapter ${state.currentIndex + 1}/${state.totalChapters} - ${state.completedChapters} done$failedText",
                        fontSize = 12.sp,
                        color = if (state.failedChapters > 0) Color(0xFFFFAB40) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isBatchComplete && batchState != null) {
                    val state = batchState!!
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = if (state.failedChapters == 0) Color(0xFF34D399) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val summary = when {
                        state.failedChapters == 0 -> "All ${state.completedChapters} chapters downloaded"
                        else -> "${state.completedChapters} downloaded, ${state.failedChapters} failed"
                    }
                    Text(
                        summary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state.failedChapters == 0) Color(0xFF34D399) else Color(0xFFFFAB40)
                    )
                }

                if (!isFetchingUrls && !isBatchActive) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Select all toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedChapters = if (allSelected) emptySet()
                                else chapters.indices.toSet()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (allSelected) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (allSelected) "Deselect All" else "Select All",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Select missing toggle
                    if (missingIndices.isNotEmpty() && missingIndices.size < chapters.size) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedChapters = if (selectedChapters == missingIndices) emptySet()
                                    else missingIndices
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (selectedChapters == missingIndices) Icons.Default.Check else Icons.Default.Download,
                                contentDescription = null,
                                tint = if (selectedChapters == missingIndices) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Select Missing (${missingIndices.size})",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Chapter list (scrollable)
                val chapterListState = rememberLazyListState()
                LaunchedEffect(batchState?.currentIndex) {
                    val idx = batchState?.currentIndex
                    if (idx != null && idx >= 0) {
                        chapterListState.animateScrollToItem(idx)
                    }
                }
                LazyColumn(
                    state = chapterListState,
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        val isSelected = index in selectedChapters && !isBatchActive && !isBatchComplete
                        val isRead = index in readChapterIndices
                        val chNumDouble = chapterChNums[index]
                        val chNum = chapterChNums[index].let { if (it == it.toLong().toDouble()) "${it.toLong()}" else "$it" }

                        // Batch download status for this chapter
                        val chapterResult = if (isBatchActive || isBatchComplete) {
                            val state = batchState
                            when {
                                state == null -> ChapterDownloadResult.PENDING
                                chNumDouble in state.completedNumbers -> ChapterDownloadResult.COMPLETED
                                chNumDouble in state.failedNumbers -> ChapterDownloadResult.FAILED
                                index == state.currentIndex && !state.isComplete -> {
                                    if (state.attempts > 1) ChapterDownloadResult.RETRYING else ChapterDownloadResult.DOWNLOADING
                                }
                                index > state.currentIndex -> ChapterDownloadResult.PENDING
                                else -> ChapterDownloadResult.COMPLETED
                            }
                        } else null

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        chapterResult == ChapterDownloadResult.DOWNLOADING || chapterResult == ChapterDownloadResult.RETRYING ->
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                        chapterResult == ChapterDownloadResult.COMPLETED ->
                                            Color(0xFF34D399).copy(alpha = 0.06f)
                                        chapterResult == ChapterDownloadResult.FAILED ->
                                            Color(0xFFFF5252).copy(alpha = 0.06f)
                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (!isBatchActive && !isBatchComplete) {
                                        Modifier.clickable {
                                            selectedChapters = if (isSelected) selectedChapters - index
                                            else selectedChapters + index
                                        }
                                    } else Modifier
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (chapterResult == null) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(
                                            1.5.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.size(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (chapterResult) {
                                        ChapterDownloadResult.COMPLETED -> {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Downloaded",
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        ChapterDownloadResult.FAILED -> {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Failed",
                                                tint = Color(0xFFFF5252),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        ChapterDownloadResult.DOWNLOADING -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        ChapterDownloadResult.RETRYING -> {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Retrying",
                                                tint = Color(0xFFFFAB40),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        ChapterDownloadResult.PENDING -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.outlineVariant)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Ch. $chNum",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isRead && chapterResult == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(56.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                chapter.title ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isRead && chapterResult == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Read",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isBatchComplete) {
                        onDismiss()
                        return@Button
                    }
                    val selected = selectedChapters.mapNotNull { idx ->
                        chapters.getOrNull(idx)
                    }
                    if (selected.isNotEmpty()) {
                        isFetchingUrls = true
                        Toast.makeText(context, "Downloading ${selected.size} chapter(s)...", Toast.LENGTH_SHORT).show()
                        val mediaId = viewModel.mangaDetail.value?.id ?: 0
                        val mangaIdStr = "anilist_$mediaId"
                        if (!coverUrl.isNullOrBlank()) {
                            viewModel.saveCoverImage(mangaTitle, coverUrl)
                        }
                        var chaptersRemaining = selected.size
                        val chapterTriples = mutableListOf<Triple<Double, String, List<String>>>()
                        selected.forEach { ch ->
                            val chNum = ch.title
                                ?.removePrefix("Chapter ")
                                ?.substringBefore(":")
                                ?.trim()
                                ?: "${chapters.indexOf(ch) + 1}"
                            viewModel.fetchChapterImagesForDownload(
                                mangaTitle = mangaTitle,
                                chapterNumber = chNum
                            ) { result ->
                                val images = result.getOrDefault(emptyList())
                                val num = chNum.toDoubleOrNull() ?: 0.0
                                synchronized(chapterTriples) {
                                    chapterTriples.add(Triple(num, ch.url, images))
                                    chaptersRemaining--
                                    if (chaptersRemaining == 0) {
                                        isFetchingUrls = false
                                        viewModel.downloadSelectedChapters(
                                            mangaTitle = mangaTitle,
                                            mangaId = mangaIdStr,
                                            chapters = chapterTriples.toList()
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = (selectedChapters.isNotEmpty() && !isFetchingUrls && !isBatchActive) || isBatchComplete,
                shape = RoundedCornerShape(12.dp)
            ) {
                when {
                    isFetchingUrls -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetching...")
                    }
                    isBatchActive -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Downloading...")
                    }
                    isBatchComplete -> {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Close")
                    }
                    else -> {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download ${selectedChapters.size}")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isFetchingUrls && !isBatchActive
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// ======================== Utility Functions ========================

private fun formatLabel(format: String?): String? = when (format?.uppercase()) {
    "MANGA" -> "Manga"
    "NOVEL" -> "Light Novel"
    "ONE_SHOT" -> "One-Shot"
    "DOUJIN" -> "Doujin"
    "MANGA_KEN" -> "Manga"
    else -> format
}

private fun formatStatus(status: String?): String = when (status?.uppercase()) {
    "RELEASING" -> "Ongoing"
    "FINISHED" -> "Completed"
    "NOT_YET_RELEASED" -> "Not Released"
    "CANCELLED" -> "Cancelled"
    "HIATUS" -> "On Hiatus"
    else -> status ?: ""
}

private fun formatSource(source: String?): String = when (source?.uppercase()) {
    "ORIGINAL" -> "Original"
    "MANGA" -> "Manga"
    "LIGHT_NOVEL" -> "Light Novel"
    "VISUAL_NOVEL" -> "Visual Novel"
    "WEB_MANGA" -> "Web Manga"
    "NOVEL" -> "Novel"
    "DOUJIN" -> "Doujin"
    "GAME" -> "Game"
    "ANIME" -> "Anime"
    "OTHER" -> "Other"
    else -> source ?: "Unknown"
}

private fun formatRelationType(type: String?): String = when (type?.uppercase()) {
    "SEQUEL" -> "Sequel"
    "PREQUEL" -> "Prequel"
    "ALTERNATIVE" -> "Alternative"
    "SIDE_STORY" -> "Side Story"
    "SPIN_OFF" -> "Spin-off"
    "SUMMARY" -> "Summary"
    "COMPILATION" -> "Compilation"
    "CHARACTER" -> "Character"
    "OTHER" -> "Related"
    else -> type ?: "Related"
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 1_000_000 -> String.format("%.1fM", num / 1_000_000f)
        num >= 1_000 -> String.format("%.1fK", num / 1_000f)
        else -> num.toString()
    }
}

private fun formatDate(year: Int?, month: Int?, day: Int?): String {
    val y = if (year != null && year > 0) year else return "?"
    return if (month != null && month in 1..12 && day != null && day in 1..31) {
        "${getMonthAbbr(month)} $day, $y"
    } else {
        "$y"
    }
}

private fun getMonthAbbr(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> "?"
}
