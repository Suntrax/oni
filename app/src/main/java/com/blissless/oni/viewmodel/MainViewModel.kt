package com.blissless.oni.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.Html
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blissless.oni.data.AniListManager
import com.blissless.oni.data.ChapterInfo
import com.blissless.oni.data.ChapterImages
import com.blissless.oni.data.HomeSection
import com.blissless.oni.data.MangaDetail
import com.blissless.oni.data.MangaRepository
import com.blissless.oni.data.AniListSearchResult
import com.blissless.oni.data.MangaSearchResult
import com.blissless.oni.data.MangaTrack
import com.blissless.oni.data.ReadingStatus
import com.blissless.oni.data.SettingsManager
import com.blissless.oni.data.TrackingManager
import com.blissless.oni.update.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

data class InstalledExtension(val label: String, val packageName: String) {
    val authority: String get() = "$packageName.provider"
}

class MainViewModel(private val context: Context) : ViewModel() {

    private val repository = MangaRepository(context)
    private val trackingManager = TrackingManager(context)
    private val anilistManager = AniListManager(context)
    private val settingsManager = SettingsManager(context)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<UiState<List<AniListSearchResult>>>(UiState.Idle)
    val searchResults: StateFlow<UiState<List<AniListSearchResult>>> = _searchResults.asStateFlow()

    private val _homeSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val homeSections: StateFlow<List<HomeSection>> = _homeSections.asStateFlow()

    private val _continueReading = MutableStateFlow<List<MangaTrack>>(emptyList())
    val continueReading: StateFlow<List<MangaTrack>> = _continueReading.asStateFlow()

    private val _resumeReading = MutableStateFlow<List<MangaTrack>>(emptyList())
    val resumeReading: StateFlow<List<MangaTrack>> = _resumeReading.asStateFlow()

    private val _planningToRead = MutableStateFlow<List<MangaTrack>>(emptyList())
    val planningToRead: StateFlow<List<MangaTrack>> = _planningToRead.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chapters: StateFlow<List<ChapterInfo>> = _chapters.asStateFlow()

    private val _mangaDetail = MutableStateFlow<MangaDetail?>(null)
    val mangaDetail: StateFlow<MangaDetail?> = _mangaDetail.asStateFlow()

    private val _selectedChapterIndex = MutableStateFlow(-1)
    val selectedChapterIndex: StateFlow<Int> = _selectedChapterIndex.asStateFlow()

    private val _resumeScrollProgress = MutableStateFlow(-1f)
    val resumeScrollProgress: StateFlow<Float> = _resumeScrollProgress.asStateFlow()

    private val _chapterImages = MutableStateFlow<UiState<ChapterImages>>(UiState.Idle)
    val chapterImages: StateFlow<UiState<ChapterImages>> = _chapterImages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isChapterRead = MutableStateFlow(false)
    val isChapterRead: StateFlow<Boolean> = _isChapterRead.asStateFlow()

    private val _readChapterIndices = MutableStateFlow<Set<Int>>(emptySet())
    val readChapterIndices: StateFlow<Set<Int>> = _readChapterIndices.asStateFlow()

    private val _nextChapterToRead = MutableStateFlow<Int?>(null)
    val nextChapterToRead: StateFlow<Int?> = _nextChapterToRead.asStateFlow()

    private var lastSearchedQuery: String = ""
    private var searchJob: kotlinx.coroutines.Job? = null
    
    private var currentMangaId: String? = null
    private var currentMangaTitle: String? = null
    private var currentMangaCoverUrl: String? = null
    private var currentMangaUrl: String? = null

    // AniList state
    private val _anilistUsername = MutableStateFlow<String?>(null)
    val anilistUsername: StateFlow<String?> = _anilistUsername.asStateFlow()

    private val _isAniListSyncing = MutableStateFlow(false)
    val isAniListSyncing: StateFlow<Boolean> = _isAniListSyncing.asStateFlow()

    private val _anilistSyncThreshold = MutableStateFlow(settingsManager.getAniListSyncThreshold())
    val anilistSyncThreshold: StateFlow<Int> = _anilistSyncThreshold.asStateFlow()

    private val _showMergeDialog = MutableStateFlow(false)
    val showMergeDialog: StateFlow<Boolean> = _showMergeDialog.asStateFlow()

    private var pendingAnilistUpdate: Job? = null
    private var hasSyncedOnStart = false

    private val _pendingUpdateRelease = MutableStateFlow<GitHubRelease?>(null)
    val pendingUpdateRelease: StateFlow<GitHubRelease?> = _pendingUpdateRelease.asStateFlow()

    private val _checkUpdatesOnStart = MutableStateFlow(settingsManager.getCheckUpdatesOnStart())
    val checkUpdatesOnStart: StateFlow<Boolean> = _checkUpdatesOnStart.asStateFlow()

    private val _installedExtensions = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val installedExtensions: StateFlow<List<InstalledExtension>> = _installedExtensions.asStateFlow()

    fun discoverExtensions() {
        val beaconIntent = Intent("com.blissless.mangaclient.EXTENSION_BEACON")
        val resolveInfoList = context.packageManager.queryBroadcastReceivers(beaconIntent, 0)
        val extensions = resolveInfoList
            .filter { info ->
                info.loadLabel(context.packageManager).toString()
                    .startsWith("Oni: ", ignoreCase = true)
            }
            .map { info ->
                InstalledExtension(
                    label = info.loadLabel(context.packageManager).toString(),
                    packageName = info.activityInfo.packageName
                )
            }
        _installedExtensions.value = extensions
    }

    private val _selectedExtensionAuthority = MutableStateFlow(settingsManager.getSelectedExtensionAuthority())
    val selectedExtensionAuthority: StateFlow<String?> = _selectedExtensionAuthority.asStateFlow()

    fun selectExtension(authority: String?) {
        settingsManager.setSelectedExtensionAuthority(authority)
        _selectedExtensionAuthority.value = authority
    }

    init {
        if (settingsManager.getCheckUpdatesOnStart()) {
            checkForUpdatesSilently()
        }
    }

    fun setCheckUpdatesOnStart(enabled: Boolean) {
        settingsManager.setCheckUpdatesOnStart(enabled)
        _checkUpdatesOnStart.value = enabled
    }

    fun checkForUpdatesSilently() {
        viewModelScope.launch {
            try {
                val url = "https://api.github.com/repos/Suntrax/oni/releases/latest"
                val request = Request.Builder().url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                val response = withContext(Dispatchers.IO) {
                    OkHttpClient().newCall(request).execute()
                }
                if (!response.isSuccessful) return@launch
                val body = withContext(Dispatchers.IO) { response.body!!.string() }
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val release = json.decodeFromString<GitHubRelease>(body)
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                val cleanTag = release.tagName.removePrefix("v").removePrefix("V")
                val parts1 = cleanTag.split(".").map { it.toIntOrNull() ?: 0 }
                val parts2 = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
                val maxLen = maxOf(parts1.size, parts2.size)
                var cmp = 0
                for (i in 0 until maxLen) {
                    val p1 = parts1.getOrElse(i) { 0 }
                    val p2 = parts2.getOrElse(i) { 0 }
                    if (p1 != p2) { cmp = p1 - p2; break }
                }
                if (cmp > 0) {
                    _pendingUpdateRelease.value = release
                }
            } catch (_: Exception) { }
        }
    }

    fun getCurrentMangaCoverUrl(): String? = currentMangaCoverUrl
    fun getCurrentMangaUrl(): String? = currentMangaUrl

    fun resolveMangaTracking(mangaId: String): MangaTrack? {
        var track = trackingManager.getMangaTracking(mangaId)
        if (track == null && currentMangaUrl != null) {
            track = trackingManager.getAllTracking().find { it.mangaUrl == currentMangaUrl }
        }
        return track
    }

    private fun log(tag: String, msg: String) {
        Log.d("ViewModel", "[$tag] $msg")
    }

    private fun countWholeChapters(chapters: List<ChapterInfo>): Int {
        var count = 0
        chapters.forEach { chapter ->
            val title = chapter.title ?: ""
            val numStr = extractChapterNumberString(title)
            if (numStr.contains('.') || numStr.contains(',')) return@forEach
            val mainChapter = extractMainChapterNumber(title)
            if (mainChapter > 0) count++
        }
        log("COUNT", "Whole chapters: $count / ${chapters.size}")
        return count
    }

    private fun extractMainChapterNumber(title: String): Int {
        val patterns = listOf(
            Regex("Chapter\\s*(\\d+)"),
            Regex("Ch\\s*\\.\\s*(\\d+)"),
            Regex("^(\\d+)"),
            Regex("(\\d+)(?:\\.\\d+)?")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(title)
            if (match != null) {
                val numStr = match.groupValues[1]
                return numStr.toIntOrNull() ?: 0
            }
        }
        return 0
    }

    private fun extractChapterNumberString(title: String): String {
        val patterns = listOf(
            Regex("Chapter\\s*([\\d.]+)"),
            Regex("Ch\\s*\\.\\s*([\\d.]+)"),
            Regex("^([\\d.]+)"),
            Regex("([\\d.]+)")
        )
        for (pattern in patterns) {
            val match = pattern.find(title)
            if (match != null) {
                val numStr = match.groupValues[1].trimEnd('.')
                if (numStr.isNotBlank() && numStr.any { it.isDigit() }) return numStr
            }
        }
        return title
    }

    fun loadHomePage() {
        viewModelScope.launch {
            _isLoading.value = true
            refreshTrackingLists()
            // Sync from AniList on app start (only once)
            if (!hasSyncedOnStart && anilistManager.isLoggedIn()) {
                hasSyncedOnStart = true
                syncAnilistManga()
            }
            val result = repository.getHomePage()
            result.fold(
                onSuccess = { sections ->
                    _homeSections.value = sections
                    log("HOME", "Loaded ${sections.size} sections")
                },
                onFailure = {
                    log("ERROR", "Failed to load home: ${it.message}")
                }
            )
            _isLoading.value = false
        }
    }
    
    fun refreshTrackingLists() {
        val allReading = trackingManager.getContinueReading()
        _resumeReading.value = allReading.filter { it.scrollProgress > 0f }
        _continueReading.value = allReading
        _planningToRead.value = trackingManager.getPlanningToRead()
    }
    
    fun addToPlanning(mangaId: String, title: String, coverUrl: String?, mangaUrl: String, totalChapters: Int) {
        trackingManager.markAsPlanning(mangaId, title, coverUrl, mangaUrl, totalChapters)
        refreshTrackingLists()
    }
    
    fun removeFromPlanning(mangaId: String) {
        trackingManager.removeTracking(mangaId)
        refreshTrackingLists()
    }

    fun removeFromReading(mangaId: String) {
        trackingManager.removeTracking(mangaId)
        refreshTrackingLists()
    }

    fun addToReading(mangaId: String, title: String, coverUrl: String?, mangaUrl: String, totalChapters: Int, resetProgress: Boolean = false) {
        trackingManager.markAsReading(mangaId, title, coverUrl, mangaUrl, totalChapters, resetProgress)
        refreshTrackingLists()
    }
    
    fun togglePlanning(mangaId: String, title: String, coverUrl: String?, mangaUrl: String, totalChapters: Int) {
        if (isInPlanning(mangaId)) {
            removeFromPlanning(mangaId)
        } else {
            addToPlanning(mangaId, title, coverUrl, mangaUrl, totalChapters)
        }
    }
    
    fun isInPlanning(mangaId: String): Boolean {
        return trackingManager.getMangaTracking(mangaId)?.status == com.blissless.oni.data.ReadingStatus.PLANNING
    }

    fun getMangaTracking(mangaId: String): com.blissless.oni.data.MangaTrack? {
        return trackingManager.getMangaTracking(mangaId)
    }

    fun updateTrackingStatus(mangaId: String, status: com.blissless.oni.data.ReadingStatus) {
        val existing = resolveMangaTracking(mangaId)
        val effectiveId: String
        if (existing != null) {
            trackingManager.updateTrackingStatus(existing.mangaId, status)
            effectiveId = existing.mangaId
        } else {
            val track = MangaTrack(
                mangaId = mangaId,
                title = currentMangaTitle ?: (_mangaDetail.value?.title ?: ""),
                coverUrl = currentMangaCoverUrl,
                currentChapterIndex = 0,
                currentChapterNumber = 0,
                currentChapterUrl = "",
                totalChapters = _mangaDetail.value?.totalChapterCount ?: 0,
                status = status,
                lastReadTimestamp = System.currentTimeMillis(),
                mangaUrl = currentMangaUrl ?: "https://atsu.moe/manga/$mangaId"
            )
            trackingManager.updateTracking(track)
            effectiveId = mangaId
        }
        refreshTrackingLists()
        val tracking = trackingManager.getMangaTracking(effectiveId)
        if (tracking != null) {
            updateAnilistProgressNow(tracking)
        }
    }



    fun continueFromTracking(track: com.blissless.oni.data.MangaTrack, onReady: () -> Unit) {
        val mangaId = extractUniqueMangaId(track.mangaId, track.mangaUrl)
        currentMangaId = mangaId
        currentMangaTitle = track.title
        currentMangaCoverUrl = track.coverUrl
        currentMangaUrl = track.mangaUrl
        _selectedChapterIndex.value = -1
        _isLoading.value = true
        _chapterImages.value = UiState.Idle
        
        viewModelScope.launch {
            val detailResult = repository.getMangaDetails(track.mangaUrl)
            detailResult.fold(
                onSuccess = { mangaDetail ->
                    currentMangaCoverUrl = track.coverUrl ?: mangaDetail.coverUrl ?: currentMangaCoverUrl
                    val detailWithCover = mangaDetail.copy(
                        coverUrl = currentMangaCoverUrl
                    )
                    _mangaDetail.value = detailWithCover
                    log("DETAIL", "Loaded detail with cover: ${detailWithCover.coverUrl}, track cover: ${track.coverUrl}")
                },
                onFailure = {
                    log("ERROR", "Failed to load detail: ${it.message}")
                }
            )
            
            val result = repository.getChapters(track.mangaUrl)
            result.fold(
                onSuccess = { chapterData ->
                    val chapterList = chapterData.chapters
                    _chapters.value = chapterList
                    val totalChapters = countWholeChapters(chapterList)
                    log("CHAPTERS", "Loaded ${chapterList.size} chapters for continue reading, total: $totalChapters")
                    trackingManager.updateTotalChapters(mangaId, totalChapters) 
                    refreshTrackingLists()
                    
                    // Find position by chapter URL or number match
                    val savedUrl = track.currentChapterUrl
                    var currentPosition = -1
                    
                    // Try URL match
                    if (savedUrl.isNotBlank()) {
                        currentPosition = chapterList.indexOfFirst { it.url == savedUrl }
                        Log.d("CONTINUE", "URL match attempt: '$savedUrl', found index: $currentPosition")
                    }
                    
                    // If no URL match, try by saved chapter number
                    if (currentPosition < 0 && track.currentChapterNumber > 0) {
                        currentPosition = chapterList.indexOfFirst { ch -> 
                            ch.title?.contains(track.currentChapterNumber.toString()) == true
                        }
                        Log.d("CONTINUE", "Number match: ${track.currentChapterNumber}, found index: $currentPosition")
                    }
                    
                    // Last resort: use saved index but validate it
                    if (currentPosition < 0) {
                        currentPosition = track.currentChapterIndex.coerceIn(0, chapterList.lastIndex)
                        Log.d("CONTINUE", "Using fallback index: $currentPosition")
                    }
                    
                    val safeChapterIndex = currentPosition.coerceIn(0, chapterList.lastIndex)
                    val nextToRead = if (track.currentChapterNumber > 0) safeChapterIndex + 1 else 0
                    
                    _readChapterIndices.value = (0 until safeChapterIndex).toSet()
                    _nextChapterToRead.value = nextToRead + 1
                    Log.d("CONTINUE", "Final: current=$currentPosition safe=$safeChapterIndex next=$nextToRead, chapterNumber=${track.currentChapterNumber}")
                    
                    // Load the next chapter
                    val chapter = chapterList.getOrNull(nextToRead)
                    if (chapter != null) {
                        _selectedChapterIndex.value = nextToRead
                        _isChapterRead.value = safeChapterIndex > 0
                        _chapterImages.value = UiState.Loading
                        loadChapterImages(chapter.url)
                        log("READY", "Loading next chapter: ${chapter.title}")
                        onReady()
                    } else {
                        // At last chapter, load current
                        _selectedChapterIndex.value = safeChapterIndex
                        _chapterImages.value = UiState.Loading
                        loadChapterImages(chapterList[safeChapterIndex].url)
                    }
                },
                onFailure = {
                    log("ERROR", "Failed to load chapters: ${it.message}")
                    _isLoading.value = false
                }
            )
        }
    }

    fun resumeFromTracking(track: com.blissless.oni.data.MangaTrack, onReady: () -> Unit) {
        val mangaId = extractUniqueMangaId(track.mangaId, track.mangaUrl)
        currentMangaId = mangaId
        currentMangaTitle = track.title
        currentMangaCoverUrl = track.coverUrl
        currentMangaUrl = track.mangaUrl
        _selectedChapterIndex.value = -1
        _isLoading.value = true
        _chapterImages.value = UiState.Idle
        _resumeScrollProgress.value = track.scrollProgress

        viewModelScope.launch {
            val detailResult = repository.getMangaDetails(track.mangaUrl)
            detailResult.fold(
                onSuccess = { mangaDetail ->
                    currentMangaCoverUrl = track.coverUrl ?: mangaDetail.coverUrl ?: currentMangaCoverUrl
                    _mangaDetail.value = mangaDetail.copy(coverUrl = currentMangaCoverUrl)
                },
                onFailure = {
                    log("ERROR", "Failed to load detail: ${it.message}")
                }
            )

            val result = repository.getChapters(track.mangaUrl)
            result.fold(
                onSuccess = { chapterData ->
                    val chapterList = chapterData.chapters
                    _chapters.value = chapterList
                    val totalChapters = countWholeChapters(chapterList)
                    trackingManager.updateTotalChapters(mangaId, totalChapters)

                    // The chapter to resume is the one after the last completed
                    val safeIndex = (track.currentChapterIndex + 1).coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
                    val chapter = chapterList.getOrNull(safeIndex)
                    if (chapter != null) {
                        _selectedChapterIndex.value = safeIndex
                        _isChapterRead.value = false
                        _isLoading.value = true
                        loadChapterImages(chapter.url)
                        onReady()
                    }
                },
                onFailure = {
                    log("ERROR", "Failed to load chapters: ${it.message}")
                    _isLoading.value = false
                }
            )
        }
    }

    fun continueFromCurrentManga(onReady: () -> Unit) {
        val mangaDetail = _mangaDetail.value ?: return
        val mangaUrl = currentMangaUrl ?: return
        val mangaId = currentMangaId ?: return

        _selectedChapterIndex.value = -1
        _isLoading.value = true
        _chapterImages.value = UiState.Idle
        
        val tracking = trackingManager.getMangaTracking(mangaId)
        val savedIndex = if (tracking?.status == com.blissless.oni.data.ReadingStatus.READING) {
            tracking.currentChapterIndex
        } else {
            0
        }
        val nextChapterIndex = if (savedIndex > 0) savedIndex + 1 else 0

        val chapterList = _chapters.value
        if (chapterList.isNotEmpty()) {
            val safeChapterIndex = nextChapterIndex.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            _readChapterIndices.value = (0 until safeChapterIndex).toSet()
            _nextChapterToRead.value = safeChapterIndex
            
            val chapter = chapterList.getOrNull(safeChapterIndex)
            if (chapter != null) {
                _selectedChapterIndex.value = safeChapterIndex
                _isChapterRead.value = safeChapterIndex > 0
                loadChapterImages(chapter.url)
                onReady()
            }
            _isLoading.value = false
            return
        }
        
        viewModelScope.launch {
            val result = repository.getChapters(mangaUrl)
            result.fold(
                onSuccess = { chapterData ->
                    val chapterList = chapterData.chapters
                    _chapters.value = chapterList
                    
                    val safeChapterIndex = nextChapterIndex.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
                    
                    _readChapterIndices.value = (0 until safeChapterIndex).toSet()
                    _nextChapterToRead.value = safeChapterIndex
                    
                    val chapter = chapterList.getOrNull(safeChapterIndex)
                    if (chapter != null) {
                        _selectedChapterIndex.value = safeChapterIndex
                        _isChapterRead.value = safeChapterIndex > 0
                        loadChapterImages(chapter.url)
                        onReady()
                    }
                },
                onFailure = {
                    log("ERROR", "Failed to load chapters: ${it.message}")
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun onChapterScrollProgress(scrollPercent: Float) {
        val threshold = _anilistSyncThreshold.value / 100f
        if (_selectedChapterIndex.value < 0) return
        
        currentMangaId?.let { mangaId ->
            val chapter = _chapters.value.getOrNull(_selectedChapterIndex.value)
            if (chapter != null) {
                val chapterNumber = extractMainChapterNumber(chapter.title ?: "")
                val existing = trackingManager.getMangaTracking(mangaId)
                
                // Create tracking entry if none exists (any scroll level)
                if (existing == null) {
                    val track = MangaTrack(
                        mangaId = mangaId,
                        title = currentMangaTitle ?: "",
                        coverUrl = currentMangaCoverUrl,
                        currentChapterIndex = _selectedChapterIndex.value,
                        currentChapterNumber = chapterNumber,
                        currentChapterUrl = chapter.url,
                        totalChapters = countWholeChapters(_chapters.value),
                        status = ReadingStatus.READING,
                        lastReadTimestamp = System.currentTimeMillis(),
                        mangaUrl = currentMangaUrl ?: "https://atsu.moe/manga/$mangaId",
                        scrollProgress = scrollPercent
                    )
                    trackingManager.updateTracking(track)
                    log("TRACK", "Created tracking for chapter ${_selectedChapterIndex.value} at $scrollPercent")
                } else {
                    // Always save scroll progress (no tracking advancement until threshold)
                    trackingManager.updateScrollProgress(mangaId, scrollPercent)
                }
                
                refreshTrackingLists()
                
                // Mark chapter as read when threshold is passed
                if (scrollPercent >= threshold) {
                    val trackForUpdate = trackingManager.getMangaTracking(mangaId)
                    if (trackForUpdate == null || trackForUpdate.currentChapterIndex != _selectedChapterIndex.value || trackForUpdate.currentChapterNumber == 0) {
                        trackingManager.updateChapterProgress(mangaId, _selectedChapterIndex.value, chapterNumber, chapter.url)
                        log("TRACK", "Updated to chapter ${_selectedChapterIndex.value}")
                    }
                    _isChapterRead.value = true
                    _readChapterIndices.value = _readChapterIndices.value + _selectedChapterIndex.value
                    _nextChapterToRead.value = _selectedChapterIndex.value + 1
                    
                    // Debounced AniList update
                    val tracking = trackingManager.getMangaTracking(mangaId)
                    if (tracking != null) {
                        scheduleAnilistProgressUpdate(tracking)
                    }
                }
            }
        }
    }
    
    fun updateCurrentChapter() {
        currentMangaId?.let { mangaId ->
            val chapter = _chapters.value.getOrNull(_selectedChapterIndex.value)
            if (chapter != null) {
                val existing = trackingManager.getMangaTracking(mangaId)
                val chapterNumber = extractMainChapterNumber(chapter.title ?: "")
                if (existing == null) {
                    val track = MangaTrack(
                        mangaId = mangaId,
                        title = currentMangaTitle ?: "",
                        coverUrl = currentMangaCoverUrl,
                        currentChapterIndex = _selectedChapterIndex.value,
                        currentChapterNumber = chapterNumber,
                        currentChapterUrl = chapter.url,
                        totalChapters = countWholeChapters(_chapters.value),
                        status = ReadingStatus.READING,
                        lastReadTimestamp = System.currentTimeMillis(),
                        mangaUrl = currentMangaUrl ?: "https://atsu.moe/manga/$mangaId"
                    )
                    trackingManager.updateTracking(track)
                } else if (existing.currentChapterIndex != _selectedChapterIndex.value || existing.currentChapterNumber == 0) {
                    trackingManager.updateChapterProgress(mangaId, _selectedChapterIndex.value, chapterNumber, chapter.url)
                }
                refreshTrackingLists()
            }
        }
    }

    private fun scheduleAnilistProgressUpdate(track: MangaTrack) {
        pendingAnilistUpdate?.cancel()
        pendingAnilistUpdate = viewModelScope.launch {
            delay(3000)
            updateAnilistProgressNow(track)
        }
    }

    private fun updateAnilistProgressNow(track: MangaTrack) {
        val mediaId = track.anilistMediaId ?: extractAnilistMediaId(track.mangaId) ?: return
        if (!anilistManager.isLoggedIn()) return
        val anilistStatus = when (track.status) {
            ReadingStatus.READING -> "CURRENT"
            ReadingStatus.PLANNING -> "PLANNING"
            ReadingStatus.COMPLETED -> "COMPLETED"
            ReadingStatus.ON_HOLD -> "PAUSED"
            ReadingStatus.DROPPED -> "DROPPED"
        }
        viewModelScope.launch {
            val result = anilistManager.updateMediaListEntry(mediaId, track.currentChapterNumber, anilistStatus)
            result.fold(
                onSuccess = {
                    Log.d("ANILIST", "Updated progress for media $mediaId to chapter ${track.currentChapterNumber}")
                },
                onFailure = { e ->
                    Log.e("ANILIST", "Failed to update progress: ${e.message}")
                }
            )
        }
    }

    private fun extractAnilistMediaId(mangaId: String): Int? {
        val prefix = "anilist_"
        return if (mangaId.startsWith(prefix)) {
            mangaId.substringAfter(prefix).toIntOrNull()
        } else null
    }

    fun updateQuery(query: String) {
        _searchQuery.value = query
        if (query != lastSearchedQuery) {
            lastSearchedQuery = ""
        }
    }

    fun search() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) {
            Log.d("SEARCH", "Empty query, skipping")
            return
        }

        Log.d("SEARCH", "search() called with: '$query', last searched: '$lastSearchedQuery'")
        
        if (query == lastSearchedQuery) {
            Log.d("SEARCH", "Same query as last, checking state...")
            if (_searchResults.value is UiState.Success) {
                Log.d("SEARCH", "Already have results for '$query', skipping")
                return
            }
        }

        Log.d("SEARCH", "Executing AniList search for: '$query'")
        lastSearchedQuery = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchResults.value = UiState.Loading
            Log.d("SEARCH", "Loading state set, about to call AniList")
            val result = anilistManager.searchManga(query)
            if (_searchQuery.value != query) {
                Log.d("SEARCH", "Query changed during search ('$query' -> '${_searchQuery.value}'), ignoring results")
                return@launch
            }
            _searchResults.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Search failed") }
            )
            Log.d("SEARCH", "Results: ${(_searchResults.value as? UiState.Success)?.data?.size ?: 0} items")
        }
    }

    fun searchMangaAdvanced(
        query: String?,
        genres: List<String>?,
        format: String?,
        status: String?,
        sort: String?,
        page: Int,
        perPage: Int,
        onResult: (List<AniListSearchResult>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = anilistManager.searchMangaAdvanced(
                search = query,
                genres = genres,
                format = format,
                status = status,
                sort = sort,
                page = page,
                perPage = perPage
            )
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { onError(it.message ?: "Search failed") }
            )
        }
    }

    fun selectManga(manga: AniListSearchResult) {
        log("SELECT", "Selected from AniList: ${manga.title} (id=${manga.id})")
        val mangaId = "anilist_${manga.id}"
        currentMangaId = mangaId
        currentMangaTitle = manga.title
        currentMangaCoverUrl = manga.coverUrl
        currentMangaUrl = null
        _mangaDetail.value = null
        _isLoading.value = true

        viewModelScope.launch {
            val titleToTry = listOfNotNull(
                manga.title,
                manga.englishTitle,
                manga.nativeTitle
            ).firstOrNull { it.isNotBlank() } ?: manga.title

            val matchResult = repository.search(titleToTry)
            val matched = matchResult.getOrNull()?.firstOrNull { match ->
                match.title.contains(manga.title, ignoreCase = true) ||
                (manga.englishTitle != null && match.title.contains(manga.englishTitle, ignoreCase = true))
            }

            if (matched != null) {
                log("SELECT", "Matched to source manga: ${matched.title}, url: ${matched.url}")
                currentMangaUrl = matched.url
                currentMangaCoverUrl = matched.coverUrl ?: manga.coverUrl
                val resolvedMangaId = matched.mangaId ?: extractUniqueMangaId(
                    matched.url.substringAfter("/manga/").substringBefore("?"),
                    matched.url
                )
                currentMangaId = resolvedMangaId
                loadMangaDetails(matched.url, currentMangaCoverUrl)
                loadReadChapters(resolvedMangaId)
            } else {
                log("WARN", "No atsu.moe match found for '${manga.title}', using AniList data")
                currentMangaId = mangaId
                currentMangaTitle = manga.title
                currentMangaCoverUrl = manga.coverUrl
                currentMangaUrl = "https://anilist.co/manga/${manga.id}"
                val description = manga.description?.let { stripHtml(it) } ?: ""
                _mangaDetail.value = com.blissless.oni.data.MangaDetail(
                    id = mangaId,
                    title = manga.title,
                    englishTitle = manga.englishTitle,
                    synopsis = description,
                    coverUrl = manga.coverUrl,
                    bannerUrl = null,
                    genres = manga.genres ?: emptyList(),
                    status = manga.status?.let { formatStatus(it) } ?: "",
                    type = manga.format ?: "",
                    avgRating = (manga.meanScore ?: 0).toDouble() / 10.0,
                    totalChapterCount = manga.chapters ?: 0,
                    otherNames = listOfNotNull(manga.nativeTitle, manga.englishTitle).filter { it != manga.title },
                    authors = emptyList()
                )
                _chapters.value = emptyList()
                _isLoading.value = false
                refreshTrackingLists()
            }
        }
    }

    fun selectManga(manga: MangaSearchResult) {
        log("SELECT", "Selected: ${manga.title}, url: ${manga.url}")
        val mangaId = manga.mangaId ?: extractUniqueMangaId(
            manga.url.substringAfter("/manga/").substringBefore("?"),
            manga.url
        )
        currentMangaId = mangaId
        currentMangaTitle = manga.title
        currentMangaCoverUrl = manga.coverUrl
        currentMangaUrl = manga.url
        loadMangaDetails(manga.url, manga.coverUrl)
        loadReadChapters(mangaId)
    }

    private fun extractUniqueMangaId(baseId: String, mangaUrl: String): String {
        val params = mangaUrl.substringAfter("?").split("&").filter { it.isNotBlank() }
        val versionParams = params.filter { it.startsWith("colored") || it.startsWith("uncolored") || it.startsWith("ver") }
        return if (versionParams.isNotEmpty()) {
            "$baseId?${versionParams.joinToString("&")}"
        } else {
            baseId
        }
    }

    private fun loadReadChapters(mangaId: String) {
        Log.d("LOAD", "loadReadChapters for: $mangaId")
        val tracking = trackingManager.getMangaTracking(mangaId)
        if (tracking != null) {
            Log.d("LOAD", "Found tracking: chapterIndex=${tracking.currentChapterIndex}")
            _readChapterIndices.value = (0..tracking.currentChapterIndex).toSet()
            _nextChapterToRead.value = tracking.currentChapterIndex + 1
        } else {
            Log.d("LOAD", "No tracking found - resetting")
            _readChapterIndices.value = emptySet()
            _nextChapterToRead.value = 0
        }
    }

    fun startReading() {
        _mangaDetail.value?.let { detail ->
            val baseMangaId = detail.id
            val mangaId = extractUniqueMangaId(baseMangaId, currentMangaUrl ?: "")
            currentMangaId = mangaId
            currentMangaTitle = detail.title
            currentMangaCoverUrl = detail.coverUrl
            val mangaUrl = currentMangaUrl ?: "https://atsu.moe/manga/$baseMangaId"
            
            // Check for saved progress - only if in READING status and valid
            val existingTracking = trackingManager.getMangaTracking(mangaId)
            val savedIndex = if (existingTracking?.status == com.blissless.oni.data.ReadingStatus.READING && 
                existingTracking.currentChapterIndex > 0) {
                existingTracking.currentChapterIndex
            } else {
                0
            }
            
            // Start new - always reset to beginning when pressing Start Reading
            _readChapterIndices.value = emptySet()
            _nextChapterToRead.value = 0
            _selectedChapterIndex.value = 0
            Log.d("START", "Starting fresh at chapter 0 (savedIndex was $savedIndex)")
            
            _isLoading.value = true  // Show loading screen
            
            addToReading(mangaId, detail.title, detail.coverUrl, mangaUrl, detail.totalChapterCount, resetProgress = true)
            loadChapters(mangaUrl)
        }
    }

    private fun loadMangaDetails(mangaUrl: String, searchCoverUrl: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _mangaDetail.value = null
            
            // Load both detail and chapters in parallel, use chapters to get correct total
            val detailDeferred = async { repository.getMangaDetails(mangaUrl) }
            val chaptersDeferred = async { repository.getChapters(mangaUrl) }
            
            detailDeferred.await().fold(
                onSuccess = { mangaDetail ->
                    val effectiveCover = searchCoverUrl ?: currentMangaCoverUrl ?: mangaDetail.coverUrl?.takeIf { it.isNotBlank() }
                    val detailWithCover = mangaDetail.copy(
                        coverUrl = effectiveCover
                    )
                    _mangaDetail.value = detailWithCover
                    log("DETAILS", "Loaded: ${mangaDetail.title}, raw chapters: ${mangaDetail.totalChapterCount}")
                },
                onFailure = {
                    log("ERROR", "Failed: ${it.message}")
                }
            )
            
            // Now update with real chapter count (excluding decimal sub-chapters)
            chaptersDeferred.await().fold(
                onSuccess = { chapterData ->
                    val total = countWholeChapters(chapterData.chapters)
                    _mangaDetail.value?.let { detail ->
                        _mangaDetail.value = detail.copy(totalChapterCount = total)
                    }
                    Log.d("DETAILS", "Updated chapter count to: $total")
                },
                onFailure = { }
            )
            
            _isLoading.value = false
        }
    }

    private fun loadChapters(mangaUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedChapterIndex.value = -1
            _chapterImages.value = UiState.Idle
            Log.d("CHAPTERS", "Loading chapters for: $mangaUrl")
            val result = repository.getChapters(mangaUrl)
            result.fold(
                onSuccess = { chapterData ->
                    val chapterList = chapterData.chapters
                    _chapters.value = chapterList
                    
                    // Count only whole chapters (exclude decimal sub-chapters like 1.1, 1.2)
                    val totalChapters = countWholeChapters(chapterList)
                    Log.d("CHAPTERS", "Loaded ${chapterList.size} items, whole chapters: $totalChapters")
                    val baseMangaId = mangaUrl.substringAfter("/manga/").substringBefore("?")
                    val uniqueMangaId = extractUniqueMangaId(baseMangaId, mangaUrl)
                    Log.d("CHAPTERS", "Using mangaId: $uniqueMangaId")
                    Log.d("CHAPTERS", "Total chapters: $totalChapters")
                    
                    // Update detail with correct count
                    _mangaDetail.value?.let { detail ->
                        _mangaDetail.value = detail.copy(
                            totalChapterCount = totalChapters,
                            synopsis = "${detail.synopsis}\n\nOriginal: Dragon Ball (${chapterData.dbChapters}) | Dragon Ball Z (${chapterData.dbzChapters})"
                        )
                    }
                    
                    trackingManager.updateTotalChapters(uniqueMangaId, totalChapters)
                    refreshTrackingLists()
                    
                    // Use the index that was set in startReading/continue
                    val savedIndex = _selectedChapterIndex.value
                    
                    if (savedIndex > 0 && savedIndex < chapterList.size) {
                        Log.d("CHAPTERS", "Loading chapter: $savedIndex")
                        selectChapter(savedIndex)
                    } else if (savedIndex == 0) {
                        // Start from beginning (chapter 0)
                        Log.d("CHAPTERS", "Starting from chapter 0")
                        selectChapter(0)
                    }
                    // If -1, stay on chapter list
                },
                onFailure = {
                    Log.d("ERROR", "Failed: ${it.message}")
                }
            )
            _isLoading.value = false
        }
    }

    fun selectChapter(index: Int) {
        if (index >= 0 && index < _chapters.value.size) {
            _selectedChapterIndex.value = index
            _isChapterRead.value = false
            _isLoading.value = true
            val chapter = _chapters.value[index]
            currentMangaId?.let { mangaId ->
                val tracking = trackingManager.getMangaTracking(mangaId)
                if (tracking != null && index < tracking.currentChapterIndex) {
                    _isChapterRead.value = true
                }
            }
            log("SELECT", "Selected chapter $index: ${chapter.url}")
            
            loadChapterImages(chapter.url)
        }
    }

    private fun fetchImagesFromExtension(
        mangaTitle: String,
        chapterParam: String,
        authority: String
    ): Result<List<String>> {
        return try {
            val uri = Uri.parse("content://$authority/scrape")
                .buildUpon()
                .appendQueryParameter("manga", mangaTitle)
                .appendQueryParameter("anime", mangaTitle)
                .appendQueryParameter("chapter", chapterParam)
                .build()

            log("EXT", "Querying extension: manga='$mangaTitle' chapter='$chapterParam' authority='$authority'")

            val cursor = context.contentResolver.query(uri, null, null, null, null)
                ?: return Result.failure(Exception("Extension returned null cursor"))

            cursor.use { c ->
                if (!c.moveToFirst()) return Result.failure(Exception("Extension returned no data"))
                val col = c.getColumnIndex("data")
                if (col == -1) return Result.failure(Exception("Missing 'data' column"))
                val jsonData = c.getString(col)

                val json = JSONObject(jsonData)
                if (json.has("error")) return Result.failure(Exception(json.getString("error")))

                val chapter = json.optJSONObject("chapter")
                    ?: return Result.failure(Exception("Unexpected response"))
                val imagesArray = chapter.optJSONArray("images")
                    ?: return Result.failure(Exception("No images in response"))
                val images = mutableListOf<String>()
                for (i in 0 until imagesArray.length()) {
                    images.add(imagesArray.getString(i))
                }
                if (images.isEmpty()) return Result.failure(Exception("Chapter has no images"))
                log("EXT", "Extension returned ${images.size} images, first: ${images.firstOrNull()?.take(80)}")
                Result.success(images)
            }
        } catch (e: Exception) {
            log("EXT", "Extension threw: ${e.message}")
            Result.failure(e)
        }
    }

    private fun loadChapterImages(chapterUrl: String) {
        log("LOAD", "Attempting to load: $chapterUrl")

        viewModelScope.launch {
            _chapterImages.value = UiState.Loading

            val authority = _selectedExtensionAuthority.value
            if (authority != null) {
                val chapter = _chapters.value.getOrNull(_selectedChapterIndex.value)
                val title = chapter?.title ?: ""
                val chapterParam = extractChapterNumberString(title)
                val mangaTitle = currentMangaTitle
                    ?: _mangaDetail.value?.englishTitle
                    ?: _mangaDetail.value?.title
                    ?: ""
                if (mangaTitle.isNotBlank() && chapterParam.isNotBlank()) {
                    log("LOAD", "Fetching: title='$title' -> chapterParam='$chapterParam' mangaTitle='$mangaTitle'")
                    val extResult = withContext(Dispatchers.IO) {
                        fetchImagesFromExtension(mangaTitle, chapterParam, authority)
                    }
                    extResult.onSuccess { images ->
                        val ci = ChapterImages(chapterUrl, images)
                        log("LOAD", "Extension success: ${images.size} images for $chapterUrl")
                        _chapterImages.value = UiState.Success(ci)
                        _isLoading.value = false
                        return@launch
                    }
                    log("LOAD", "Extension failed: ${extResult.exceptionOrNull()?.message}")
                }
            }

            _chapterImages.value = UiState.Error(
                if (authority == null) "No extension selected. Install and select an extension in Settings."
                else "Failed to load chapter images from extension"
            )
            _isLoading.value = false
        }
    }

    fun goToNext() {
        val current = _selectedChapterIndex.value
        if (current < _chapters.value.size - 1) {
            selectChapter(current + 1)
        }
    }

    fun goToPrevious() {
        val current = _selectedChapterIndex.value
        if (current > 0) {
            selectChapter(current - 1)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = UiState.Idle
        lastSearchedQuery = ""
    }

    fun clearSelection() {
        _chapters.value = emptyList()
        _selectedChapterIndex.value = -1
        _chapterImages.value = UiState.Idle
    }
    
    fun showChapterList() {
        // Just show chapter list without auto-selecting
        _selectedChapterIndex.value = -1
        _chapterImages.value = UiState.Idle
        _isLoading.value = false
    }
    
    fun showChapterListOnly() {
        // Show chapter list without loading any chapter - but ensure chapters are loaded
        _selectedChapterIndex.value = -1
        _chapterImages.value = UiState.Idle
        
        // If chapters not loaded, load them
        if (_chapters.value.isEmpty()) {
            Log.d("CHAPTERS", "Chapters not loaded, loading for chapter list")
            currentMangaUrl?.let { url ->
                _isLoading.value = true
                loadChaptersForList()
            }
        } else {
            _isLoading.value = false
        }
    }
    
    private fun loadChaptersForList() {
        viewModelScope.launch {
            val result = repository.getChapters(currentMangaUrl!!)
            result.fold(
                onSuccess = { chapterData ->
                    _chapters.value = chapterData.chapters
                    _isLoading.value = false
                    Log.d("CHAPTERS", "Loaded ${chapterData.chapters.size} chapters for list")
                },
                onFailure = { 
                    _isLoading.value = false
                }
            )
        }
    }

    fun getAnilistAuthUrl(): String = anilistManager.getAuthUrl()

    fun isAnilistLoggedIn(): Boolean = anilistManager.isLoggedIn()

    fun handleAuthRedirect(intent: Intent?) {
        intent?.dataString?.takeIf { it.startsWith("animescraper://success") }?.let { uri ->
            Uri.parse(uri.replace("#", "?")).getQueryParameter("access_token")?.let { token ->
                anilistManager.saveAccessToken(token)
                viewModelScope.launch {
                    val nameResult = anilistManager.fetchAndCacheUserInfo()
                    nameResult.fold(
                        onSuccess = { name ->
                            _anilistUsername.value = name
                            val localTracks = trackingManager.getAllTracking()
                            if (localTracks.isNotEmpty()) {
                                _showMergeDialog.value = true
                            } else {
                                syncAnilistManga()
                            }
                        },
                        onFailure = { Log.e("ANILIST", "Failed to fetch user info") }
                    )
                }
            }
        }
    }

    fun syncAnilistManga() {
        viewModelScope.launch {
            _isAniListSyncing.value = true
            val result = anilistManager.getUserMangaLists()
            result.fold(
                onSuccess = { entries ->
                    Log.d("ANILIST", "Fetched ${entries.size} entries from AniList")
                },
                onFailure = { Log.e("ANILIST", "Sync failed: ${it.message}") }
            )
            // Match and create tracking entries outside fold to allow suspend calls
            val entries = anilistManager.getSyncedManga()
            val matched = mutableListOf<com.blissless.oni.data.AniListMangaEntry>()
            for (entry in entries) {
                if (entry.status in setOf("CURRENT", "PLANNING", "REPEATING")) {
                    if (entry.localMangaUrl == null) {
                        matched.add(matchWithSearch(entry))
                    } else if (entry.chapters == null) {
                        matched.add(fillMissingChapters(entry))
                    } else {
                        matched.add(entry)
                    }
                } else {
                    matched.add(entry)
                }
            }
            anilistManager.createTrackingEntries(matched, trackingManager)
            refreshTrackingLists()
            _anilistUsername.value = anilistManager.getLoggedInUser()
            Log.d("ANILIST", "Synced ${matched.size} manga entries")
            _isAniListSyncing.value = false
        }
    }

    fun overwriteAnilistWithLocal() {
        _showMergeDialog.value = false
        viewModelScope.launch {
            val localTracks = trackingManager.getAllTracking()
            for (track in localTracks) {
                val mediaId = track.anilistMediaId ?: extractAnilistMediaId(track.mangaId)
                if (mediaId != null && anilistManager.isLoggedIn()) {
                    val anilistStatus = when (track.status) {
                        ReadingStatus.READING -> "CURRENT"
                        ReadingStatus.PLANNING -> "PLANNING"
                        ReadingStatus.COMPLETED -> "COMPLETED"
                        ReadingStatus.ON_HOLD -> "PAUSED"
                        ReadingStatus.DROPPED -> "DROPPED"
                    }
                    anilistManager.updateMediaListEntry(mediaId, track.currentChapterNumber, anilistStatus)
                }
            }
        }
    }

    fun discardLocalAndSync() {
        _showMergeDialog.value = false
        for (track in trackingManager.getAllTracking()) {
            trackingManager.removeTracking(track.mangaId)
        }
        refreshTrackingLists()
        syncAnilistManga()
    }

    fun mergeLocalAndAnilist() {
        _showMergeDialog.value = false
        viewModelScope.launch {
            val result = anilistManager.getUserMangaLists()
            result.fold(
                onSuccess = { Log.d("ANILIST", "Fetched ${it.size} entries from AniList") },
                onFailure = { Log.e("ANILIST", "Sync failed: ${it.message}") }
            )
            val entries = anilistManager.getSyncedManga()
            val matched = mutableListOf<com.blissless.oni.data.AniListMangaEntry>()
            for (entry in entries) {
                val existing = trackingManager.getMangaTracking("anilist_${entry.mediaId}")
                if (existing != null) {
                    matched.add(entry)
                } else {
                    if (entry.status in setOf("CURRENT", "PLANNING", "REPEATING")) {
                        if (entry.localMangaUrl == null) {
                            matched.add(matchWithSearch(entry))
                        } else if (entry.chapters == null) {
                            matched.add(fillMissingChapters(entry))
                        } else {
                            matched.add(entry)
                        }
                    } else {
                        matched.add(entry)
                    }
                }
            }
            anilistManager.createTrackingEntries(matched, trackingManager)
            refreshTrackingLists()
            _anilistUsername.value = anilistManager.getLoggedInUser()
            _isAniListSyncing.value = false
        }
    }

    private suspend fun matchWithSearch(entry: com.blissless.oni.data.AniListMangaEntry): com.blissless.oni.data.AniListMangaEntry {
        val titlesToTry = listOfNotNull(entry.englishTitle, entry.title, entry.nativeTitle)
        for (title in titlesToTry) {
            try {
                val result = repository.search(title)
                result.getOrNull()?.let { results ->
                    val match = results.firstOrNull { r ->
                        r.title.equals(entry.title, ignoreCase = true) ||
                        r.title.equals(entry.englishTitle, ignoreCase = true) ||
                        entry.title.equals(r.title, ignoreCase = true)
                    }
                    if (match != null) {
                        var matchedEntry = entry.copy(localMangaUrl = match.url)
                        if (matchedEntry.chapters == null) {
                            val chaptersResult = repository.getChapters(match.url)
                            chaptersResult.fold(
                                onSuccess = { chapterData ->
                                    val total = countWholeChapters(chapterData.chapters)
                                    if (total > 0) {
                                        matchedEntry = matchedEntry.copy(chapters = total)
                                    }
                                },
                                onFailure = { }
                            )
                        }
                        anilistManager.updateSyncedMangaEntry(matchedEntry)
                        return matchedEntry
                    }
                }
            } catch (e: Exception) {
                Log.e("ANILIST", "Search failed for ${entry.title}: ${e.message}")
            }
        }
        return entry
    }

    private suspend fun fillMissingChapters(entry: com.blissless.oni.data.AniListMangaEntry): com.blissless.oni.data.AniListMangaEntry {
        val url = entry.localMangaUrl ?: return entry
        val chaptersResult = repository.getChapters(url)
        chaptersResult.fold(
            onSuccess = { chapterData ->
                val total = countWholeChapters(chapterData.chapters)
                if (total > 0) {
                    val updated = entry.copy(chapters = total)
                    anilistManager.updateSyncedMangaEntry(updated)
                    return updated
                }
            },
            onFailure = { }
        )
        return entry
    }

    fun getAnilistUsername(): String? = anilistManager.getLoggedInUser()

    fun logoutAniList() {
        val allTracks = trackingManager.getAllTracking()
        for (track in allTracks) {
            if (track.mangaId.startsWith("anilist_") || track.anilistMediaId != null) {
                trackingManager.removeTracking(track.mangaId)
            }
        }
        anilistManager.logout()
        _anilistUsername.value = null
        refreshTrackingLists()
    }

    fun checkAnilistSession() {
        if (anilistManager.isLoggedIn()) {
            _anilistUsername.value = anilistManager.getLoggedInUser()
        } else {
            _anilistUsername.value = null
        }
    }

    fun setManualChapterProgress(chapterNumber: Int) {
        val mangaId = currentMangaId ?: return
        val totalChs = countWholeChapters(_chapters.value)
            .coerceAtLeast(_mangaDetail.value?.totalChapterCount ?: 0)
            .let { if (it <= 0) Int.MAX_VALUE else it }
        val clamped = chapterNumber.coerceAtMost(totalChs)
        val existing = trackingManager.getMangaTracking(mangaId)
        val chapterIndex = (clamped - 1).coerceAtLeast(0)
        if (existing != null) {
            val updated = existing.copy(
                currentChapterIndex = chapterIndex,
                currentChapterNumber = clamped,
                lastReadTimestamp = System.currentTimeMillis(),
                status = ReadingStatus.READING
            )
            trackingManager.updateTracking(updated)
            // Send to AniList immediately (no debounce for manual input)
            updateAnilistProgressNow(updated)
        } else {
            val track = MangaTrack(
                mangaId = mangaId,
                title = currentMangaTitle ?: (_mangaDetail.value?.title ?: ""),
                coverUrl = currentMangaCoverUrl,
                currentChapterIndex = chapterIndex,
                currentChapterNumber = clamped,
                currentChapterUrl = "",
                totalChapters = countWholeChapters(_chapters.value).coerceAtLeast(_mangaDetail.value?.totalChapterCount ?: 0),
                status = ReadingStatus.READING,
                lastReadTimestamp = System.currentTimeMillis(),
                mangaUrl = currentMangaUrl ?: "https://atsu.moe/manga/$mangaId"
            )
            trackingManager.updateTracking(track)
            updateAnilistProgressNow(track)
        }
        refreshTrackingLists()
    }

    fun clearResumeScrollProgress() {
        _resumeScrollProgress.value = -1f
    }

    fun clearResumeProgress(mangaId: String) {
        trackingManager.resetScrollProgress(mangaId)
        refreshTrackingLists()
    }

    fun clearMangaDetail() {
        _mangaDetail.value = null
    }

    fun updateAnilistSyncThreshold(percent: Int) {
        settingsManager.setAniListSyncThreshold(percent)
        _anilistSyncThreshold.value = percent
    }

    private fun stripHtml(html: String): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString()
        }
    }

    private fun formatStatus(status: String): String = when (status.uppercase()) {
        "RELEASING" -> "Ongoing"
        "FINISHED" -> "Completed"
        "NOT_YET_RELEASED" -> "Not Released"
        "CANCELLED" -> "Cancelled"
        "HIATUS" -> "On Hiatus"
        else -> status
    }

}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
