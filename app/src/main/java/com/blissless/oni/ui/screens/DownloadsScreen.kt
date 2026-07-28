package com.blissless.oni.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.oni.data.ChapterInfo
import com.blissless.oni.data.DownloadStatus
import com.blissless.oni.data.DownloadedChapter
import com.blissless.oni.data.DownloadedManga
import com.blissless.oni.data.DownloadTask
import com.blissless.oni.ui.theme.StatusCompleted
import com.blissless.oni.viewmodel.DownloadedResumeEntry
import com.blissless.oni.viewmodel.MainViewModel

@Composable
fun DownloadsScreen(
    viewModel: MainViewModel,
    onReadOffline: (mangaTitle: String, chapterNumber: Double) -> Unit = { _, _ -> },
    onDiscardProgress: (mangaSlug: String) -> Unit = { _ -> },
    onFullscreenChanged: (Boolean) -> Unit = {}
) {
    val activeDownloads by viewModel.downloadManager.activeDownloads.collectAsState()
    val downloadedManga by viewModel.downloadedManga.collectAsState()
    val downloadedResumeReading by viewModel.downloadedResumeReading.collectAsState()
    var downloadDir by remember { mutableStateOf(viewModel.getDownloadDirectory()) }
    val context = LocalContext.current
    val defaultDir = remember { context.getExternalFilesDir(null)?.absolutePath + "/oni" }
    val isCustomDir = downloadDir != defaultDir

    LaunchedEffect(Unit) { viewModel.scanDownloadedManga() }

    var showClearDialog by remember { mutableStateOf(false) }
    var showRevertDirDialog by remember { mutableStateOf(false) }
    var mangaToDelete by remember { mutableStateOf<DownloadedManga?>(null) }
    var chapterToDelete by remember { mutableStateOf<Pair<String, DownloadedChapter>?>(null) }
    var mangaForChapterList by remember { mutableStateOf<DownloadedManga?>(null) }

    LaunchedEffect(mangaForChapterList != null) { onFullscreenChanged(mangaForChapterList != null) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { onFullscreenChanged(false) }
    }

    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val path = uri.path ?: return@rememberLauncherForActivityResult
            val resolved = when {
                path.startsWith("/tree/primary:") ->
                    "/storage/emulated/0/" + path.removePrefix("/tree/primary:")
                path.startsWith("/document/primary:") ->
                    "/storage/emulated/0/" + path.removePrefix("/document/primary:")
                else -> path
            }
            viewModel.setDownloadDirectory(resolved)
            downloadDir = resolved
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.moveDownloadsToDirectory(resolved) {
                viewModel.scanDownloadedManga()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Downloads",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            if (activeDownloads.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        Icons.Default.Delete, contentDescription = "Clear all",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Download Location", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(downloadDir, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (isCustomDir) {
                    IconButton(onClick = { showRevertDirDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Revert to default", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = { directoryPicker.launch(null) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Change directory", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val hasAnyContent = activeDownloads.isNotEmpty() || downloadedManga.isNotEmpty() || downloadedResumeReading.isNotEmpty()

        if (!hasAnyContent) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No downloads yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Download chapters from the manga detail screen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (downloadedResumeReading.isNotEmpty()) {
                    item(key = "resume_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Resume Reading",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${downloadedResumeReading.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    item(key = "resume_row") {
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(downloadedResumeReading.size) { index ->
                                val entry = downloadedResumeReading[index]
                                DownloadedResumeCard(
                                    entry = entry,
                                    onClick = {
                                        onReadOffline(entry.title, entry.currentChapter)
                                    },
                                    onDiscard = { onDiscardProgress(entry.slug) }
                                )
                            }
                        }
                    }
                }

                if (activeDownloads.isNotEmpty()) {
                    item(key = "active_header") {
                        Text("Active Downloads", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    val grouped = activeDownloads.groupBy { it.mangaTitle }
                    grouped.forEach { (mangaTitle, tasks) ->
                        item(key = "active_manga_$mangaTitle") {
                            Text(mangaTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                        }
                        items(tasks, key = { it.id }) { task ->
                            ActiveDownloadItem(task = task, onCancel = { viewModel.cancelDownload(task.id) })
                        }
                    }
                }

                if (downloadedManga.isNotEmpty()) {
                    item(key = "library_header") {
                        Spacer(modifier = Modifier.height(if (activeDownloads.isNotEmpty()) 12.dp else 0.dp))
                        Text("Library", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    items(downloadedManga, key = { it.slug }) { manga ->
                        DownloadedMangaCard(
                            manga = manga,
                            onShowChapters = { mangaForChapterList = manga },
                            onDeleteManga = { mangaToDelete = manga }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Clear all downloads?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all download records. Downloaded files will remain on disk.") },
            confirmButton = {
                Button(
                    onClick = {
                        activeDownloads.forEach { viewModel.removeDownload(it.id) }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    mangaToDelete?.let { manga ->
        AlertDialog(
            onDismissRequest = { mangaToDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete manga?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${manga.title}\" and all ${manga.chapters.size} downloaded chapter(s)? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteManga(manga.slug)
                        mangaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { mangaToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    chapterToDelete?.let { (slug, chapter) ->
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete chapter?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete Chapter ${chapter.chapterNumber} (${chapter.pageCount} pages)? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChapter(slug, chapter.chapterNumber)
                        chapterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { chapterToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showRevertDirDialog) {
        AlertDialog(
            onDismissRequest = { showRevertDirDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Revert to default?", fontWeight = FontWeight.Bold) },
            text = { Text("Move all downloads back to the default directory?\n\nFrom: $downloadDir\nTo: $defaultDir") },
            confirmButton = {
                Button(
                    onClick = {
                        showRevertDirDialog = false
                        viewModel.setDownloadDirectory(defaultDir)
                        downloadDir = defaultDir
                        viewModel.moveDownloadsToDirectory(defaultDir) {
                            viewModel.scanDownloadedManga()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Revert") }
            },
            dismissButton = {
                TextButton(onClick = { showRevertDirDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    mangaForChapterList?.let { manga ->
        var confirmDeleteChapter by remember { mutableStateOf<DownloadedChapter?>(null) }
        val sortedChapters = remember(manga) { manga.chapters.sortedBy { it.chapterNumber } }
        var searchQuery by remember { mutableStateOf("") }

        val filteredChapters = remember(sortedChapters, searchQuery) {
            if (searchQuery.isBlank()) sortedChapters
            else sortedChapters.filter { ch ->
                val chNumStr = if (ch.chapterNumber == ch.chapterNumber.toLong().toDouble()) {
                    "${ch.chapterNumber.toLong()}"
                } else "${ch.chapterNumber}"
                "chapter $chNumStr".contains(searchQuery, ignoreCase = true) ||
                    "ch. $chNumStr".contains(searchQuery, ignoreCase = true)
            }
        }

        val groupedChapters = remember(filteredChapters) {
            filteredChapters.mapIndexed { idx, ch ->
                val chLabel = if (ch.chapterNumber == ch.chapterNumber.toLong().toDouble()) {
                    "Chapter ${ch.chapterNumber.toLong()}"
                } else "Chapter ${ch.chapterNumber}"
                idx to ChapterInfo(url = ch.chapterDir.absolutePath, title = chLabel)
            }.chunked(20).map { items ->
                val startCh = filteredChapters[items.first().first]
                val endCh = filteredChapters[items.last().first]
                val fmt = { d: Double -> if (d == d.toLong().toDouble()) "${d.toLong()}" else "$d" }
                val groupKey = if (startCh.chapterNumber == endCh.chapterNumber) "Ch. ${fmt(startCh.chapterNumber)}" else "Ch. ${fmt(startCh.chapterNumber)} - ${fmt(endCh.chapterNumber)}"
                groupKey to items
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            androidx.activity.compose.BackHandler { mangaForChapterList = null }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { mangaForChapterList = null }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            manga.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${sortedChapters.size} chapter(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search chapters...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (groupedChapters.isEmpty() && searchQuery.isNotBlank()) {
                        item(key = "no_results") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No chapters match \"$searchQuery\"",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        groupedChapters.forEachIndexed { groupIdx, (groupKey, items) ->
                            item(key = "group_$groupIdx") {
                                ChapterGroup(
                                    groupKey = groupKey,
                                    groupChapters = items,
                                    selectedIndex = -1,
                                    readChapterIndices = emptySet(),
                                    nextChapterToRead = null,
                                    initiallyExpanded = false,
                                    onChapterClick = { absIdx ->
                                        mangaForChapterList = null
                                        onReadOffline(manga.title, filteredChapters[absIdx].chapterNumber)
                                    },
                                    onDeleteChapter = { chNum ->
                                        val ch = sortedChapters.find { it.chapterNumber == chNum }
                                        if (ch != null) confirmDeleteChapter = ch
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        confirmDeleteChapter?.let { ch ->
            AlertDialog(
                onDismissRequest = { confirmDeleteChapter = null },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
                title = { Text("Delete chapter?", fontWeight = FontWeight.Bold) },
                text = { Text("Delete Chapter ${ch.chapterNumber} (${ch.pageCount} pages)? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteChapter(manga.slug, ch.chapterNumber)
                            val updated = manga.copy(chapters = manga.chapters.filter { it.chapterNumber != ch.chapterNumber })
                            if (updated.chapters.isEmpty()) {
                                mangaForChapterList = null
                            } else {
                                mangaForChapterList = updated
                            }
                            confirmDeleteChapter = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteChapter = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
private fun ActiveDownloadItem(
    task: DownloadTask,
    onCancel: () -> Unit
) {
    val statusColor = when (task.status) {
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        DownloadStatus.COMPLETED -> StatusCompleted
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.PAUSED -> Color(0xFFFBBF24)
        DownloadStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val progress = if (task.totalPages > 0) task.downloadedPages.toFloat() / task.totalPages.toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.chapterTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        when (task.status) {
                            DownloadStatus.DOWNLOADING -> "Downloading... ${task.downloadedPages}/${task.totalPages}"
                            DownloadStatus.COMPLETED -> "Completed \u00b7 ${task.totalPages} pages"
                            DownloadStatus.FAILED -> task.errorMessage ?: "Failed"
                            DownloadStatus.PAUSED -> "Paused"
                            DownloadStatus.PENDING -> "Queued"
                        },
                        style = MaterialTheme.typography.bodySmall, color = statusColor
                    )
                }
                when (task.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.PENDING, DownloadStatus.PAUSED -> {
                        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                    else -> {}
                }
            }

            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant)) {
                    Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxSize().background(statusColor, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}

@Composable
private fun DownloadedMangaCard(
    manga: DownloadedManga,
    onShowChapters: () -> Unit,
    onDeleteManga: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowChapters() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (manga.coverPath != null) {
                AsyncImage(
                    model = manga.coverPath,
                    contentDescription = manga.title,
                    modifier = Modifier
                        .width(48.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(manga.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text("${manga.chapters.size} chapter(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDeleteManga, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete manga", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun DownloadedResumeCard(
    entry: DownloadedResumeEntry,
    onClick: () -> Unit,
    onDiscard: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(240.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (entry.coverPath != null) {
                AsyncImage(
                    model = entry.coverPath,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(entry.title.take(2).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick)
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
                            text = "Ch. ${entry.currentChapter.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .pointerInput(Unit) {
                                detectTapGestures { onDiscard() }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove progress",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Column {
                    val progress = entry.scrollProgress.coerceIn(0f, 1f)
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
                        text = entry.title,
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
