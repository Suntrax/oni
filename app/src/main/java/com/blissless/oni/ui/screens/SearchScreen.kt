package com.blissless.oni.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.oni.data.AniListSearchResult
import com.blissless.oni.viewmodel.MainViewModel
import com.blissless.oni.viewmodel.UiState
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

val ALL_GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror",
    "Mystery", "Romance", "Sci-Fi", "Slice of Life", "Sports",
    "Thriller", "Supernatural", "Music", "Psychological",
    "Military", "Mecha", "School", "Seinen", "Shoujo", "Shounen"
)

val ALL_MANGA_FORMATS = listOf("MANGA", "NOVEL", "ONE_SHOT")
val ALL_MANGA_STATUSES = listOf("RELEASING", "FINISHED", "NOT_YET_RELEASED", "CANCELLED", "HIATUS")
val ALL_SORTS = listOf(
    "SEARCH_MATCH" to "Relevance",
    "POPULARITY_DESC" to "Popularity",
    "SCORE_DESC" to "Score",
    "TRENDING_DESC" to "Trending",
    "UPDATED_AT_DESC" to "Recently Updated",
    "START_DATE_DESC" to "Release Date",
    "FAVOURITES_DESC" to "Favorites",
    "CHAPTERS_DESC" to "Chapter Count",
    "TITLE_ROMAJI" to "Title"
)

data class SearchFilters(
    val query: String = "",
    val genres: Set<String> = emptySet(),
    val format: String? = null,
    val status: String? = null,
    val sort: String = "SEARCH_MATCH"
)

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onMangaSelected: (AniListSearchResult) -> Unit,
    onDismiss: () -> Unit = {},
    isActive: Boolean = false
) {
    var filters by remember { mutableStateOf(SearchFilters()) }
    var results by remember { mutableStateOf<List<AniListSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showFormatDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var showGenreSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun performSearch(page: Int = 1) {
        if (page == 1) {
            isSearching = true
            hasSearched = true
        } else {
            isLoadingMore = true
        }
        viewModel.searchMangaAdvanced(
            query = filters.query.ifBlank { null },
            genres = filters.genres.toList().ifEmpty { null },
            format = filters.format,
            status = filters.status,
            sort = filters.sort,
            page = page,
            perPage = 30,
            onResult = { newResults ->
                if (page == 1) {
                    results = newResults
                    hasMore = newResults.size >= 30
                } else {
                    results = results + newResults
                    hasMore = newResults.size >= 30
                }
                currentPage = page
                isSearching = false
                isLoadingMore = false
            },
            onError = { msg ->
                isSearching = false
                isLoadingMore = false
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    var autoSearchSkippedInitial by remember { mutableStateOf(false) }
    LaunchedEffect(filters.genres, filters.format, filters.status, filters.sort) {
        if (autoSearchSkippedInitial) {
            performSearch()
        } else {
            autoSearchSkippedInitial = true
        }
    }

    LaunchedEffect(filters.query) {
        if (filters.query.isNotBlank()) {
            delay(400.milliseconds)
            performSearch()
        }
    }

    val activeFilterCount = listOfNotNull(
        filters.genres.takeIf { it.isNotEmpty() }?.let { 1 },
        filters.format,
        filters.status,
        filters.sort.takeIf { it != "SEARCH_MATCH" }?.let { 1 }
    ).size

    fun clearAllFilters() {
        filters = SearchFilters(query = filters.query)
        results = emptyList()
        hasSearched = false
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            delay(200.milliseconds)
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("Search", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.titleLarge)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = filters.query,
                        onValueChange = { filters = filters.copy(query = it) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide(); performSearch() }),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (filters.query.isEmpty()) Text("Search manga...", color = Color.White.copy(alpha = 0.35f), fontSize = 16.sp)
                                innerTextField()
                            }
                        }
                    )
                    IconButton(
                        onClick = { filters = filters.copy(query = ""); results = emptyList(); hasSearched = false },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = if (filters.query.isNotEmpty()) 0.5f else 0f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFilters = !showFilters }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Filters", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.titleSmall)
                    if (activeFilterCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary) {
                            Text("$activeFilterCount", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (showFilters) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = "Toggle filters",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Row {
                    if (activeFilterCount > 0) {
                        TextButton(onClick = { clearAllFilters() }, modifier = Modifier.height(32.dp)) {
                            Text("Reset", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(
                        onClick = { keyboardController?.hide(); performSearch() },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        enabled = filters.query.isNotBlank() || activeFilterCount > 0
                    ) {
                        Text("Search", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            AnimatedVisibility(visible = showFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    FilterRow(
                        label = "Genres",
                        count = filters.genres.size,
                        onClick = { showGenreSheet = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DropdownFilter("Format", filters.format, ALL_MANGA_FORMATS, showFormatDropdown,
                            { showFormatDropdown = !showFormatDropdown; showStatusDropdown = false; showSortDropdown = false },
                            { showFormatDropdown = false },
                            { filters = filters.copy(format = it) })
                        DropdownFilter("Status", filters.status, ALL_MANGA_STATUSES, showStatusDropdown,
                            { showStatusDropdown = !showStatusDropdown; showFormatDropdown = false; showSortDropdown = false },
                            { showStatusDropdown = false },
                            { filters = filters.copy(status = it) })
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        DropdownFilter("Sort",
                            ALL_SORTS.find { it.first == filters.sort }?.second ?: "Relevance",
                            ALL_SORTS.map { it.second },
                            showSortDropdown,
                            { showSortDropdown = !showSortDropdown; showFormatDropdown = false; showStatusDropdown = false },
                            { showSortDropdown = false }
                        ) { selectedLabel ->
                            val pair = ALL_SORTS.find { it.second == selectedLabel } ?: ALL_SORTS.first()
                            filters = filters.copy(sort = pair.first)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize().padding(top = 128.dp), contentAlignment = Alignment.TopCenter) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (hasSearched && results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 128.dp)) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No results found", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.titleMedium)
                        Text("Try adjusting your filters", color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (!hasSearched) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 128.dp)) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Discover Manga", color = Color.White.copy(alpha = 0.35f), style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Search by name or use filters above", color = Color.White.copy(alpha = 0.25f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results, key = { it.id }) { manga ->
                        SearchResultCard(
                            manga = manga,
                            onClick = {
                                keyboardController?.hide()
                                onMangaSelected(manga)
                            }
                        )
                    }
                    if (hasMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    TextButton(onClick = { performSearch(currentPage + 1) }) {
                                        Text("Load More", color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGenreSheet) {
        MultiSelectSheet(
            options = ALL_GENRES,
            selected = filters.genres,
            onToggle = { genre ->
                filters = if (genre in filters.genres) filters.copy(genres = filters.genres - genre)
                else filters.copy(genres = filters.genres + genre)
            },
            onDismiss = { showGenreSheet = false }
        )
    }
}

@Composable
private fun SearchResultCard(
    manga: AniListSearchResult,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val displayScore = manga.meanScore?.let { it / 10.0 }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(manga.coverUrl)
                        .memoryCacheKey(manga.coverUrl)
                        .diskCacheKey(manga.coverUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                )
                displayScore?.let { score ->
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            String.format(Locale.getDefault(), "%.1f", score),
                            color = Color(0xFFFFD700),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            val displayTitle = when {
                !manga.englishTitle.isNullOrBlank() && manga.englishTitle != "null" -> manga.englishTitle
                !manga.title.isNullOrBlank() && manga.title != "null" -> manga.title
                !manga.nativeTitle.isNullOrBlank() && manga.nativeTitle != "null" -> manga.nativeTitle
                else -> "Unknown"
            }
            Text(
                displayTitle,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                manga.format?.let { fmt ->
                    Text(fmt.replace("_", " "), color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    if (manga.chapters != null && manga.chapters > 0) Text(" • ", color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.labelSmall)
                }
                manga.chapters?.let { ch ->
                    if (ch > 0) Text("${ch}ch", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF2A2A2A),
        modifier = Modifier.fillMaxWidth().height(42.dp).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (count > 0) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary) {
                    Text("$count", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MultiSelectSheet(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(options, searchQuery) {
        if (searchQuery.isBlank()) options
        else options.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Genres", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                    }
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .height(36.dp)
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) Text("Search...", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                            innerTextField()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matches", color = Color.White.copy(alpha = 0.3f))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filtered.forEach { option ->
                                FilterChip(
                                    selected = option in selected,
                                    onClick = { onToggle(option) },
                                    label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF2A2A2A),
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        labelColor = Color.White.copy(alpha = 0.7f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        enabled = true,
                                        selected = option in selected
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DropdownFilter(
    label: String,
    currentValue: String?,
    options: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        Box {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2A2A2A),
                modifier = Modifier.fillMaxWidth().height(34.dp).clickable(onClick = onToggle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (currentValue ?: "Any").replace("_", " "),
                        color = if (currentValue != null) Color.White else Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                DropdownMenuItem(text = { Text("Any") }, onClick = { onSelect(null); onDismiss() })
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.replace("_", " ")) },
                        onClick = { onSelect(option); onDismiss() }
                    )
                }
            }
        }
    }
}
