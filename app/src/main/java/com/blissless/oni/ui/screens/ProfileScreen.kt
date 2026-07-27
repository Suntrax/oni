package com.blissless.oni.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.oni.data.AniListFavorite
import com.blissless.oni.data.AniListUserActivity
import com.blissless.oni.ui.theme.StatusColors
import com.blissless.oni.ui.theme.StatusCompleted
import com.blissless.oni.ui.theme.StatusPaused
import com.blissless.oni.ui.theme.StatusDropped
import com.blissless.oni.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class ProfileSection { ABOUT_ME, FAVORITES, HISTORY }

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onMangaClick: (Int) -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val userFavorites by viewModel.userFavorites.collectAsState()
    val userActivity by viewModel.userActivity.collectAsState()
    val isProfileLoading by viewModel.isProfileLoading.collectAsState()
    val anilistUsername by viewModel.anilistUsername.collectAsState()
    val isLoggedIn = anilistUsername != null

    val readingCount by viewModel.readingCount.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val planningCount by viewModel.planningCount.collectAsState()
    val onHoldCount by viewModel.onHoldCount.collectAsState()
    val droppedCount by viewModel.droppedCount.collectAsState()

    var selectedSection by remember { mutableStateOf(ProfileSection.ABOUT_ME) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val slideOffset = remember { Animatable(600f) }
    val dismissSlideOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        slideOffset.animateTo(targetValue = 0f, animationSpec = tween(200, easing = LinearEasing))
    }

    fun dismissWithAnimation() {
        scope.launch {
            dismissSlideOffset.snapTo(0f)
            dismissSlideOffset.animateTo(targetValue = 600f, animationSpec = tween(150, easing = LinearEasing))
            onBack()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (slideOffset.value > 0 || dismissSlideOffset.value > 0) 0f else 1f,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing), label = "alpha"
    )

    LaunchedEffect(Unit) {
        if (isLoggedIn) {
            viewModel.loadUserProfile()
        }
    }

    BackHandler { dismissWithAnimation() }

    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    Card(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, (slideOffset.value + dismissSlideOffset.value).roundToInt()) }
            .graphicsLayer { this.alpha = alpha }
            .padding(0.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar (matches tensei)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = statusBarsPadding.calculateTopPadding() + 8.dp,
                        start = 8.dp, end = 8.dp, bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { dismissWithAnimation() }) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    when (selectedSection) {
                        ProfileSection.ABOUT_ME -> "About Me"
                        ProfileSection.FAVORITES -> "Favorites"
                        ProfileSection.HISTORY -> "History"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.weight(1f))
                // Share button (matches tensei: only on About Me when logged in)
                if (selectedSection == ProfileSection.ABOUT_ME && isLoggedIn) {
                    IconButton(onClick = {
                        val username = anilistUsername ?: return@IconButton
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://anilist.co/user/$username")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
                    }) {
                        Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }

            // Content area
            if (!isLoggedIn) {
                NotLoggedInState(onLoginClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.getAnilistAuthUrl()))
                    context.startActivity(intent)
                })
            } else if (isProfileLoading && userProfile == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    when (selectedSection) {
                        ProfileSection.ABOUT_ME -> AboutMeContent(
                            username = userProfile?.name ?: anilistUsername ?: "User",
                            userAvatar = userProfile?.avatarUrl ?: viewModel.getAvatarUrl(),
                            userBanner = userProfile?.bannerUrl,
                            userBio = userProfile?.about,
                            userCreatedAt = userProfile?.createdAt,
                            meanScore = userProfile?.meanScore,
                            activities = userActivity,
                            readingCount = readingCount,
                            completedCount = completedCount,
                            planningCount = planningCount,
                            onHoldCount = onHoldCount,
                            droppedCount = droppedCount,
                            totalManga = readingCount + completedCount + planningCount + onHoldCount + droppedCount
                        )
                        ProfileSection.FAVORITES -> FavoritesContent(favorites = userFavorites, onMangaClick = onMangaClick)
                        ProfileSection.HISTORY -> HistoryContent(activities = userActivity, onMangaClick = onMangaClick)
                    }
                }
            }

            // Bottom nav bar (matches tensei)
            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(
                            start = 24.dp, end = 24.dp, top = 12.dp,
                            bottom = navigationBarsPadding.calculateBottomPadding() + 12.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileNavButton(
                        icon = Icons.Default.Person, title = "About Me",
                        isSelected = selectedSection == ProfileSection.ABOUT_ME,
                        onClick = { selectedSection = ProfileSection.ABOUT_ME }
                    )
                    ProfileNavButton(
                        icon = Icons.Default.Favorite, title = "Favorites",
                        isSelected = selectedSection == ProfileSection.FAVORITES,
                        onClick = { selectedSection = ProfileSection.FAVORITES },
                        badge = userFavorites.size
                    )
                    ProfileNavButton(
                        icon = Icons.Default.History, title = "History",
                        isSelected = selectedSection == ProfileSection.HISTORY,
                        onClick = { selectedSection = ProfileSection.HISTORY },
                        badge = userActivity.size
                    )
                }
            }
        }
    }
}

// ── Bottom nav button (matches tensei) ──────────────────────────────────

@Composable
private fun ProfileNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badge: Int? = null
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                icon, contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            if (badge != null && badge > 0) {
                Badge(
                    modifier = Modifier.offset(x = 10.dp, y = (-4).dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(badge.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ── Not logged in state ─────────────────────────────────────────────────

@Composable
private fun NotLoggedInState(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(
            Icons.Default.Person, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Not logged in",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Login with AniList to see your profile",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(
            onClick = onLoginClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Login with AniList")
        }
    }
}

// ── Tab 1: About Me (matches tensei exactly) ────────────────────────────

@Composable
private fun AboutMeContent(
    username: String,
    userAvatar: String? = null,
    userBanner: String? = null,
    userBio: String? = null,
    userCreatedAt: Long? = null,
    meanScore: Float? = null,
    activities: List<AniListUserActivity> = emptyList(),
    readingCount: Int = 0,
    completedCount: Int = 0,
    planningCount: Int = 0,
    onHoldCount: Int = 0,
    droppedCount: Int = 0,
    totalManga: Int = 0
) {
    var showFullscreenAvatar by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Banner + Avatar overlap (tensei-style)
            Box(modifier = Modifier.fillMaxWidth()) {
                // Banner (full width, 160dp)
                userBanner?.let { bannerUrl ->
                    AsyncImage(
                        model = bannerUrl, contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                // Avatar overlapping the bottom of the banner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 40.dp)
                ) {
                    userAvatar?.let { avatarUrl ->
                        AsyncImage(
                            model = avatarUrl, contentDescription = "Avatar",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .clickable { showFullscreenAvatar = true },
                            contentScale = ContentScale.Crop
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Username (matches tensei)
            Text(
                username,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Bio (matches tensei)
            userBio?.let { bio ->
                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 5,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Join date (matches tensei: "Joined d MMMM, yyyy")
            userCreatedAt?.let { timestamp ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Joined ${formatDate(timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats row (manga: chapters read + score only)
            val totalChaptersRead = activities.sumOf { it.progress }
            val hasStats = totalChaptersRead > 0 || meanScore != null
            if (hasStats) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        StatCard(
                            value = totalChaptersRead.toString(),
                            label = "Chapters",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        StatCard(
                            value = meanScore?.let { "%.1f".format(it) } ?: "-",
                            label = "Mean Score",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Library stats (tensei-style status breakdown)
            if (totalManga > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LibraryStatRow(label = "Reading", count = readingCount, total = totalManga, color = StatusColors["CURRENT"] ?: MaterialTheme.colorScheme.primary)
                    LibraryStatRow(label = "Completed", count = completedCount, total = totalManga, color = StatusColors["COMPLETED"] ?: StatusCompleted)
                    LibraryStatRow(label = "Planning", count = planningCount, total = totalManga, color = StatusColors["PLANNING"] ?: MaterialTheme.colorScheme.tertiary)
                    LibraryStatRow(label = "On Hold", count = onHoldCount, total = totalManga, color = StatusColors["ON_HOLD"] ?: StatusPaused)
                    LibraryStatRow(label = "Dropped", count = droppedCount, total = totalManga, color = StatusColors["DROPPED"] ?: StatusDropped)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Fullscreen avatar dialog (matches tensei)
        if (showFullscreenAvatar) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showFullscreenAvatar = false }) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.fillMaxSize().clickable { showFullscreenAvatar = false })
                    userAvatar?.let { avatarUrl ->
                        AsyncImage(
                            model = avatarUrl, contentDescription = "Avatar",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Library stat bar (tensei-style) ──────────────────────────────────────

@Composable
private fun LibraryStatRow(label: String, count: Int, total: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val fraction = if (total > 0) count.toFloat() / total else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}

// ── Shared composables ──────────────────────────────────────────────────

@Composable
private fun StatCard(value: String, label: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Tab 2: Favorites (tensei-style horizontal cards) ───────────────────

@Composable
private fun FavoritesContent(favorites: List<AniListFavorite>, onMangaClick: (Int) -> Unit = {}) {
    if (favorites.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Favorite, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("No favorites yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(favorites) { _, fav ->
                FavoriteCard(fav = fav, onClick = { if (fav.id > 0) onMangaClick(fav.id) })
            }
        }
    }
}

@Composable
private fun FavoriteCard(fav: AniListFavorite, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = fav.coverUrl, contentDescription = fav.title,
                modifier = Modifier
                    .width(50.dp)
                    .height(70.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fav.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Format + year
                val formatYear = listOfNotNull(
                    fav.format?.replaceFirstChar { it.uppercase() },
                    fav.year?.toString()
                ).joinToString(" • ")
                if (formatYear.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        formatYear,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                // Score
                if (fav.score != null && fav.score > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "\u2605 ${fav.score}%",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Tab 3: History (tensei-style) ────────────────────────────────────────

@Composable
private fun HistoryContent(activities: List<AniListUserActivity>, onMangaClick: (Int) -> Unit = {}) {
    if (activities.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("No reading history", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(activities) { _, activity ->
                HistoryItem(activity = activity, onClick = { if (activity.mediaId > 0) onMangaClick(activity.mediaId) })
            }
        }
    }
}

@Composable
private fun HistoryItem(activity: AniListUserActivity, onClick: () -> Unit = {}) {
    val statusUpper = activity.status.uppercase()
    val statusKey = when {
        statusUpper == "REPEATING" -> "CURRENT"
        statusUpper == "HOLD" -> "ON_HOLD"
        else -> statusUpper
    }
    val statusColor = StatusColors[statusKey] ?: MaterialTheme.colorScheme.onSurfaceVariant
    val statusLabel = when {
        statusKey == "CURRENT" -> "Reading"
        statusKey == "COMPLETED" -> "Completed"
        statusKey == "PLANNING" -> "Planning"
        statusKey == "ON_HOLD" -> "On Hold"
        statusKey == "DROPPED" -> "Dropped"
        else -> activity.status
    }

    val relativeTime = remember(activity.updatedAt) { formatRelativeTime(activity.updatedAt) }

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = activity.coverUrl, contentDescription = activity.mangaTitle,
                modifier = Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Manga title
                Text(
                    activity.mangaTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Status chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Chapter progress + time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activity.totalChapters != null && activity.totalChapters > 0) {
                        Text(
                            "Ch. ${activity.progress} / ${activity.totalChapters}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (activity.progress > 0) {
                        Text(
                            "Ch. ${activity.progress}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        relativeTime,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun formatMinutesWatched(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return "-"
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "$days days"
        hours > 0 -> "$hours hours"
        else -> "$minutes min"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("d MMMM, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000))
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 604800}w ago"
        else -> {
            val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
            sdf.format(Date(timestamp * 1000))
        }
    }
}
