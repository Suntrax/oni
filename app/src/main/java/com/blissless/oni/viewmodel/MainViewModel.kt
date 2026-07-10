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
import com.blissless.oni.data.AniListMangaDetail
import com.blissless.oni.data.AniListSearchResult
import com.blissless.oni.data.ChapterInfo
import com.blissless.oni.data.ChapterImages
import com.blissless.oni.data.ExploreSection
import com.blissless.oni.data.MangaDexManager
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

    private val trackingManager = TrackingManager(context)
    private val anilistManager = AniListManager(context)
    private val settingsManager = SettingsManager(context)
    private val mangaDexManager = MangaDexManager()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<UiState<List<AniListSearchResult>>>(UiState.Idle)
    val searchResults: StateFlow<UiState<List<AniListSearchResult>>> = _searchResults.asStateFlow()

    private val _exploreSections = MutableStateFlow<List<ExploreSection>>(emptyList())
    val exploreSections: StateFlow<List<ExploreSection>> = _exploreSections.asStateFlow()

    private val _continueReading = MutableStateFlow<List<MangaTrack>>(emptyList())
    val continueReading: StateFlow<List<MangaTrack>> = _continueReading.asStateFlow()

    private val _resumeReading = MutableStateFlow<List<MangaTrack>>(emptyList())
    val resumeReading: StateFlow<List<MangaTrack>> = _resumeReading.asStateFlow()

    private val _planningToRead = MutableStateFlow<List<MangaTrack>>(emptyList())
    val planningToRead: StateFlow<List<MangaTrack>> = _planningToRead.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chapters: StateFlow<List<ChapterInfo>> = _chapters.asStateFlow()

    private val _mangaDetail = MutableStateFlow<AniListMangaDetail?>(null)
    val mangaDetail: StateFlow<AniListMangaDetail?> = _mangaDetail.asStateFlow()

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
    private var currentMediaId: Int? = null

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
        if (anilistManager.isLoggedIn()) {
            viewModelScope.launch {
                syncAnilistManga()
            }
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

    // ======================== Explore Page ========================

    fun loadExplorePage() {
        viewModelScope.launch {
            _isLoading.value = true
            refreshTrackingLists()
            if (anilistManager.isLoggedIn()) {
                syncAnilistManga()
            }
            val result = anilistManager.getExploreSections()
            result.fold(
                onSuccess = { sections ->
                    _exploreSections.value = sections
                    log("EXPLORE", "Loaded ${sections.size} sections")
                },
                onFailure = {
                    log("ERROR", "Failed to load explore: ${it.message}")
                }
            )
            _isLoading.value = false
        }
    }

    // ======================== Tracking ========================

    fun refreshTrackingLists() {
        val allReading = trackingManager.getContinueReading()
        _resumeReading.value = allReading.filter { it.scrollProgress > 0f }
        _continueReading.value = allReading
        _planningToRead.value = trackingManager.getPlanningToRead()
    }

    fun addToPlanning(mangaId: String, title: String, coverUrl: String?, mangaUrl: String, totalChapters: Int) {
        trackingManager.markAsPlanning(mangaId, title, coverUrl, mangaUrl, totalChapters)
        refreshTrackingLists()
        val track = trackingManager.getMangaTracking(mangaId)
        if (track != null) updateAnilistProgressNow(track)
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
        val track = trackingManager.getMangaTracking(mangaId)
        if (track != null) updateAnilistProgressNow(track)
    }

    fun togglePlanning(mangaId: String, title: String, coverUrl: String?, mangaUrl: String, totalChapters: Int) {
        if (isInPlanning(mangaId)) {
            removeFromPlanning(mangaId)
        } else {
            addToPlanning(mangaId, title, coverUrl, mangaUrl, totalChapters)
        }
    }

    fun isInPlanning(mangaId: String): Boolean {
        return trackingManager.getMangaTracking(mangaId)?.status == ReadingStatus.PLANNING
    }

    fun getMangaTracking(mangaId: String): MangaTrack? {
        return trackingManager.getMangaTracking(mangaId)
    }

    fun updateTrackingStatus(mangaId: String, status: ReadingStatus) {
        val existing = resolveMangaTracking(mangaId)
        if (existing != null) {
            val updated = existing.copy(
                status = status,
                lastReadTimestamp = System.currentTimeMillis()
            )
            trackingManager.updateTracking(updated)
            updateAnilistProgressNow(updated)
        } else {
            val detail = _mangaDetail.value
            val track = MangaTrack(
                mangaId = mangaId,
                title = currentMangaTitle ?: "",
                coverUrl = currentMangaCoverUrl,
                currentChapterIndex = 0,
                currentChapterNumber = 0,
                currentChapterUrl = "",
                totalChapters = detail?.chapters ?: 0,
                status = status,
                lastReadTimestamp = System.currentTimeMillis(),
                mangaUrl = currentMangaUrl ?: "https://anilist.co/manga/${currentMediaId ?: ""}",
                anilistMediaId = currentMediaId
            )
            trackingManager.updateTracking(track)
            updateAnilistProgressNow(track)
        }
        refreshTrackingLists()
    }

    fun removeFromAnilist(mangaId: String) {
        val track = resolveMangaTracking(mangaId) ?: return
        val mediaId = track.anilistMediaId ?: extractAnilistMediaId(mangaId)
        if (mediaId != null && anilistManager.isLoggedIn()) {
            viewModelScope.launch {
                anilistManager.deleteMediaListEntry(mediaId)
            }
        }
        trackingManager.removeTracking(mangaId)
        refreshTrackingLists()
    }

    // ======================== Select Manga & Load Detail ========================

    fun selectManga(manga: AniListSearchResult) {
        log("SELECT", "Selected from AniList: ${manga.title} (id=${manga.id})")
        val mangaId = "anilist_${manga.id}"
        currentMangaId = mangaId
        currentMangaTitle = manga.title
        currentMangaCoverUrl = manga.coverUrl
        currentMangaUrl = "https://anilist.co/manga/${manga.id}"
        currentMediaId = manga.id
        _mangaDetail.value = null
        _isLoading.value = true

        viewModelScope.launch {
            loadAniListDetail(manga.id, manga.coverUrl)
            loadAniListChapters(manga.id)
            loadReadChapters(mangaId)
        }
    }

    fun selectManga(manga: MangaSearchResult) {
        log("SELECT", "Selected from result: ${manga.title}")
        val mediaId = extractMediaIdFromMangaId(manga.mangaId)
        if (mediaId != null) {
            currentMangaId = manga.mangaId
            currentMangaTitle = manga.title
            currentMangaCoverUrl = manga.coverUrl
            currentMangaUrl = "https://anilist.co/manga/$mediaId"
            currentMediaId = mediaId
            _mangaDetail.value = null
            _isLoading.value = true

            viewModelScope.launch {
                loadAniListDetail(mediaId, manga.coverUrl)
                loadAniListChapters(mediaId)
                manga.mangaId?.let { loadReadChapters(it) }
            }
        } else {
            // Legacy tracking data without AniList ID - try looking up by title
            currentMangaId = manga.mangaId
            currentMangaTitle = manga.title
            currentMangaCoverUrl = manga.coverUrl
            currentMangaUrl = manga.url
            log("WARN", "No AniList media ID for ${manga.title}, attempting search lookup")
            viewModelScope.launch {
                val searchResult = anilistManager.searchManga(manga.title)
                searchResult.fold(
                    onSuccess = { results ->
                        val match = results.firstOrNull()
                        if (match != null) {
                            currentMediaId = match.id
                            currentMangaUrl = "https://anilist.co/manga/${match.id}"
                            currentMangaId = "anilist_${match.id}"
                            loadAniListDetail(match.id, match.coverUrl ?: manga.coverUrl)
                            loadAniListChapters(match.id)
                            loadReadChapters("anilist_${match.id}")
                        } else {
                            _mangaDetail.value = null
                            _isLoading.value = false
                        }
                    },
                    onFailure = {
                        _isLoading.value = false
                    }
                )
            }
        }
    }

    private suspend fun loadAniListDetail(mediaId: Int, coverUrl: String?) {
        val result = anilistManager.getMediaDetail(mediaId)
        result.fold(
            onSuccess = { detail ->
                val effectiveCover = detail.coverExtraLarge ?: detail.coverLarge ?: coverUrl ?: currentMangaCoverUrl
                currentMangaCoverUrl = effectiveCover
                _mangaDetail.value = detail
                currentMangaId?.let { mangaId ->
                    val existing = trackingManager.getMangaTracking(mangaId)
                    if (existing != null && effectiveCover != null && existing.coverUrl != effectiveCover) {
                        trackingManager.updateTracking(existing.copy(coverUrl = effectiveCover))
                    }
                }
                log("DETAIL", "Loaded: ${detail.titleRomaji}")
                refreshTrackingLists()
            },
            onFailure = {
                log("ERROR", "Failed to load detail: ${it.message}")
            }
        )
        _isLoading.value = false
    }

    private fun generateChapterList(mediaId: Int, totalChapters: Int): List<ChapterInfo> {
        if (totalChapters <= 0) return emptyList()
        return (1..totalChapters).map { i ->
            ChapterInfo(url = "anilist_${mediaId}_ch_$i", title = "Chapter $i")
        }.reversed()
    }

    private suspend fun resolveChapterCount(mediaId: Int): Int {
        val detail = _mangaDetail.value
        val mangaId = currentMangaId
        val tracking = mangaId?.let { trackingManager.getMangaTracking(it) }

        if (detail?.chapters != null) return detail.chapters

        val isOngoing = detail?.status == "RELEASING"

        if (isOngoing) {
            val title = detail?.titleEnglish ?: detail?.titleRomaji ?: currentMangaTitle
            if (title != null) {
                val mdChapters = mangaDexManager.getChapterCount(title, mediaId)
                if (mdChapters != null && mdChapters > 0) {
                    mangaId?.let { trackingManager.updateTotalChapters(it, mdChapters) }
                    return mdChapters
                }
            }
        }

        return tracking?.totalChapters ?: 0
    }

    private suspend fun loadAniListChapters(mediaId: Int) {
        val mangaId = currentMangaId
        val totalChapters = resolveChapterCount(mediaId)
        val chapterList = generateChapterList(mediaId, totalChapters)

        _chapters.value = chapterList
        log("CHAPTERS", "Generated ${chapterList.size} chapters for media $mediaId")

        mangaId?.let { trackingManager.updateTotalChapters(it, totalChapters) }
        refreshTrackingLists()
        _isLoading.value = false
    }

    // ======================== Continue / Resume Reading ========================

    fun continueFromTracking(track: MangaTrack, onReady: () -> Unit) {
        val mediaId = track.anilistMediaId ?: extractAnilistMediaId(track.mangaId)
        if (mediaId == null) {
            log("ERROR", "No AniList media ID for tracking entry")
            _isLoading.value = false
            return
        }
        currentMediaId = mediaId
        currentMangaId = track.mangaId
        currentMangaTitle = track.title
        currentMangaCoverUrl = track.coverUrl
        currentMangaUrl = track.mangaUrl
        _selectedChapterIndex.value = -1
        _isLoading.value = true
        _chapterImages.value = UiState.Idle

        refreshTrackingLists()

        viewModelScope.launch {
            val detailResult = anilistManager.getMediaDetail(mediaId)
            detailResult.fold(
                onSuccess = { mangaDetail ->
                    currentMangaCoverUrl = mangaDetail.coverExtraLarge ?: mangaDetail.coverLarge ?: track.coverUrl
                    _mangaDetail.value = mangaDetail
                    log("DETAIL", "Loaded detail for continue")
                },
                onFailure = {
                    log("ERROR", "Failed to load detail: ${it.message}")
                }
            )

            val totalChapters = resolveChapterCount(mediaId)
            val chapterList = generateChapterList(mediaId, totalChapters)
            _chapters.value = chapterList
            trackingManager.updateTotalChapters(track.mangaId, totalChapters)
            refreshTrackingLists()

            val savedUrl = track.currentChapterUrl
            var currentPosition = -1

            if (savedUrl.isNotBlank()) {
                currentPosition = chapterList.indexOfFirst { it.url == savedUrl }
            }

            if (currentPosition < 0 && track.currentChapterNumber > 0) {
                currentPosition = chapterList.indexOfFirst { ch ->
                    ch.title?.contains(track.currentChapterNumber.toString()) == true
                }
            }

            if (currentPosition < 0) {
                currentPosition = track.currentChapterIndex.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            }

            val safeChapterIndex = currentPosition.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            val nextToRead = if (track.currentChapterNumber > 0) safeChapterIndex + 1 else 0

            _readChapterIndices.value = (0 until safeChapterIndex).toSet()
            _nextChapterToRead.value = nextToRead + 1

            val chapter = chapterList.getOrNull(nextToRead)
            if (chapter != null) {
                _selectedChapterIndex.value = nextToRead
                _isChapterRead.value = safeChapterIndex > 0
                _chapterImages.value = UiState.Loading
                loadChapterImages(chapter.url)
                onReady()
            } else {
                _selectedChapterIndex.value = safeChapterIndex
                _chapterImages.value = UiState.Loading
                loadChapterImages(chapterList[safeChapterIndex].url)
            }
        }
    }

    fun resumeFromTracking(track: MangaTrack, onReady: () -> Unit) {
        val mediaId = track.anilistMediaId ?: extractAnilistMediaId(track.mangaId)
        if (mediaId == null) {
            log("ERROR", "No AniList media ID for resume")
            _isLoading.value = false
            return
        }
        currentMediaId = mediaId
        currentMangaId = track.mangaId
        currentMangaTitle = track.title
        currentMangaCoverUrl = track.coverUrl
        currentMangaUrl = track.mangaUrl
        _selectedChapterIndex.value = -1
        _isLoading.value = true
        _chapterImages.value = UiState.Idle
        _resumeScrollProgress.value = track.scrollProgress

        viewModelScope.launch {
            val detailResult = anilistManager.getMediaDetail(mediaId)
            detailResult.fold(
                onSuccess = { mangaDetail ->
                    currentMangaCoverUrl = mangaDetail.coverExtraLarge ?: mangaDetail.coverLarge ?: track.coverUrl
                    _mangaDetail.value = mangaDetail
                },
                onFailure = {
                    log("ERROR", "Failed to load detail: ${it.message}")
                }
            )

            val totalChapters = resolveChapterCount(mediaId)
            val chapterList = generateChapterList(mediaId, totalChapters)
            _chapters.value = chapterList
            trackingManager.updateTotalChapters(track.mangaId, totalChapters)

            val safeIndex = (track.currentChapterIndex + 1).coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            val chapter = chapterList.getOrNull(safeIndex)
            if (chapter != null) {
                _selectedChapterIndex.value = safeIndex
                _isChapterRead.value = false
                _isLoading.value = true
                loadChapterImages(chapter.url)
                onReady()
            }
        }
    }

    fun continueFromCurrentManga(onReady: () -> Unit) {
        val mangaDetail = _mangaDetail.value ?: return
        val mediaId = currentMediaId ?: return
        val mangaId = currentMangaId ?: return
        val mangaUrl = currentMangaUrl ?: return

        _selectedChapterIndex.value = -1
        _isLoading.value = true
        _chapterImages.value = UiState.Idle

        val tracking = trackingManager.getMangaTracking(mangaId)
        val savedIndex = if (tracking?.status == ReadingStatus.READING) {
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
            val totalChapters = resolveChapterCount(mediaId)
            val chapters = generateChapterList(mediaId, totalChapters)
            _chapters.value = chapters

            val safeChapterIndex = nextChapterIndex.coerceIn(0, chapters.lastIndex.coerceAtLeast(0))
            _readChapterIndices.value = (0 until safeChapterIndex).toSet()
            _nextChapterToRead.value = safeChapterIndex

            val chapter = chapters.getOrNull(safeChapterIndex)
            if (chapter != null) {
                _selectedChapterIndex.value = safeChapterIndex
                _isChapterRead.value = safeChapterIndex > 0
                loadChapterImages(chapter.url)
                onReady()
            }
        }
    }

    fun startReading() {
        _mangaDetail.value?.let { detail ->
            val mediaId = currentMediaId ?: return
            val mangaId = "anilist_$mediaId"
            currentMangaId = mangaId
            currentMangaTitle = detail.titleRomaji
            currentMangaCoverUrl = detail.coverExtraLarge ?: detail.coverLarge

            val existingTracking = trackingManager.getMangaTracking(mangaId)
            val savedIndex = if (existingTracking?.status == ReadingStatus.READING &&
                existingTracking.currentChapterIndex > 0) {
                existingTracking.currentChapterIndex
            } else {
                0
            }

            _readChapterIndices.value = emptySet()
            _nextChapterToRead.value = 0
            _selectedChapterIndex.value = 0
            _isLoading.value = true

            addToReading(mangaId, detail.titleRomaji, currentMangaCoverUrl, currentMangaUrl ?: "https://anilist.co/manga/$mediaId", detail.chapters ?: 0, resetProgress = true)
            generateAndLoadChapters(mediaId)
        }
    }

    private fun generateAndLoadChapters(mediaId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedChapterIndex.value = -1
            _chapterImages.value = UiState.Idle

            val mangaId = currentMangaId
            val totalChapters = resolveChapterCount(mediaId)
            val chapterList = generateChapterList(mediaId, totalChapters)
            _chapters.value = chapterList

            mangaId?.let {
                trackingManager.updateTotalChapters(it, totalChapters)
                refreshTrackingLists()
            }

            _isLoading.value = false

            val savedIndex = _selectedChapterIndex.value
            if (savedIndex > 0 && savedIndex < chapterList.size) {
                selectChapter(savedIndex)
            } else if (savedIndex == 0) {
                selectChapter(0)
            }
        }
    }

    // ======================== Chapter Selection & Reading ========================

    private fun selectChapterFromIndex(index: Int) {
        _selectedChapterIndex.value = index
        _isChapterRead.value = false
        _isLoading.value = true
        val chapter = _chapters.value.getOrNull(index)
        currentMangaId?.let { mangaId ->
            val tracking = trackingManager.getMangaTracking(mangaId)
            if (tracking != null && index < tracking.currentChapterIndex) {
                _isChapterRead.value = true
            }
        }
        if (chapter != null) {
            loadChapterImages(chapter.url)
        }
    }

    fun selectChapter(index: Int) {
        if (index >= 0 && index < _chapters.value.size) {
            selectChapterFromIndex(index)
        }
    }

    fun showChapterList() {
        _selectedChapterIndex.value = -1
        _chapterImages.value = UiState.Idle
        _isLoading.value = false
    }

    fun showChapterListOnly() {
        _selectedChapterIndex.value = -1
        _chapterImages.value = UiState.Idle
        if (_chapters.value.isEmpty()) {
            val mediaId = currentMediaId
            if (mediaId == null) {
                _isLoading.value = false
                return
            }
            _isLoading.value = true
            generateAndLoadChapters(mediaId)
        } else {
            _isLoading.value = false
        }
    }

    // ======================== Scroll Progress & Tracking ========================

    fun onChapterScrollProgress(scrollPercent: Float) {
        val threshold = _anilistSyncThreshold.value / 100f
        if (_selectedChapterIndex.value < 0) return
        if (!scrollPercent.isFinite()) return

        currentMangaId?.let { mangaId ->
            val chapter = _chapters.value.getOrNull(_selectedChapterIndex.value)
            if (chapter != null) {
                val chapterNumber = _selectedChapterIndex.value + 1
                val existing = trackingManager.getMangaTracking(mangaId)

                if (existing == null) {
                    val track = MangaTrack(
                        mangaId = mangaId,
                        title = currentMangaTitle ?: "",
                        coverUrl = currentMangaCoverUrl,
                        currentChapterIndex = _selectedChapterIndex.value,
                        currentChapterNumber = chapterNumber,
                        currentChapterUrl = chapter.url,
                        totalChapters = _chapters.value.size,
                        status = ReadingStatus.READING,
                        lastReadTimestamp = System.currentTimeMillis(),
                        mangaUrl = currentMangaUrl ?: "https://anilist.co/manga/${currentMediaId ?: ""}",
                        scrollProgress = scrollPercent,
                        anilistMediaId = currentMediaId
                    )
                    trackingManager.updateTracking(track)
                    log("TRACK", "Created tracking for chapter ${_selectedChapterIndex.value} at $scrollPercent")
                } else {
                    trackingManager.updateScrollProgress(mangaId, scrollPercent)
                }

                if (scrollPercent >= threshold) {
                    val trackForUpdate = trackingManager.getMangaTracking(mangaId)
                    if (trackForUpdate == null || trackForUpdate.currentChapterIndex != _selectedChapterIndex.value || trackForUpdate.currentChapterNumber < 0) {
                        trackingManager.updateChapterProgress(mangaId, _selectedChapterIndex.value, chapterNumber, chapter.url)
                        log("TRACK", "Updated to chapter ${_selectedChapterIndex.value}")
                    } else {
                        trackingManager.resetScrollProgress(mangaId)
                    }
                    _isChapterRead.value = true
                    _readChapterIndices.value = _readChapterIndices.value + _selectedChapterIndex.value
                    _nextChapterToRead.value = _selectedChapterIndex.value + 1

                    val tracking = trackingManager.getMangaTracking(mangaId)
                    if (tracking != null) {
                        scheduleAnilistProgressUpdate(tracking)
                    }
                }

                refreshTrackingLists()
            }
        }
    }

    fun updateCurrentChapter() {
        currentMangaId?.let { mangaId ->
            val chapter = _chapters.value.getOrNull(_selectedChapterIndex.value)
            if (chapter != null) {
                val chapterNumber = _selectedChapterIndex.value + 1
                val existing = trackingManager.getMangaTracking(mangaId)
                if (existing == null) {
                    val track = MangaTrack(
                        mangaId = mangaId,
                        title = currentMangaTitle ?: "",
                        coverUrl = currentMangaCoverUrl,
                        currentChapterIndex = _selectedChapterIndex.value,
                        currentChapterNumber = chapterNumber,
                        currentChapterUrl = chapter.url,
                        totalChapters = _chapters.value.size,
                        status = ReadingStatus.READING,
                        lastReadTimestamp = System.currentTimeMillis(),
                        mangaUrl = currentMangaUrl ?: "https://anilist.co/manga/${currentMediaId ?: ""}",
                        anilistMediaId = currentMediaId
                    )
                    trackingManager.updateTracking(track)
                } else if (existing.currentChapterIndex != _selectedChapterIndex.value || existing.currentChapterNumber < 0) {
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
        var mediaId = track.anilistMediaId ?: extractAnilistMediaId(track.mangaId)
        if (mediaId == null && track.mangaUrl.isNotBlank()) {
            mediaId = anilistManager.getSyncedManga()
                .firstOrNull { it.localMangaUrl == track.mangaUrl || it.siteUrl == track.mangaUrl }
                ?.mediaId
        }
        if (mediaId == null) return
        if (!anilistManager.isLoggedIn()) return
        val anilistStatus = when (track.status) {
            ReadingStatus.READING -> "CURRENT"
            ReadingStatus.PLANNING -> "PLANNING"
            ReadingStatus.COMPLETED -> "COMPLETED"
            ReadingStatus.ON_HOLD -> "PAUSED"
            ReadingStatus.DROPPED -> "DROPPED"
        }
        val safeProgress = maxOf(track.currentChapterNumber, 0)
        viewModelScope.launch {
            val result = anilistManager.updateMediaListEntry(mediaId, safeProgress, anilistStatus)
            result.fold(
                onSuccess = {
                    Log.d("ANILIST", "Updated progress for media $mediaId to chapter $safeProgress")
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

    private fun extractMediaIdFromMangaId(mangaId: String?): Int? {
        if (mangaId == null) return null
        return extractAnilistMediaId(mangaId)
    }

    // ======================== Search ========================

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

    fun loadReadChapters(mangaId: String) {
        val tracking = trackingManager.getMangaTracking(mangaId)
        if (tracking != null) {
            _readChapterIndices.value = (0..tracking.currentChapterIndex).toSet()
            _nextChapterToRead.value = tracking.currentChapterIndex + 1
        } else {
            _readChapterIndices.value = emptySet()
            _nextChapterToRead.value = 0
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

    // ======================== Image Loading (Extensions) ========================

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
                val chapterParam = title.removePrefix("Chapter ").trim()
                val mangaTitle = currentMangaTitle
                    ?: _mangaDetail.value?.titleEnglish
                    ?: _mangaDetail.value?.titleRomaji
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
            selectChapterFromIndex(current + 1)
        }
    }

    fun goToPrevious() {
        val current = _selectedChapterIndex.value
        if (current > 0) {
            selectChapterFromIndex(current - 1)
        }
    }

    // ======================== AniList Auth & Sync ========================

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
            val entries = anilistManager.getSyncedManga()
            val matched = mutableListOf<com.blissless.oni.data.AniListMangaEntry>()
            for (entry in entries) {
                if (entry.status in setOf("CURRENT", "PLANNING", "REPEATING")) {
                    matched.add(entry)
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
                        matched.add(entry)
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

    // ======================== Manual Progress & Settings ========================

    fun setManualChapterProgress(chapterNumber: Int) {
        val mangaId = currentMangaId ?: return
        val totalChs = _chapters.value.size.coerceAtLeast(_mangaDetail.value?.chapters ?: 0)
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
            updateAnilistProgressNow(updated)
        } else {
            val track = MangaTrack(
                mangaId = mangaId,
                title = currentMangaTitle ?: (_mangaDetail.value?.titleRomaji ?: ""),
                coverUrl = currentMangaCoverUrl,
                currentChapterIndex = chapterIndex,
                currentChapterNumber = clamped,
                currentChapterUrl = "",
                totalChapters = _chapters.value.size.coerceAtLeast(_mangaDetail.value?.chapters ?: 0),
                status = ReadingStatus.READING,
                lastReadTimestamp = System.currentTimeMillis(),
                mangaUrl = currentMangaUrl ?: "https://anilist.co/manga/${currentMediaId ?: ""}",
                anilistMediaId = currentMediaId
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
