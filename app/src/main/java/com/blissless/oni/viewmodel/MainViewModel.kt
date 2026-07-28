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
import com.blissless.oni.data.DownloadManager
import com.blissless.oni.data.ExploreSection
import com.blissless.oni.data.MangaDexAggregate
import com.blissless.oni.data.MangaDexManager
import com.blissless.oni.data.MangaSearchResult
import com.blissless.oni.data.MangaTrack
import com.blissless.oni.data.ReaderMode
import com.blissless.oni.data.ReadingStatus
import com.blissless.oni.data.SettingsManager
import com.blissless.oni.data.TrackingManager
import com.blissless.oni.ui.theme.ThemeMode
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

/**
 * A single chapter entry returned by the extension's `/chapters` endpoint.
 *
 * The extension (e.g. atsumaru-extension) searches its source (atsu.moe) for
 * the manga and returns the full chapter list. Each entry has:
 *   - number:   chapter number as a string (e.g. "1", "1.5", "346.2")
 *   - title:    chapter title (may be empty)
 *   - id:       the extension's internal chapter ID (used for image fetching)
 *   - index:    0-based position in the extension's list
 *   - pageCount: number of pages (0 if unknown)
 */
data class ExtensionChapter(
    val number: String,
    val title: String,
    val id: String,
    val index: Int,
    val pageCount: Int
)

data class DownloadedResumeEntry(
    val title: String,
    val slug: String,
    val coverPath: String?,
    val currentChapter: Double,
    val totalChapters: Int,
    val mangaId: String?,
    val scrollProgress: Float = 0f
)

class MainViewModel(private val context: Context) : ViewModel() {

    private val trackingManager = TrackingManager(context)
    private val anilistManager = AniListManager(context)
    private val settingsManager = SettingsManager(context)
    private val mangaDexManager = MangaDexManager()
    val downloadManager = DownloadManager(context, settingsManager)

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

    /**
     * Total volume count for the currently-selected manga.
     *
     * Sources, in priority order:
     *   1. AniList `volumes` field (already exposed via [mangaDetail])
     *   2. MangaDex aggregate volume count (populated here)
     *   3. Cached value on the persisted [MangaTrack]
     *
     * The manga detail screen reads this when [AniListMangaDetail.volumes] is null.
     */
    private val _mangaDexVolumeCount = MutableStateFlow<Int?>(null)
    val mangaDexVolumeCount: StateFlow<Int?> = _mangaDexVolumeCount.asStateFlow()

    /**
     * Total chapter count from MangaDex. Used by the detail screen when
     * [AniListMangaDetail.chapters] is null (which happens for many ongoing manga).
     */
    private val _mangaDexChapterCount = MutableStateFlow<Int?>(null)
    val mangaDexChapterCount: StateFlow<Int?> = _mangaDexChapterCount.asStateFlow()

    /**
     * The MangaDex manga UUID for the currently-selected manga. Cached here so we
     * don't re-query the title-search endpoint on every chapter refresh.
     */
    private var currentMangaDexId: String? = null

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

    private val _openChapterSelectOnLoad = MutableStateFlow(false)
    val openChapterSelectOnLoad: StateFlow<Boolean> = _openChapterSelectOnLoad.asStateFlow()

    fun consumeOpenChapterSelect() { _openChapterSelectOnLoad.value = false }
    fun requestOpenChapterSelect() { _openChapterSelectOnLoad.value = true }

    private var currentMangaId: String? = null
    private var currentMangaTitle: String? = null
    private var currentMangaCoverUrl: String? = null
    private var currentMangaUrl: String? = null
    private var currentMediaId: Int? = null

    private val _mangaTitle = MutableStateFlow<String>("")
    val mangaTitle: StateFlow<String> = _mangaTitle.asStateFlow()

    // AniList state
    private val _anilistUsername = MutableStateFlow<String?>(null)
    val anilistUsername: StateFlow<String?> = _anilistUsername.asStateFlow()

    private val _isAniListSyncing = MutableStateFlow(false)
    val isAniListSyncing: StateFlow<Boolean> = _isAniListSyncing.asStateFlow()

    private val _anilistSyncThreshold = MutableStateFlow(settingsManager.getAniListSyncThreshold())
    val anilistSyncThreshold: StateFlow<Int> = _anilistSyncThreshold.asStateFlow()



    private val _showMergeDialog = MutableStateFlow(false)
    val showMergeDialog: StateFlow<Boolean> = _showMergeDialog.asStateFlow()

    // Profile state
    private val _userProfile = MutableStateFlow<com.blissless.oni.data.AniListUserProfile?>(null)
    val userProfile: StateFlow<com.blissless.oni.data.AniListUserProfile?> = _userProfile.asStateFlow()

    private val _userFavorites = MutableStateFlow<List<com.blissless.oni.data.AniListFavorite>>(emptyList())
    val userFavorites: StateFlow<List<com.blissless.oni.data.AniListFavorite>> = _userFavorites.asStateFlow()

    private val _currentMangaIsFavorited = MutableStateFlow(false)
    val currentMangaIsFavorited: StateFlow<Boolean> = _currentMangaIsFavorited.asStateFlow()

    private val _userActivity = MutableStateFlow<List<com.blissless.oni.data.AniListUserActivity>>(emptyList())
    val userActivity: StateFlow<List<com.blissless.oni.data.AniListUserActivity>> = _userActivity.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    // Downloaded manga scanning
    private val _downloadedManga = MutableStateFlow<List<com.blissless.oni.data.DownloadedManga>>(emptyList())
    val downloadedManga: StateFlow<List<com.blissless.oni.data.DownloadedManga>> = _downloadedManga.asStateFlow()

    private val _downloadedResumeReading = MutableStateFlow<List<DownloadedResumeEntry>>(emptyList())
    val downloadedResumeReading: StateFlow<List<DownloadedResumeEntry>> = _downloadedResumeReading.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    val batchDownloadState: StateFlow<com.blissless.oni.data.BatchDownloadState?> = downloadManager.batchDownloadState

    private var currentOfflineMangaSlug: String? = null
    private var currentOfflinePageIndex: Int = 0
    private var lastSavedOfflinePage: Int = -1

    fun scanDownloadedManga() {
        viewModelScope.launch {
            // Load from disk cache first for instant UI
            if (_downloadedManga.value.isEmpty()) {
                val cached = withContext(Dispatchers.IO) { downloadManager.loadScanCache() }
                if (cached != null) {
                    _downloadedManga.value = cached
                    buildResumeEntries(cached)
                }
            }

            // Full scan in background
            val scanned = withContext(Dispatchers.IO) { downloadManager.scanDownloadedManga() }
            _downloadedManga.value = scanned
            buildResumeEntries(scanned)

            // Save to cache for next startup
            withContext(Dispatchers.IO) { downloadManager.saveScanCache(scanned) }
        }
    }

    private fun buildResumeEntries(mangaList: List<com.blissless.oni.data.DownloadedManga>) {
        val resumeEntries = mutableListOf<DownloadedResumeEntry>()
        for (manga in mangaList) {
            if (manga.lastReadChapter != null && manga.lastReadChapter > 0) {
                val pageIndex = downloadManager.getLastReadPageIndex(manga.slug)
                val chapterPages = manga.chapters.find { it.chapterNumber == manga.lastReadChapter }?.pageCount ?: 1
                resumeEntries.add(DownloadedResumeEntry(
                    title = manga.title,
                    slug = manga.slug,
                    coverPath = manga.coverPath,
                    currentChapter = manga.lastReadChapter,
                    totalChapters = manga.chapters.size,
                    mangaId = null,
                    scrollProgress = if (chapterPages > 0) pageIndex.toFloat() / chapterPages.toFloat() else 0f
                ))
            }
        }
        _downloadedResumeReading.value = resumeEntries.sortedByDescending { it.currentChapter }
    }

    fun getCompletedChapterNumbers(mangaTitle: String): Set<Double> =
        downloadManager.getCompletedChapterNumbers(mangaTitle)

    private val _downloadedChapterNumbers = MutableStateFlow<Set<Double>>(emptySet())
    val downloadedChapterNumbers: StateFlow<Set<Double>> = _downloadedChapterNumbers.asStateFlow()

    fun loadDownloadedChapterNumbers(mangaTitle: String) {
        viewModelScope.launch {
            val nums = withContext(Dispatchers.IO) { downloadManager.getCompletedChapterNumbers(mangaTitle) }
            _downloadedChapterNumbers.value = nums
        }
    }

    fun saveCoverImage(mangaTitle: String, coverUrl: String) =
        downloadManager.saveCoverImage(mangaTitle, coverUrl)

    fun discardOfflineProgress(mangaSlug: String) {
        downloadManager.clearLastReadChapter(mangaSlug)
        _downloadedResumeReading.value = _downloadedResumeReading.value.filter { it.slug != mangaSlug }
        scanDownloadedManga()
    }

    fun loadOfflineChapterImages(mangaTitle: String, chapterNumber: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _isOfflineMode.value = true
            currentMangaTitle = mangaTitle
            currentMangaId = "offline_${mangaTitle}"

            var manga = _downloadedManga.value.find { it.title == mangaTitle || it.slug == mangaTitle }
            if (manga == null) {
                val downloaded = withContext(Dispatchers.IO) { downloadManager.scanDownloadedManga() }
                _downloadedManga.value = downloaded
                manga = downloaded.find { it.title == mangaTitle || it.slug == mangaTitle }
            }
            if (manga != null) {
                currentOfflineMangaSlug = manga.slug

                val savedChapter = withContext(Dispatchers.IO) { downloadManager.getLastReadChapter(manga.slug) }
                val savedPageIndex = withContext(Dispatchers.IO) { downloadManager.getLastReadPageIndex(manga.slug) }

                _chapters.value = manga.chapters.map { ch ->
                    ChapterInfo(url = "offline://${manga.slug}/chapter-${ch.chapterNumber}", title = "Chapter ${ch.chapterNumber}", chapterId = "", volume = null)
                }
                val targetIndex = manga.chapters.indexOfFirst { it.chapterNumber == chapterNumber }
                _selectedChapterIndex.value = targetIndex.coerceAtLeast(0)

                val files = withContext(Dispatchers.IO) {
                    downloadManager.getDownloadedPagesByMangaAndChapter(mangaTitle, chapterNumber)
                }

                val restorePageIndex = if (savedChapter == chapterNumber) savedPageIndex else 0
                currentOfflinePageIndex = restorePageIndex
                lastSavedOfflinePage = -1
                downloadManager.saveLastReadChapter(manga.slug, chapterNumber, restorePageIndex)

                val pageCount = files.size
                _resumeScrollProgress.value = if (pageCount > 0) restorePageIndex.toFloat() / pageCount.toFloat() else 0f

                if (files.isNotEmpty()) {
                    _chapterImages.value = UiState.Success(ChapterImages(chapterUrl = "offline://$mangaTitle/ch$chapterNumber", images = files.map { it.absolutePath }))
                } else {
                    _chapterImages.value = UiState.Error("No offline pages found")
                }
            } else {
                val files = withContext(Dispatchers.IO) {
                    downloadManager.getDownloadedPagesByMangaAndChapter(mangaTitle, chapterNumber)
                }
                if (files.isNotEmpty()) {
                    _chapterImages.value = UiState.Success(ChapterImages(chapterUrl = "offline://$mangaTitle/ch$chapterNumber", images = files.map { it.absolutePath }))
                } else {
                    _chapterImages.value = UiState.Error("No offline pages found")
                }
            }
            _isLoading.value = false
        }
    }

    fun selectOfflineChapter(mangaTitle: String, chapterNumber: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            var manga = _downloadedManga.value.find { it.title == mangaTitle || it.slug == mangaTitle }
            if (manga == null) {
                val downloaded = withContext(Dispatchers.IO) { downloadManager.scanDownloadedManga() }
                _downloadedManga.value = downloaded
                manga = downloaded.find { it.title == mangaTitle || it.slug == mangaTitle }
            }
            if (manga != null) {
                currentOfflinePageIndex = 0
                downloadManager.saveLastReadChapter(manga.slug, chapterNumber, 0)
            }
            val files = withContext(Dispatchers.IO) {
                downloadManager.getDownloadedPagesByMangaAndChapter(mangaTitle, chapterNumber)
            }
            if (files.isNotEmpty()) {
                _chapterImages.value = UiState.Success(ChapterImages(chapterUrl = "offline://$mangaTitle/ch$chapterNumber", images = files.map { it.absolutePath }))
            } else {
                _chapterImages.value = UiState.Error("No offline pages found")
            }
            _isLoading.value = false
        }
    }

    // Tracking counts for profile stats
    private val _readingCount = MutableStateFlow(0)
    val readingCount: StateFlow<Int> = _readingCount.asStateFlow()

    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount.asStateFlow()

    private val _planningCount = MutableStateFlow(0)
    val planningCount: StateFlow<Int> = _planningCount.asStateFlow()

    private val _onHoldCount = MutableStateFlow(0)
    val onHoldCount: StateFlow<Int> = _onHoldCount.asStateFlow()

    private val _droppedCount = MutableStateFlow(0)
    val droppedCount: StateFlow<Int> = _droppedCount.asStateFlow()

    fun getAvatarUrl(): String? = anilistManager.getAvatarUrl()

    fun loadUserProfile() {
        if (!anilistManager.isLoggedIn()) return
        _isProfileLoading.value = true
        viewModelScope.launch {
            val profileResult = anilistManager.getUserProfile()
            profileResult.onSuccess { _userProfile.value = it }
            profileResult.onFailure { log("PROFILE", "Failed to load profile: ${it.message}") }
            val favResult = anilistManager.getUserFavorites()
            favResult.onSuccess { _userFavorites.value = it }
            favResult.onFailure { log("PROFILE", "Failed to load favorites: ${it.message}") }
            val actResult = anilistManager.getUserActivity()
            actResult.onSuccess { activities ->
                _userActivity.value = activities
                val seenMediaIds = mutableMapOf<String, MutableSet<Int>>()
                for (activity in activities) {
                    val action = activity.statusAction.uppercase()
                    val key = when {
                        action.contains("COMPLETED") -> "COMPLETED"
                        action.contains("READING") || action.contains("REPEATING") || action.contains("READ") -> "CURRENT"
                        action.contains("PLAN") -> "PLANNING"
                        action.contains("PAUSED") || action.contains("HOLD") -> "PAUSED"
                        action.contains("DROPPED") -> "DROPPED"
                        else -> continue
                    }
                    seenMediaIds.getOrPut(key) { mutableSetOf() }.add(activity.mediaId)
                }
                _readingCount.value = seenMediaIds["CURRENT"]?.size ?: 0
                _completedCount.value = seenMediaIds["COMPLETED"]?.size ?: 0
                _planningCount.value = seenMediaIds["PLANNING"]?.size ?: 0
                _onHoldCount.value = seenMediaIds["PAUSED"]?.size ?: 0
                _droppedCount.value = seenMediaIds["DROPPED"]?.size ?: 0
            }
            actResult.onFailure { log("PROFILE", "Failed to load activity: ${it.message}") }
            val statsResult = anilistManager.getUserMangaListStats()
            statsResult.onSuccess { (chapters, manga, score) ->
                val profile = _userProfile.value
                if (profile != null) {
                    _userProfile.value = profile.copy(chaptersRead = chapters, mangaCount = manga, meanScore = score)
                }
            }
            statsResult.onFailure { log("PROFILE", "Failed to load stats: ${it.message}") }
            _isProfileLoading.value = false
        }
    }

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

    // Reader layout: vertical scroll (webtoon) vs paged (LTR/RTL).
    // Exposed as a StateFlow so ReaderScreen recomposes instantly when the user
    // changes the mode either from Settings or from the reader header button.
    private val _readerMode = MutableStateFlow(settingsManager.getReaderMode())
    val readerMode: StateFlow<ReaderMode> = _readerMode.asStateFlow()

    fun setReaderMode(mode: ReaderMode) {
        settingsManager.setReaderMode(mode)
        _readerMode.value = mode
    }

    /** Cycle to the next reader mode — used by the quick-toggle button in the reader header. */
    fun cycleReaderMode() {
        val next = when (_readerMode.value) {
            ReaderMode.VERTICAL_SCROLL -> ReaderMode.LEFT_TO_RIGHT
            ReaderMode.LEFT_TO_RIGHT -> ReaderMode.RIGHT_TO_LEFT
            ReaderMode.RIGHT_TO_LEFT -> ReaderMode.VERTICAL_SCROLL
        }
        setReaderMode(next)
    }

    // Lock reader rotation: when true the reader stays in portrait.
    private val _lockReaderRotation = MutableStateFlow(settingsManager.getLockReaderRotation())
    val lockReaderRotation: StateFlow<Boolean> = _lockReaderRotation.asStateFlow()

    fun setLockReaderRotation(enabled: Boolean) {
        settingsManager.setLockReaderRotation(enabled)
        _lockReaderRotation.value = enabled
    }

    // Show page indicator in reader
    private val _showPageIndicator = MutableStateFlow(settingsManager.getShowPageIndicator())
    val showPageIndicator: StateFlow<Boolean> = _showPageIndicator.asStateFlow()

    fun setShowPageIndicator(enabled: Boolean) {
        settingsManager.setShowPageIndicator(enabled)
        _showPageIndicator.value = enabled
    }

    // Startup screen
    private val _startupScreen = MutableStateFlow(settingsManager.getStartupScreen())
    val startupScreen: StateFlow<String> = _startupScreen.asStateFlow()

    fun setStartupScreen(screen: String) {
        settingsManager.setStartupScreen(screen)
        _startupScreen.value = screen
    }

    // Material 3 dynamic color
    private val _useMaterial3Color = MutableStateFlow(settingsManager.getMaterial3Color())
    val useMaterial3Color: StateFlow<Boolean> = _useMaterial3Color.asStateFlow()

    fun setMaterial3Color(enabled: Boolean) {
        settingsManager.setMaterial3Color(enabled)
        _useMaterial3Color.value = enabled
    }

    // Monochrome theme
    private val _monochromeTheme = MutableStateFlow(settingsManager.getMonochromeTheme())
    val monochromeTheme: StateFlow<Boolean> = _monochromeTheme.asStateFlow()

    fun setMonochromeTheme(enabled: Boolean) {
        settingsManager.setMonochromeTheme(enabled)
        _monochromeTheme.value = enabled
    }

    // Theme mode (system/light/dark/oled)
    private val _themeMode = MutableStateFlow(ThemeMode.fromValue(settingsManager.getThemeMode()))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        settingsManager.setThemeMode(mode.value)
        _themeMode.value = mode
    }

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
        // Pre-load the explore page on app start so it's ready by the time the
        // user swipes to the Explore tab. Previously this only loaded when the
        // user first opened ExploreScreen, causing a visible loading spinner.
        loadExplorePage()

        // Pre-load downloaded manga cache so DownloadsScreen shows data instantly
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { downloadManager.loadScanCache() }
            if (cached != null) {
                _downloadedManga.value = cached
                buildResumeEntries(cached)
            }
        }
    }

    fun setCheckUpdatesOnStart(enabled: Boolean) {
        settingsManager.setCheckUpdatesOnStart(enabled)
        _checkUpdatesOnStart.value = enabled
    }

    fun checkCurrentMangaFavorite() {
        val mediaId = currentMediaId ?: return
        viewModelScope.launch {
            val favs = anilistManager.getUserFavorites()
            favs.onSuccess { list ->
                _currentMangaIsFavorited.value = list.any { it.id == mediaId }
            }
        }
    }

    fun toggleCurrentMangaFavorite() {
        val mediaId = currentMediaId ?: return
        viewModelScope.launch {
            val result = anilistManager.toggleFavorite(mediaId)
            result.onSuccess { isFavorited ->
                _currentMangaIsFavorited.value = isFavorited
            }
            result.onFailure { log("FAVORITE", "Failed to toggle favorite: ${it.message}") }
        }
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
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Update available: ${release.tagName}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
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

        _resumeReading.value = allReading.filter {
            it.scrollProgress > 0f
        }
        _continueReading.value = allReading.filter {
            it.currentChapterNumber > 0
        }
        _planningToRead.value = trackingManager.getPlanningToRead()

        val allTracks = trackingManager.getAllTracking()
        _readingCount.value = allTracks.count { it.status == ReadingStatus.READING }
        _completedCount.value = allTracks.count { it.status == ReadingStatus.COMPLETED }
        _planningCount.value = allTracks.count { it.status == ReadingStatus.PLANNING }
        _onHoldCount.value = allTracks.count { it.status == ReadingStatus.ON_HOLD }
        _droppedCount.value = allTracks.count { it.status == ReadingStatus.DROPPED }
    }

    fun addToPlanning(mangaId: String, title: String, coverUrl: String?, mangaUrl: String, totalChapters: Int) {
        trackingManager.markAsPlanning(mangaId, title, coverUrl, mangaUrl, totalChapters)
        refreshTrackingLists()
        val track = trackingManager.getMangaTracking(mangaId)
        if (track != null) {
            updateAnilistProgressNow(track)
            // If AniList didn't give us a chapter count, ask MangaDex in the
            // background so the home screen card shows the right total.
            if (totalChapters <= 0) refreshMangaDexChapterCountForTrack(track)
        }
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
        if (track != null) {
            updateAnilistProgressNow(track)
            if (totalChapters <= 0) refreshMangaDexChapterCountForTrack(track)
        }
    }

    /**
     * Refresh the MangaDex chapter + volume count for a single track.
     * Fire-and-forget; updates the persisted track and the home screen state
     * when the lookup succeeds.
     */
    fun refreshMangaDexChapterCountForTrack(track: MangaTrack) {
        viewModelScope.launch {
            val mediaId = track.anilistMediaId ?: extractAnilistMediaId(track.mangaId)
            val aggregate = if (mediaId != null) {
                mangaDexManager.fetchAggregateForAniList(track.title, mediaId)
            } else {
                mangaDexManager.fetchAggregateForTitle(track.title)
            }
            if (aggregate != null && aggregate.totalChapters > 0) {
                val updated = track.copy(
                    totalChapters = aggregate.totalChapters,
                    mangaDexId = aggregate.mangaId,
                    mangaDexVolumeCount = aggregate.totalVolumes
                )
                trackingManager.updateTracking(updated)
                refreshTrackingLists()
                Log.d("MANGADEX", "Refreshed single track '${track.title}': ${aggregate.totalChapters} chapters")
            }
        }
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

    fun updateTrackingStatus(mangaId: String, status: ReadingStatus, chapterNumber: Double? = null) {
        val existing = resolveMangaTracking(mangaId)
        if (existing != null) {
            val updated = existing.copy(
                status = status,
                lastReadTimestamp = System.currentTimeMillis(),
                currentChapterNumber = chapterNumber ?: existing.currentChapterNumber,
                currentChapterIndex = if (chapterNumber != null) (chapterNumber - 1).coerceAtLeast(0.0).toInt() else existing.currentChapterIndex
            )
            trackingManager.updateTracking(updated)
            updateAnilistProgressNow(updated)
            // If we don't have a chapter count yet, fetch one from MangaDex so
            // the home screen card shows the right "Ch. X / Y" total.
            if (updated.totalChapters <= 0) refreshMangaDexChapterCountForTrack(updated)
        } else {
            val detail = _mangaDetail.value
            val resolvedChapter = chapterNumber ?: 0.0
            val track = MangaTrack(
                mangaId = mangaId,
                title = currentMangaTitle ?: "",
                coverUrl = currentMangaCoverUrl,
                currentChapterIndex = (resolvedChapter - 1.0).coerceAtLeast(0.0).toInt(),
                currentChapterNumber = resolvedChapter,
                currentChapterUrl = "",
                totalChapters = detail?.chapters ?: 0,
                status = status,
                lastReadTimestamp = System.currentTimeMillis(),
                mangaUrl = currentMangaUrl ?: "https://anilist.co/manga/${currentMediaId ?: ""}",
                anilistMediaId = currentMediaId
            )
            trackingManager.updateTracking(track)
            updateAnilistProgressNow(track)
            if (track.totalChapters <= 0) refreshMangaDexChapterCountForTrack(track)
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
        _mangaTitle.value = manga.title
        currentMangaCoverUrl = manga.coverUrl
        currentMangaUrl = "https://anilist.co/manga/${manga.id}"
        currentMediaId = manga.id
        _mangaDetail.value = null
        clearMangaDexState()
        _isLoading.value = true

        viewModelScope.launch {
            loadAniListDetail(manga.id, manga.coverUrl)
            loadAniListChapters(manga.id)
            loadReadChapters(mangaId)
        }
    }

    fun selectMangaById(mediaId: Int) {
        selectManga(AniListSearchResult(id = mediaId, title = ""))
    }

    fun selectManga(manga: MangaSearchResult) {
        log("SELECT", "Selected from result: ${manga.title}")
        val mediaId = extractMediaIdFromMangaId(manga.mangaId)
        if (mediaId != null) {
            currentMangaId = manga.mangaId
            currentMangaTitle = manga.title
            _mangaTitle.value = manga.title
            currentMangaCoverUrl = manga.coverUrl
            currentMangaUrl = "https://anilist.co/manga/$mediaId"
            currentMediaId = mediaId
            _mangaDetail.value = null
            clearMangaDexState()
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
            _mangaTitle.value = manga.title
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

    /**
     * Build a synthetic chapter list when we have a chapter count but no MangaDex
     * aggregate (e.g. MangaDex lookup failed entirely).
     *
     * These chapters use the legacy `anilist_<mediaId>_ch_<n>` URL scheme and can
     * only be loaded via an installed extension. They exist so the user at least
     * sees a chapter count and can use the "Set Progress" dialog.
     */
    private fun generateChapterList(mediaId: Int, totalChapters: Int): List<ChapterInfo> {
        if (totalChapters <= 0) return emptyList()
        // Oldest-first: chapter 1 at index 0, chapter N at the end.
        return (1..totalChapters).map { i ->
            ChapterInfo(url = "anilist_${mediaId}_ch_$i", title = "Chapter $i")
        }
    }

    /**
     * Resolve the total chapter count for the current manga.
     *
     * Priority:
     *   1. AniList `chapters` field (authoritative when present)
     *   2. MangaDex aggregate total (queried regardless of manga status - completed
     *      manga are also missing chapter counts on AniList surprisingly often)
     *   3. Cached value on the persisted [MangaTrack]
     *
     * Side effect: also populates [_mangaDexChapterCount], [_mangaDexVolumeCount],
     * and [currentMangaDexId] when the MangaDex lookup succeeds, and persists
     * the cache onto the tracking entry so we skip the lookup next time.
     */
    private suspend fun resolveChapterCount(mediaId: Int): Int {
        val detail = _mangaDetail.value
        val mangaId = currentMangaId
        val tracking = mangaId?.let { trackingManager.getMangaTracking(it) }

        // 1. AniList is authoritative when it has a value.
        if (detail?.chapters != null && detail.chapters > 0) return detail.chapters

        // 2. Try MangaDex. Use cached UUID if we have one, otherwise look it up.
        val title = detail?.titleEnglish?.takeIf { it.isNotBlank() }
            ?: detail?.titleRomaji?.takeIf { it.isNotBlank() }
            ?: currentMangaTitle
        if (title != null) {
            val cachedMdId = currentMangaDexId ?: tracking?.mangaDexId
            val aggregate = if (cachedMdId != null) {
                mangaDexManager.fetchAggregate(cachedMdId)
            } else {
                mangaDexManager.fetchAggregateForAniList(title, mediaId)
            }
            if (aggregate != null && aggregate.totalChapters > 0) {
                currentMangaDexId = aggregate.mangaId
                _mangaDexChapterCount.value = aggregate.totalChapters
                _mangaDexVolumeCount.value = aggregate.totalVolumes
                mangaId?.let { mid ->
                    trackingManager.updateTotalChapters(mid, aggregate.totalChapters)
                    cacheMangaDexMetadata(mid, aggregate.mangaId, aggregate.totalVolumes)
                }
                return aggregate.totalChapters
            }
        }

        // 3. Fall back to whatever we have cached locally.
        return tracking?.totalChapters ?: 0
    }

    /**
     * Persist the MangaDex UUID + volume count onto the tracking entry so we can
     * skip the title-search lookup on subsequent opens.
     */
    private fun cacheMangaDexMetadata(mangaId: String, mangaDexId: String, volumeCount: Int) {
        val existing = trackingManager.getMangaTracking(mangaId) ?: return
        if (existing.mangaDexId == mangaDexId && existing.mangaDexVolumeCount == volumeCount) return
        trackingManager.updateTracking(
            existing.copy(
                mangaDexId = mangaDexId,
                mangaDexVolumeCount = volumeCount
            )
        )
    }

    /**
     * Resolve the chapter list for the current manga.
     *
     * Two sources are queried in parallel:
     *   - MangaDex aggregate → latest chapter number + volume count (for the
     *     detail screen's StatsCard and the home screen's "Ch. X / Y" label).
     *   - Extension `/chapters` → the actual chapter list shown in the chapter
     *     selection screen. Each entry becomes a [ChapterInfo] with an
     *     `anilist_<mediaId>_ch_<number>` URL that [loadChapterImages] routes
     *     to the extension for image fetching.
     *
     * Both lists are logged to Logcat (tag "CHAPTERS") so you can compare them.
     *
     * If the extension is not selected or returns nothing, falls back to a
     * synthetic 1..N list from the AniList/MangaDex count (all greyed out).
     *
     * Shared by [loadAniListChapters], [continueFromTracking], [resumeFromTracking],
     * and [continueFromCurrentManga].
     */
    private suspend fun resolveChapterList(mediaId: Int, mangaId: String?): List<ChapterInfo> {
        val detail = _mangaDetail.value
        val tracking = mangaId?.let { trackingManager.getMangaTracking(it) }
        val title = detail?.titleEnglish?.takeIf { it.isNotBlank() }
            ?: detail?.titleRomaji?.takeIf { it.isNotBlank() }
            ?: currentMangaTitle

        log("CHAPTERS", "=== RESOLVE CHAPTER LIST ===")
        log("CHAPTERS", "Manga: '$title' (mediaId=$mediaId, mangaId=$mangaId)")
        log("CHAPTERS", "AniList chapters: ${detail?.chapters}")
        log("CHAPTERS", "AniList volumes: ${detail?.volumes}")
        log("CHAPTERS", "Extension authority: ${_selectedExtensionAuthority.value}")

        // --- 1. Fetch MangaDex aggregate for latest chapter + volume count ---
        var mdLatestChapter: Int? = null
        var mdVolumeCount: Int? = null
        if (title != null) {
            val cachedMdId = currentMangaDexId ?: tracking?.mangaDexId
            val aggregate: MangaDexAggregate? = if (cachedMdId != null) {
                mangaDexManager.fetchAggregate(cachedMdId)
            } else {
                mangaDexManager.fetchAggregateForAniList(title, mediaId)
            }
            if (aggregate != null && aggregate.totalChapters > 0) {
                currentMangaDexId = aggregate.mangaId
                mdLatestChapter = aggregate.totalChapters
                mdVolumeCount = aggregate.totalVolumes
                _mangaDexChapterCount.value = mdLatestChapter
                _mangaDexVolumeCount.value = mdVolumeCount
                if (mangaId != null) {
                    cacheMangaDexMetadata(mangaId, aggregate.mangaId, aggregate.totalVolumes)
                }
                log("CHAPTERS", "=== MANGADEX (count only) ===")
                log("CHAPTERS", "MangaDex latest chapter: $mdLatestChapter")
                log("CHAPTERS", "MangaDex volume count: $mdVolumeCount")
            } else {
                log("CHAPTERS", "MangaDex aggregate was null or empty")
            }
        }

        // --- 2. Fetch the extension's chapter list (the actual list to show) ---
        if (title != null) {
            val extChapters = fetchExtensionChapterList(title)
            if (extChapters != null && extChapters.isNotEmpty()) {
                log("CHAPTERS", "=== EXTENSION CHAPTER LIST (atsu.moe) ===")
                log("CHAPTERS", "Extension returned ${extChapters.size} chapters")

                // Build ChapterInfo list from the extension's response.
                // Oldest-first (index 0 = chapter 1) so read-progress indexing works.
                val result = extChapters
                    .sortedBy { it.index }
                    .map { ch ->
                        val display = ch.number
                        ChapterInfo(
                            url = "anilist_${mediaId}_ch_${ch.number}",
                            title = if (ch.title.isNotBlank()) "Chapter $display: ${ch.title}" else "Chapter $display"
                        )
                    }

                // Use the extension's count for tracking, but prefer MangaDex's
                // volume count if available.
                val totalCount = extChapters.size
                if (mangaId != null) {
                    trackingManager.updateTotalChapters(mangaId, totalCount)
                }

                // === COMPARISON: Log all counts side by side ===
                log("CHAPTERS", "=== COMPARISON ===")
                log("CHAPTERS", "AniList chapters: ${detail?.chapters ?: "null"}")
                log("CHAPTERS", "MangaDex latest chapter: $mdLatestChapter")
                log("CHAPTERS", "MangaDex volumes: $mdVolumeCount")
                log("CHAPTERS", "Extension chapters: $totalCount")
                if (mdLatestChapter != null && mdLatestChapter != totalCount) {
                    log("CHAPTERS", "NOTE: MangaDex latest ($mdLatestChapter) != extension count ($totalCount)")
                }

                log("CHAPTERS", "=== FINAL CHAPTER LIST (from extension) ===")
                log("CHAPTERS", "Total entries: ${result.size}")
                log("CHAPTERS", "First 5: ${result.take(5).map { it.title }}")
                log("CHAPTERS", "Last 5: ${result.takeLast(5).map { it.title }}")
                return result
            } else {
                log("CHAPTERS", "Extension returned no chapters (not selected or manga not found)")
            }
        }

        // --- 3. Fallback: synthetic chapter list from the best available count ---
        // Prefer AniList, then MangaDex latest, then cached tracking value.
        val fallbackTotal = detail?.chapters
            ?: mdLatestChapter
            ?: tracking?.totalChapters ?: 0
        log("CHAPTERS", "=== FALLBACK: synthetic chapter list (count=$fallbackTotal) ===")
        if (mangaId != null) {
            trackingManager.updateTotalChapters(mangaId, fallbackTotal)
        }
        return generateChapterList(mediaId, fallbackTotal)
    }

    /**
     * Load chapters for the current manga into [_chapters].
     *
     * Delegates to [resolveChapterList] which prefers real MangaDex chapter UUIDs
     * (loadable via the at-home server without an extension) and falls back to a
     * synthetic list when MangaDex is unavailable.
     */
    private suspend fun loadAniListChapters(mediaId: Int) {
        val mangaId = currentMangaId
        val chapterList = resolveChapterList(mediaId, mangaId)
        _chapters.value = chapterList
        log("CHAPTERS", "Loaded ${chapterList.size} chapters for media $mediaId " +
            "(mangadex=${chapterList.firstOrNull()?.url?.startsWith("mangadex:") == true})")
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
        _mangaTitle.value = track.title
        currentMangaCoverUrl = track.coverUrl
        currentMangaUrl = track.mangaUrl
        // Seed the MangaDex UUID cache from the persisted track so we skip the
        // title-search lookup if we already resolved it on a previous open.
        currentMangaDexId = track.mangaDexId
        track.mangaDexVolumeCount?.let { _mangaDexVolumeCount.value = it }
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
            val chapterList = resolveChapterList(mediaId, track.mangaId)
            _chapters.value = chapterList
            trackingManager.updateTotalChapters(track.mangaId, totalChapters)
            refreshTrackingLists()

            val savedUrl = track.currentChapterUrl
            var currentPosition = -1

            if (savedUrl.isNotBlank()) {
                currentPosition = chapterList.indexOfFirst { it.url == savedUrl }
            }

            if (currentPosition < 0 && track.currentChapterNumber > 0) {
                currentPosition = findChapterByNumber(chapterList, track.currentChapterNumber)
            }

            if (currentPosition < 0) {
                currentPosition = track.currentChapterIndex.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            }

            val safeChapterIndex = currentPosition.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            val nextToRead = safeChapterIndex + 1

            _readChapterIndices.value = (0 until safeChapterIndex).toSet()
            _nextChapterToRead.value = nextToRead

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
        _mangaTitle.value = track.title
        currentMangaCoverUrl = track.coverUrl
        currentMangaUrl = track.mangaUrl
        currentMangaDexId = track.mangaDexId
        track.mangaDexVolumeCount?.let { _mangaDexVolumeCount.value = it }
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
            val chapterList = resolveChapterList(mediaId, track.mangaId)
            _chapters.value = chapterList
            trackingManager.updateTotalChapters(track.mangaId, totalChapters)

            val savedUrl = track.currentChapterUrl
            var currentPosition = -1

            if (savedUrl.isNotBlank()) {
                currentPosition = chapterList.indexOfFirst { it.url == savedUrl }
            }

            if (currentPosition < 0 && track.currentChapterNumber > 0) {
                currentPosition = findChapterByNumber(chapterList, track.currentChapterNumber)
            }

            if (currentPosition < 0) {
                currentPosition = track.currentChapterIndex.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            }

            val safeIndex = currentPosition.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            val nextIndex = (safeIndex + 1).coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            val chapter = chapterList.getOrNull(nextIndex)
            if (chapter != null) {
                _selectedChapterIndex.value = nextIndex
                _readChapterIndices.value = (0 until nextIndex).toSet()
                _nextChapterToRead.value = nextIndex
                _isChapterRead.value = false
                _isLoading.value = true
                loadChapterImages(chapter.url)
                onReady()
            }
        }
    }

    fun continueFromCurrentManga() {
        val mangaDetail = _mangaDetail.value ?: return
        val mediaId = currentMediaId ?: return
        val mangaId = currentMangaId ?: return
        val mangaUrl = currentMangaUrl ?: return

        _selectedChapterIndex.value = -1
        _isLoading.value = true
        _chapterImages.value = UiState.Idle

        val tracking = trackingManager.getMangaTracking(mangaId)

        fun resolveNextIndex(chapterList: List<ChapterInfo>): Int {
            if (chapterList.isEmpty()) return 0
            if (tracking == null || tracking.status != ReadingStatus.READING) return 0

            val savedUrl = tracking.currentChapterUrl
            var currentPosition = -1

            if (savedUrl.isNotBlank()) {
                currentPosition = chapterList.indexOfFirst { it.url == savedUrl }
            }

            if (currentPosition < 0 && tracking.currentChapterNumber > 0) {
                currentPosition = findChapterByNumber(chapterList, tracking.currentChapterNumber)
            }

            if (currentPosition < 0) {
                currentPosition = tracking.currentChapterIndex.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            }

            return if (tracking.scrollProgress == 0f && currentPosition >= 0) {
                currentPosition + 1
            } else if (currentPosition > 0) {
                currentPosition + 1
            } else {
                0
            }
        }

        val chapterList = _chapters.value
        if (chapterList.isNotEmpty()) {
            val nextChapterIndex = resolveNextIndex(chapterList)
            val safeChapterIndex = nextChapterIndex.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
            _readChapterIndices.value = (0 until safeChapterIndex).toSet()
            _nextChapterToRead.value = safeChapterIndex

            val chapter = chapterList.getOrNull(safeChapterIndex)
            if (chapter != null) {
                _selectedChapterIndex.value = safeChapterIndex
                _isChapterRead.value = safeChapterIndex > 0
                loadChapterImages(chapter.url)
            } else {
                _isLoading.value = false
            }
            return
        }

        viewModelScope.launch {
            val totalChapters = resolveChapterCount(mediaId)
            val chapters = resolveChapterList(mediaId, mangaId)
            _chapters.value = chapters

            val nextChapterIndex = resolveNextIndex(chapters)
            val safeChapterIndex = nextChapterIndex.coerceIn(0, chapters.lastIndex.coerceAtLeast(0))
            _readChapterIndices.value = (0 until safeChapterIndex).toSet()
            _nextChapterToRead.value = safeChapterIndex

            val chapter = chapters.getOrNull(safeChapterIndex)
            if (chapter != null) {
                _selectedChapterIndex.value = safeChapterIndex
                _isChapterRead.value = safeChapterIndex > 0
                loadChapterImages(chapter.url)
            }
        }
    }

    fun startReading() {
        _mangaDetail.value?.let { detail ->
            val mediaId = currentMediaId ?: return
            val mangaId = "anilist_$mediaId"
            currentMangaId = mangaId
            currentMangaTitle = detail.titleRomaji
            _mangaTitle.value = detail.titleRomaji
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
            // resolveChapterList already calls updateTotalChapters + cacheMangaDexMetadata
            // as side effects, so we don't need to duplicate that here.
            val chapterList = resolveChapterList(mediaId, mangaId)
            _chapters.value = chapterList

            mangaId?.let { refreshTrackingLists() }

            _isLoading.value = false

            recalculateNextChapterFromTracking(chapterList)

            val savedIndex = _selectedChapterIndex.value
            if (savedIndex > 0 && savedIndex < chapterList.size) {
                selectChapter(savedIndex)
            } else if (savedIndex == 0) {
                selectChapter(0)
            }
        }
    }

    /**
     * Recalculate [_nextChapterToRead] and [_readChapterIndices] from the
     * persisted tracking data so they stay correct when the chapter list
     * changes (e.g. after switching the default extension).
     *
     * Uses chapter-number matching (not index) so the result is extension-
     * agnostic.
     */
    private fun recalculateNextChapterFromTracking(chapterList: List<ChapterInfo>) {
        val mangaId = currentMangaId ?: return
        val tracking = trackingManager.getMangaTracking(mangaId) ?: return

        val savedUrl = tracking.currentChapterUrl
        var currentPosition = -1

        if (savedUrl.isNotBlank()) {
            currentPosition = chapterList.indexOfFirst { it.url == savedUrl }
        }

        if (currentPosition < 0 && tracking.currentChapterNumber > 0) {
            currentPosition = findChapterByNumber(chapterList, tracking.currentChapterNumber)
        }

        if (currentPosition < 0) {
            currentPosition = tracking.currentChapterIndex.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
        }

        val safeIndex = currentPosition.coerceIn(0, chapterList.lastIndex.coerceAtLeast(0))
        _nextChapterToRead.value = safeIndex + 1
        _readChapterIndices.value = (0 until safeIndex).toSet()
    }

    // ======================== Chapter Selection & Reading ========================

    private fun selectChapterFromIndex(index: Int) {
        _selectedChapterIndex.value = index
        _isChapterRead.value = false
        _isLoading.value = true
        val chapter = _chapters.value.getOrNull(index)

        if (_isOfflineMode.value) {
            val url = chapter?.url ?: ""
            val slug = url.substringAfter("offline://").substringBefore("/chapter-")
            val chNumStr = url.substringAfter("chapter-").toDoubleOrNull() ?: 0.0
            val mangaTitle = currentMangaTitle ?: slug
            viewModelScope.launch {
                val files = withContext(Dispatchers.IO) {
                    downloadManager.getDownloadedPagesByMangaAndChapter(mangaTitle, chNumStr)
                }
                if (files.isNotEmpty()) {
                    _chapterImages.value = UiState.Success(ChapterImages(chapterUrl = url, images = files.map { it.absolutePath }))
                } else {
                    _chapterImages.value = UiState.Error("No offline pages found")
                }
                _isLoading.value = false
            }
            return
        }

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
            recalculateNextChapterFromTracking(_chapters.value)
            _isLoading.value = false
        }
    }

    // ======================== Scroll Progress & Tracking ========================

    /**
     * Detect whether a chapter title represents a partial chapter (e.g. "Chapter 58.5").
     */
    private fun isPartialChapter(chapter: ChapterInfo): Boolean {
        val title = chapter.title ?: return false
        val numStr = title.removePrefix("Chapter ").removePrefix("Ch. ").substringBefore(":").trim()
        return numStr.contains(".")
    }

    /**
     * Parse the chapter number from a [ChapterInfo] title as a [Double].
     *
     * Examples: "Chapter 58" → 58.0, "Chapter 58.5: Title" → 58.5, "Ch. 102" → 102.0
     * Returns 0.0 if parsing fails.
     */
    private fun parseChapterNumberDouble(chapter: ChapterInfo): Double {
        val title = chapter.title ?: return 0.0
        val numStr = title.removePrefix("Chapter ").removePrefix("Ch. ").substringBefore(":").trim()
        return numStr.toDoubleOrNull() ?: 0.0
    }

    /**
     * Find the index of a chapter in [chapters] whose parsed number matches [target].
     *
     * Strategy:
     *  1. Exact match (target == parsed number)
     *  2. Floor match (closest chapter with number ≤ target, preferring non-partial)
     *  3. -1 if nothing found
     *
     * This handles extension switching gracefully: if the new extension lacks
     * partial chapters (e.g. no 58.5), it falls back to the nearest full chapter (58.0).
     */
    private fun findChapterByNumber(chapters: List<ChapterInfo>, target: Double): Int {
        if (chapters.isEmpty() || target <= 0.0) return -1

        // Exact match
        val exactIndex = chapters.indexOfFirst { parseChapterNumberDouble(it) == target }
        if (exactIndex >= 0) return exactIndex

        // Floor match: closest chapter with number ≤ target, preferring non-partial
        var bestIndex = -1
        var bestNumber = Double.NEGATIVE_INFINITY
        for (i in chapters.indices) {
            val num = parseChapterNumberDouble(chapters[i])
            if (num <= target && num > bestNumber) {
                bestNumber = num
                bestIndex = i
            }
        }
        return bestIndex
    }

    /**
     * Compute the AniList chapter number for a given chapter index.
     * AniList number = position in the list (1-based).
     */
    private fun computeAnilistChapterNumber(index: Int): Int {
        return index + 1
    }

    fun onChapterScrollProgress(scrollPercent: Float) {
        val threshold = _anilistSyncThreshold.value / 100f
        if (_selectedChapterIndex.value < 0) return
        if (!scrollPercent.isFinite()) return

        // Offline mode: save page index to .lastread for resume
        if (_isOfflineMode.value) {
            val mangaSlug = currentOfflineMangaSlug ?: return
            val chapter = _chapters.value.getOrNull(_selectedChapterIndex.value) ?: return
            val chNumStr = chapter.url.substringAfter("chapter-").toDoubleOrNull() ?: return

            // Only update when page actually changes
            if (currentOfflinePageIndex != lastSavedOfflinePage) {
                lastSavedOfflinePage = currentOfflinePageIndex
                downloadManager.saveLastReadChapter(mangaSlug, chNumStr, currentOfflinePageIndex)

                // Update resume entry immediately so the UI reflects progress without a full scan
                val totalChapters = _chapters.value.size
                val manga = _downloadedManga.value.find { it.slug == mangaSlug }
                val chapterPages = manga?.chapters?.find { it.chapterNumber == chNumStr }?.pageCount ?: 1
                val currentEntry = _downloadedResumeReading.value.find { it.slug == mangaSlug }
                val scrollProgress = if (chapterPages > 0) currentOfflinePageIndex.toFloat() / chapterPages.toFloat() else 0f
                val updatedEntry = DownloadedResumeEntry(
                    title = currentMangaTitle ?: mangaSlug,
                    slug = mangaSlug,
                    coverPath = currentEntry?.coverPath ?: manga?.coverPath,
                    currentChapter = chNumStr,
                    totalChapters = totalChapters,
                    mangaId = null,
                    scrollProgress = scrollProgress
                )
                _downloadedResumeReading.value = (_downloadedResumeReading.value.filter { it.slug != mangaSlug } + updatedEntry)
                    .sortedByDescending { it.currentChapter }
            }
            return
        }

        currentMangaId?.let { mangaId ->
            val chapter = _chapters.value.getOrNull(_selectedChapterIndex.value)
            if (chapter != null) {
                val chapterNumber = parseChapterNumberDouble(chapter)
                val anilistChapterNumber = computeAnilistChapterNumber(_selectedChapterIndex.value)
                val existing = trackingManager.getMangaTracking(mangaId)

                if (existing == null) {
                    if (scrollPercent <= 0f) return@let
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
                    if (_selectedChapterIndex.value !in _readChapterIndices.value) {
                        trackingManager.updateScrollProgress(mangaId, scrollPercent)
                    }
                }

                if (scrollPercent >= threshold) {
                    val alreadyRead = _selectedChapterIndex.value in _readChapterIndices.value
                    val partial = isPartialChapter(chapter)

                    if (!alreadyRead) {
                        val trackForUpdate = trackingManager.getMangaTracking(mangaId)
                        if (trackForUpdate == null || trackForUpdate.currentChapterIndex != _selectedChapterIndex.value || trackForUpdate.currentChapterNumber < 0) {
                            trackingManager.updateChapterProgress(mangaId, _selectedChapterIndex.value, chapterNumber, chapter.url)
                            log("TRACK", "Updated to chapter ${_selectedChapterIndex.value} (num=$chapterNumber)")
                        } else {
                            trackingManager.resetScrollProgress(mangaId)
                        }
                        _readChapterIndices.value = _readChapterIndices.value + _selectedChapterIndex.value

                        if (!partial) {
                            val tracking = trackingManager.getMangaTracking(mangaId)
                            if (tracking != null) {
                                scheduleAnilistProgressUpdate(tracking)
                            }
                        } else {
                            log("TRACK", "Skipping AniList sync for partial chapter $chapterNumber")
                        }
                    }

                    _isChapterRead.value = true
                    _nextChapterToRead.value = (_nextChapterToRead.value ?: 0).coerceAtLeast(_selectedChapterIndex.value + 1)
                }

                refreshTrackingLists()
            }
        }
    }

    fun updateCurrentChapter() {
        currentMangaId?.let { mangaId ->
            val chapter = _chapters.value.getOrNull(_selectedChapterIndex.value)
            if (chapter != null) {
                val chapterNumber = parseChapterNumberDouble(chapter)
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
        val safeProgress = maxOf(track.currentChapterIndex + 1, 0)
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
        _isOfflineMode.value = false
        currentOfflineMangaSlug = null
        refreshTrackingLists()
    }

    /**
     * Reset all MangaDex-derived state for the current manga. Called when the user
     * navigates away from the manga detail screen so the next manga starts fresh.
     */
    private fun clearMangaDexState() {
        currentMangaDexId = null
        _mangaDexChapterCount.value = null
        _mangaDexVolumeCount.value = null
    }

    // ======================== Downloads ========================

    fun downloadChapter(
        mangaTitle: String,
        mangaId: String,
        chapterNumber: Double,
        chapterUrl: String,
        imageUrls: List<String>
    ) {
        downloadManager.startDownload(
            mangaTitle = mangaTitle,
            mangaId = mangaId,
            chapterNumber = chapterNumber,
            chapterTitle = "Chapter $chapterNumber",
            chapterUrl = chapterUrl,
            imageUrls = imageUrls
        )
    }

    fun downloadSelectedChapters(
        mangaTitle: String,
        mangaId: String,
        chapters: List<Triple<Double, String, List<String>>>
    ) {
        downloadManager.startBatchDownload(mangaTitle, mangaId, chapters)
    }

    fun cancelDownload(taskId: String) = downloadManager.cancelDownload(taskId)
    fun removeDownload(taskId: String) = downloadManager.removeTask(taskId)
    fun getDownloadDirectory(): String = downloadManager.getDownloadDirectory()
    fun setDownloadDirectory(path: String) = downloadManager.setDownloadDirectory(path)
    fun getDownloadedChapterIds(mangaId: String): Set<String> = downloadManager.getDownloadedChapterIds(mangaId)

    fun deleteManga(mangaSlug: String) {
        downloadManager.deleteManga(mangaSlug)
        scanDownloadedManga()
    }

    fun deleteChapter(mangaSlug: String, chapterNumber: Double) {
        downloadManager.deleteChapter(mangaSlug, chapterNumber)
        scanDownloadedManga()
    }

    fun moveDownloadsToDirectory(newDir: String, onDone: () -> Unit = {}) {
        downloadManager.moveDownloadsToDirectory(newDir, onDone)
    }

    /**
     * Fetch page images for a chapter via the selected extension (for downloads).
     * Runs the ContentProvider query on the IO dispatcher.
     */
    fun fetchChapterImagesForDownload(
        mangaTitle: String,
        chapterNumber: String,
        onResult: (Result<List<String>>) -> Unit
    ) {
        val authority = _selectedExtensionAuthority.value
        if (authority == null) {
            onResult(Result.failure(Exception("No extension selected")))
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchImagesFromExtension(mangaTitle, chapterNumber, authority)
            }
            onResult(result)
        }
    }

    // ======================== Image Loading (Extensions) ========================

    /**
     * Fetch the full chapter list from the user's selected extension.
     *
     * Calls the extension's `/chapters` ContentProvider path (added in
     * atsumaru-extension v2). The extension searches atsu.moe for the manga,
     * calls /api/manga/info, and returns the full chapters array with
     * {number, title, id, index, pageCount} for each chapter.
     *
     * This REPLACES the MangaDex aggregate for chapter list building. MangaDex
     * is no longer used at all — the extension (atsu.moe) is the sole source
     * of truth for which chapters exist.
     *
     * Returns null if no extension is selected or the call fails.
     */
    private suspend fun fetchExtensionChapterList(mangaTitle: String): List<ExtensionChapter>? {
        val authority = _selectedExtensionAuthority.value ?: return null
        if (mangaTitle.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse("content://$authority/chapters")
                    .buildUpon()
                    .appendQueryParameter("manga", mangaTitle)
                    .appendQueryParameter("anime", mangaTitle)
                    .build()
                log("EXT", "=== FETCH CHAPTER LIST: mangaTitle='$mangaTitle' authority='$authority' ===")
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor == null) {
                    log("EXT", "Extension returned null cursor")
                    return@withContext null
                }
                cursor.use { c ->
                    if (!c.moveToFirst()) {
                        log("EXT", "Extension returned no data")
                        return@withContext null
                    }
                    val col = c.getColumnIndex("data")
                    if (col == -1) {
                        log("EXT", "Missing 'data' column")
                        return@withContext null
                    }
                    val jsonData = c.getString(col)
                    log("EXT", "Extension raw response (first 500 chars): ${jsonData.take(500)}")
                    val json = JSONObject(jsonData)
                    if (json.has("error")) {
                        log("EXT", "Extension error: ${json.getString("error")}")
                        return@withContext null
                    }
                    val chaptersArr = json.optJSONArray("chapters") ?: return@withContext null
                    val total = json.optInt("totalChapters", chaptersArr.length())
                    val chapters = mutableListOf<ExtensionChapter>()
                    for (i in 0 until chaptersArr.length()) {
                        val ch = chaptersArr.optJSONObject(i) ?: continue
                        val number = ch.optString("number", "")
                        val title = ch.optString("title", "")
                        val id = ch.optString("id", "")
                        val index = ch.optInt("index", i)
                        val pageCount = ch.optInt("pageCount", 0)
                        chapters.add(ExtensionChapter(number, title, id, index, pageCount))
                    }
                    log("EXT", "=== EXTENSION CHAPTER LIST ===")
                    log("EXT", "totalChapters: $total")
                    log("EXT", "chapters returned: ${chapters.size}")
                    if (chapters.isNotEmpty()) {
                        log("EXT", "First 5: ${chapters.take(5).map { "${it.number} - ${it.title}" }}")
                        log("EXT", "Last 5: ${chapters.takeLast(5).map { "${it.number} - ${it.title}" }}")
                    }
                    chapters
                }
            } catch (e: Exception) {
                log("EXT", "fetchExtensionChapterList failed for '$mangaTitle': ${e.message}")
                null
            }
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

        // Short-circuit: chapter that neither MangaDex nor the extension can
        // provide (URL is `mangadex:unavailable:<n>`). These exist in the list
        // so the user sees a complete 1..N chapter list, but they can't be read.
        if (chapterUrl.startsWith("mangadex:unavailable:")) {
            val chapterNum = chapterUrl.removePrefix("mangadex:unavailable:")
            _chapterImages.value = UiState.Error(
                "Chapter $chapterNum is not available. " +
                "MangaDex doesn't have it (likely removed due to licensing) and " +
                "no extension provides it. Select an extension in Settings that " +
                "has this manga."
            )
            _isLoading.value = false
            return
        }

        if (_selectedExtensionAuthority.value == null) {
            _chapterImages.value = UiState.Error(
                "No extension selected. Install and select an extension in Settings."
            )
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _chapterImages.value = UiState.Loading

            // All chapter images are loaded via the user's selected extension.
            val authority = _selectedExtensionAuthority.value ?: run {
                _chapterImages.value = UiState.Error(
                    "No extension selected. Install and select an extension in Settings."
                )
                _isLoading.value = false
                return@launch
            }
            val chapter = _chapters.value.getOrNull(_selectedChapterIndex.value)
            val title = chapter?.title ?: ""
            // Extract just the chapter number from the title.
            // Title format: "Chapter 346" or "Chapter 346.2: Some Title"
            // → chapterParam = "346" or "346.2"
            val chapterParam = title.removePrefix("Chapter ").substringBefore(":").trim()
            val mangaTitle = currentMangaTitle
                ?: _mangaDetail.value?.titleEnglish
                ?: _mangaDetail.value?.titleRomaji
                ?: ""
            if (mangaTitle.isNotBlank() && chapterParam.isNotBlank()) {
                log("LOAD", "Fetching via extension: title='$title' -> chapterParam='$chapterParam' mangaTitle='$mangaTitle'")
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

            _chapterImages.value = UiState.Error("Failed to load chapter images")
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
                onFailure = {
                    Log.e("ANILIST", "Sync failed: ${it.message}")
                    _isAniListSyncing.value = false
                    return@launch
                }
            )
            val entries = anilistManager.getSyncedManga()
            anilistManager.createTrackingEntries(entries, trackingManager)
            refreshTrackingLists()
            _anilistUsername.value = anilistManager.getLoggedInUser()
            Log.d("ANILIST", "Synced ${entries.size} manga entries")
            _isAniListSyncing.value = false

            // After the AniList sync has populated tracking entries, kick off a
            // background MangaDex chapter-count refresh so the home screen cards
            // show correct "Ch. X / Y" totals. This is fire-and-forget — each
            // track is updated independently and the home screen re-collects on
            // every refreshTrackingLists() call.
            refreshMangaDexChapterCountsForAllTracks()
        }
    }

    /**
     * Background-refresh MangaDex chapter + volume counts for every tracking
     * entry that is missing them (or that AniList reported as null/0).
     *
     * Runs on a coroutine so the caller doesn't have to be suspending. Iterates
     * sequentially to avoid hammering the MangaDex API with N parallel requests
     * (which would risk rate-limiting). Each successful lookup is cached on the
     * [MangaTrack] so subsequent syncs skip the title search.
     *
     * Called automatically from [syncAnilistManga] and safe to call manually.
     */
    fun refreshMangaDexChapterCountsForAllTracks() {
        viewModelScope.launch {
            val tracks = trackingManager.getAllTracking()
            Log.d("MANGADEX", "Refreshing chapter counts for ${tracks.size} tracked manga")
            var refreshed = 0
            for (track in tracks) {
                // Skip tracks that already have a cached MangaDex ID AND a positive
                // totalChapters value — no need to re-query MangaDex for those.
                if (track.mangaDexId != null && track.totalChapters > 0) continue

                val mediaId = track.anilistMediaId ?: extractAnilistMediaId(track.mangaId)
                val aggregate = if (mediaId != null) {
                    mangaDexManager.fetchAggregateForAniList(track.title, mediaId)
                } else {
                    mangaDexManager.fetchAggregateForTitle(track.title)
                }

                if (aggregate != null && aggregate.totalChapters > 0) {
                    val updated = track.copy(
                        totalChapters = aggregate.totalChapters,
                        mangaDexId = aggregate.mangaId,
                        mangaDexVolumeCount = aggregate.totalVolumes
                    )
                    trackingManager.updateTracking(updated)
                    refreshed++
                    Log.d("MANGADEX", "Refreshed '${track.title}': " +
                        "${aggregate.totalChapters} chapters, ${aggregate.totalVolumes} volumes")
                    // Update the home screen as each track comes in, rather than
                    // waiting for the entire batch to finish.
                    refreshTrackingLists()
                }
            }
            Log.d("MANGADEX", "Chapter-count refresh complete: $refreshed/${tracks.size} updated")
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
                    anilistManager.updateMediaListEntry(mediaId, track.currentChapterIndex + 1, anilistStatus)
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

    fun setManualChapterProgress(chapterNumber: Double) {
        val mangaId = currentMangaId ?: return
        val totalChs = _chapters.value.size.coerceAtLeast(_mangaDetail.value?.chapters ?: 0)
            .let { if (it <= 0) Int.MAX_VALUE else it }
        val clamped = chapterNumber.coerceAtMost(totalChs.toDouble())
        val existing = trackingManager.getMangaTracking(mangaId)
        val chapterIndex = (clamped - 1.0).coerceAtLeast(0.0).toInt()
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

    fun updateOfflinePageIndex(pageIndex: Int) {
        currentOfflinePageIndex = pageIndex
    }

    fun clearResumeProgress(mangaId: String) {
        trackingManager.removeTracking(mangaId)
        refreshTrackingLists()
    }

    fun clearMangaDetail() {
        _mangaDetail.value = null
        clearMangaDexState()
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
