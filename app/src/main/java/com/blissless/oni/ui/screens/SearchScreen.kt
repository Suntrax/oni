package com.blissless.oni.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.blissless.oni.data.MangaSearchResult
import com.blissless.oni.viewmodel.MainViewModel
import com.blissless.oni.viewmodel.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.blissless.oni.ui.theme.BlueAccent
import com.blissless.oni.ui.theme.BlueLight
import com.blissless.oni.ui.theme.DarkBackground
import com.blissless.oni.ui.theme.DarkCard
import com.blissless.oni.ui.theme.DarkSurface
import com.blissless.oni.ui.theme.DarkSurfaceVariant
import com.blissless.oni.ui.theme.GlassStroke
import com.blissless.oni.ui.theme.SilverDark
import com.blissless.oni.ui.theme.SilverLight

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onMangaSelected: (MangaSearchResult) -> Unit,
    isActive: Boolean = false
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(isActive) {
        if (isActive) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        if (searchQuery.length >= 2) {
            delay(400)
            if (searchQuery == viewModel.searchQuery.value && searchQuery.length >= 2) {
                android.util.Log.d("SEARCH", "LaunchedEffect: triggering search for '$searchQuery'")
                viewModel.search()
            }
        } else if (searchQuery.isEmpty()) {
            viewModel.clearSearch()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .border(
                        if (searchQuery.isNotEmpty()) 1.dp else 0.5.dp,
                        if (searchQuery.isNotEmpty()) GlassStroke.copy(alpha = 0.5f) else GlassStroke,
                        RoundedCornerShape(14.dp)
                    )
                    .focusRequester(focusRequester)
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateQuery(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.search()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search manga...",
                                        color = SilverDark,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.clearSearch()
                                keyboardController?.hide()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SilverDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        when (val state = searchResults) {
            is UiState.Success -> {
                if (state.data.isNotEmpty()) {
                    items(state.data) { manga ->
                        SearchMangaCard(
                            manga = manga,
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onMangaSelected(manga)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BlueAccent,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            color = Color.Red
                        )
                    }
                }
            }
            else -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start typing to search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchMangaCard(
    manga: MangaSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, GlassStroke)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (manga.coverUrl != null) {
                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = manga.title,
                    modifier = Modifier
                        .size(56.dp, 78.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp, 78.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = manga.title.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = BlueAccent.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BlueAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tap to read",
                        style = MaterialTheme.typography.labelSmall,
                        color = BlueLight,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}
