package com.blissless.oni.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class DownloadedManga(
    val title: String,
    val slug: String,
    val coverPath: String?,
    val chapters: List<DownloadedChapter>,
    val lastReadChapter: Double? = null
)

data class DownloadedChapter(
    val chapterNumber: Double,
    val chapterDir: File,
    val pageCount: Int
)

data class DownloadTask(
    val id: String,
    val mangaTitle: String,
    val mangaId: String,
    val chapterNumber: Double,
    val chapterTitle: String,
    val status: DownloadStatus,
    val totalPages: Int,
    val downloadedPages: Int,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED, PAUSED
}

enum class ChapterDownloadResult {
    PENDING, DOWNLOADING, COMPLETED, FAILED, RETRYING
}

data class BatchDownloadState(
    val mangaTitle: String,
    val totalChapters: Int,
    val completedChapters: Int = 0,
    val failedChapters: Int = 0,
    val currentIndex: Int = -1,
    val isComplete: Boolean = false,
    val currentChapterNumber: Double? = null,
    val attempts: Int = 1,
    val completedNumbers: Set<Double> = emptySet(),
    val failedNumbers: Set<Double> = emptySet()
)

class DownloadManager(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("Oni_downloads", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val scanCacheFile: File by lazy {
        File(context.filesDir, "scan_cache.json")
    }

    private data class CachedManga(
        val title: String,
        val slug: String,
        val coverPath: String?,
        val chapterNumbers: List<Double>,
        val chapterPageCounts: Map<Double, Int>,
        val lastReadChapter: Double?
    )

    fun saveScanCache(manga: List<DownloadedManga>) {
        try {
            val cached = manga.map { m ->
                CachedManga(
                    title = m.title,
                    slug = m.slug,
                    coverPath = m.coverPath,
                    chapterNumbers = m.chapters.map { it.chapterNumber },
                    chapterPageCounts = m.chapters.associate { it.chapterNumber to it.pageCount },
                    lastReadChapter = m.lastReadChapter
                )
            }
            scanCacheFile.writeText(gson.toJson(cached))
        } catch (_: Exception) {}
    }

    fun loadScanCache(): List<DownloadedManga>? {
        return try {
            if (!scanCacheFile.exists()) return null
            val type = object : TypeToken<List<CachedManga>>() {}.type
            val cached: List<CachedManga> = gson.fromJson(scanCacheFile.readText(), type) ?: return null
            val baseDir = File(getDownloadDirectory())
            cached.mapNotNull { c ->
                val chapterDirs = c.chapterNumbers.mapNotNull { num ->
                    val chDir = File(baseDir, "${c.slug}/chapter-$num")
                    if (chDir.isDirectory) num to chDir else null
                }
                if (chapterDirs.isEmpty()) return@mapNotNull null
                val chapters = chapterDirs.map { (num, dir) ->
                    DownloadedChapter(num, dir, c.chapterPageCounts[num] ?: 0)
                }.filter { it.pageCount > 0 }.sortedBy { it.chapterNumber }
                if (chapters.isEmpty()) return@mapNotNull null
                DownloadedManga(c.title, c.slug, c.coverPath, chapters, c.lastReadChapter)
            }
        } catch (_: Exception) { null }
    }

    private val _activeDownloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    val activeDownloads: StateFlow<List<DownloadTask>> = _activeDownloads.asStateFlow()

    private val _batchDownloadState = MutableStateFlow<BatchDownloadState?>(null)
    val batchDownloadState: StateFlow<BatchDownloadState?> = _batchDownloadState.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var notificationIdCounter = 2000

    companion object {
        private const val KEY_DOWNLOADS = "download_tasks"
        private const val TAG = "DownloadManager"
        private const val CHANNEL_ID = "manga_downloads"
        private const val MAX_RETRIES = 2
        private const val BATCH_NOTIFICATION_ID = 9999
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        try {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Manga Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Manga chapter download progress"
            }
            notificationManager.createNotificationChannel(channel)
        } catch (_: Exception) {}
    }

    private fun showDownloadNotification(taskId: String, title: String, progress: Int, total: Int) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val percent = if (total > 0) (progress * 100 / total) else 0
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("$progress/$total pages")
            .setProgress(total, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        val nid = taskId.hashCode() + notificationIdCounter
        notificationManager.notify(nid, notification)
    }

    private fun showDownloadCompleteNotification(title: String) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(title.hashCode() + notificationIdCounter, notification)
    }

    private fun dismissDownloadNotification(taskId: String) {
        notificationManager.cancel(taskId.hashCode() + notificationIdCounter)
    }

    private fun showBatchStartNotification(title: String, count: Int) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $count chapters")
            .setContentText(title)
            .setProgress(count, 0, false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(BATCH_NOTIFICATION_ID, notification)
    }

    private fun showBatchProgressNotification(title: String, current: Int, total: Int, completed: Int, hasFailed: Boolean) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val failedText = if (hasFailed) ", some failed" else ""
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading chapter $current/$total")
            .setContentText("$title - $completed done$failedText")
            .setProgress(total, current - 1, false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(BATCH_NOTIFICATION_ID, notification)
    }

    private fun showBatchCompleteNotification(title: String, completed: Int, failed: Int, total: Int) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val text = when {
            failed == 0 -> "All $total chapters downloaded"
            else -> "$completed/$total downloaded, $failed failed"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(BATCH_NOTIFICATION_ID, notification)
    }

    fun getDownloadDirectory(): String {
        return settingsManager.getDownloadDirectory(context)
    }

    fun setDownloadDirectory(path: String) {
        settingsManager.setDownloadDirectory(path)
    }

    fun startDownload(
        mangaTitle: String,
        mangaId: String,
        chapterNumber: Double,
        chapterTitle: String,
        chapterUrl: String,
        imageUrls: List<String>
    ) {
        val taskId = "${mangaId}_ch_${chapterNumber}"
        val existing = _activeDownloads.value.find { it.id == taskId }
        if (existing != null && (existing.status == DownloadStatus.DOWNLOADING || existing.status == DownloadStatus.PENDING)) {
            return
        }

        val task = DownloadTask(
            id = taskId,
            mangaTitle = mangaTitle,
            mangaId = mangaId,
            chapterNumber = chapterNumber,
            chapterTitle = chapterTitle,
            status = DownloadStatus.PENDING,
            totalPages = imageUrls.size,
            downloadedPages = 0
        )

        updateTask(task)

        val job = scope.launch {
            downloadChapter(task, imageUrls)
        }
        downloadJobs[taskId] = job
    }

    fun startBatchDownload(
        mangaTitle: String,
        mangaId: String,
        chapters: List<Triple<Double, String, List<String>>>
    ) {
        if (chapters.isEmpty()) return

        downloadJobs["batch"]?.cancel()

        val job = scope.launch {
            var completed = 0
            var failed = 0
            val completedNums = mutableSetOf<Double>()
            val failedNums = mutableSetOf<Double>()

            showBatchStartNotification(mangaTitle, chapters.size)

            for ((index, triple) in chapters.withIndex()) {
                val (chapterNumber, chapterUrl, imageUrls) = triple
                val taskId = "${mangaId}_ch_${chapterNumber}"

                val existing = _activeDownloads.value.find { it.id == taskId }
                if (existing != null && (existing.status == DownloadStatus.DOWNLOADING || existing.status == DownloadStatus.PENDING)) {
                    continue
                }

                _batchDownloadState.value = BatchDownloadState(
                    mangaTitle = mangaTitle,
                    totalChapters = chapters.size,
                    completedChapters = completed,
                    failedChapters = failed,
                    currentIndex = index,
                    isComplete = false,
                    currentChapterNumber = chapterNumber,
                    attempts = 1,
                    completedNumbers = completedNums.toSet(),
                    failedNumbers = failedNums.toSet()
                )

                showBatchProgressNotification(mangaTitle, index + 1, chapters.size, completed, false)

                val task = DownloadTask(
                    id = taskId,
                    mangaTitle = mangaTitle,
                    mangaId = mangaId,
                    chapterNumber = chapterNumber,
                    chapterTitle = "Chapter $chapterNumber",
                    status = DownloadStatus.PENDING,
                    totalPages = imageUrls.size,
                    downloadedPages = 0
                )
                updateTask(task)

                downloadChapter(task, imageUrls, showNotification = false)

                val finalTask = _activeDownloads.value.find { it.id == taskId }
                if (finalTask?.status == DownloadStatus.COMPLETED) {
                    completed++
                    completedNums.add(chapterNumber)
                } else {
                    failed++
                    failedNums.add(chapterNumber)
                }
            }

            _batchDownloadState.value = BatchDownloadState(
                mangaTitle = mangaTitle,
                totalChapters = chapters.size,
                completedChapters = completed,
                failedChapters = failed,
                currentIndex = chapters.size - 1,
                isComplete = true,
                completedNumbers = completedNums.toSet(),
                failedNumbers = failedNums.toSet()
            )

            showBatchCompleteNotification(mangaTitle, completed, failed, chapters.size)

            kotlinx.coroutines.delay(5000)
            _batchDownloadState.value = null
        }

        downloadJobs["batch"] = job
    }

    fun cancelDownload(taskId: String) {
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        val current = _activeDownloads.value.find { it.id == taskId } ?: return
        updateTask(current.copy(status = DownloadStatus.FAILED, errorMessage = "Cancelled"))
    }

    fun pauseDownload(taskId: String) {
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        val current = _activeDownloads.value.find { it.id == taskId } ?: return
        updateTask(current.copy(status = DownloadStatus.PAUSED))
    }

    fun resumeDownload(taskId: String, imageUrls: List<String>) {
        val current = _activeDownloads.value.find { it.id == taskId } ?: return
        if (current.status != DownloadStatus.PAUSED && current.status != DownloadStatus.FAILED) return
        val task = current.copy(status = DownloadStatus.PENDING, errorMessage = null)
        updateTask(task)
        val job = scope.launch {
            downloadChapter(task, imageUrls)
        }
        downloadJobs[taskId] = job
    }

    fun removeTask(taskId: String) {
        cancelDownload(taskId)
        val all = loadTasks().toMutableList()
        all.removeAll { it.id == taskId }
        saveTasks(all)
        _activeDownloads.value = all
    }

    fun getDownloadedChapterIds(mangaId: String): Set<String> {
        return _activeDownloads.value
            .filter { it.mangaId == mangaId && it.status == DownloadStatus.COMPLETED }
            .map { it.id }
            .toSet()
    }

    fun getDownloadedPages(taskId: String): List<File> {
        val task = _activeDownloads.value.find { it.id == taskId } ?: return emptyList()
        if (task.status != DownloadStatus.COMPLETED) return emptyList()
        val dir = getChapterDirectory(task.mangaTitle, task.chapterNumber)
        return dir.listFiles()?.sortedBy { it.name }?.filter { it.isFile } ?: emptyList()
    }

    fun getDownloadedPagesByMangaAndChapter(mangaTitle: String, chapterNumber: Double): List<File> {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val mangaSlug = resolveMangaSlug(baseDir, sanitizeFileName(mangaTitle))
        val chapterSlug = "chapter-${chapterNumber}"
        val dir = File(baseDir, "$mangaSlug/$chapterSlug")
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.sortedBy { it.name }?.filter { it.isFile && it.extension == "jpg" } ?: emptyList()
    }

    fun getCompletedChapterNumbers(mangaTitle: String): Set<Double> {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val mangaSlug = resolveMangaSlug(baseDir, sanitizeFileName(mangaTitle))
        val mangaDir = File(baseDir, mangaSlug)
        if (!mangaDir.exists()) return emptySet()
        return mangaDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("chapter-") }
            ?.mapNotNull { it.name.removePrefix("chapter-").toDoubleOrNull() }
            ?.filter { chNum ->
                val chapterDir = File(mangaDir, "chapter-$chNum")
                val pageCount = chapterDir.listFiles()?.filter { it.isFile && it.extension == "jpg" }?.size ?: 0
                pageCount > 0
            }
            ?.toSet() ?: emptySet()
    }

    fun saveCoverImage(mangaTitle: String, coverUrl: String) {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val mangaSlug = resolveMangaSlug(baseDir, sanitizeFileName(mangaTitle))
        val coverFile = File(baseDir, "$mangaSlug/cover.jpg")
        if (coverFile.exists()) return
        coverFile.parentFile?.mkdirs()
        scope.launch {
            try {
                val request = Request.Builder().url(coverUrl).addHeader("User-Agent", "oni/1.0").build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null) coverFile.writeBytes(bytes)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun scanDownloadedManga(): List<DownloadedManga> = coroutineScope {
        val baseDir = File(getDownloadDirectory())
        if (!baseDir.exists()) return@coroutineScope emptyList()

        val mangaDirs = baseDir.listFiles()?.filter { it.isDirectory } ?: return@coroutineScope emptyList()

        val result = mangaDirs.map { mangaDir ->
            async(Dispatchers.IO) {
                val chapterDirs = mangaDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("chapter-") } ?: return@async null
                val chapters = chapterDirs.mapNotNull ch@{ chDir ->
                    val numStr = chDir.name.removePrefix("chapter-").toDoubleOrNull() ?: return@ch null
                    val pageCount = chDir.list()?.count { it.endsWith(".jpg", ignoreCase = true) } ?: 0
                    if (pageCount > 0) DownloadedChapter(numStr, chDir, pageCount) else null
                }.sortedBy { it.chapterNumber }

                if (chapters.isNotEmpty()) {
                    val title = mangaDir.name.replace("-", " ").replaceFirstChar { it.uppercase() }
                    val coverFile = File(mangaDir, "cover.jpg")
                    val coverPath = if (coverFile.exists() && coverFile.length() > 0) coverFile.absolutePath else null
                    val marker = File(mangaDir, ".lastread")
                    val lastRead = if (marker.exists()) marker.readText().trim().substringBefore(":").toDoubleOrNull() else null
                    DownloadedManga(title, mangaDir.name, coverPath, chapters, lastRead)
                } else null
            }
        }.awaitAll().filterNotNull()

        // Merge entries with the same display title (handles case-insensitive dupes like "Blue lock" / "blue-lock")
        val merged = mutableMapOf<String, DownloadedManga>()
        for (manga in result.sortedByDescending { it.chapters.size }) {
            val existing = merged[manga.title]
            if (existing == null) {
                merged[manga.title] = manga
            } else {
                // Keep the entry with more chapters, merge any missing ones
                val allChapters = (existing.chapters + manga.chapters)
                    .distinctBy { it.chapterNumber }
                    .sortedBy { it.chapterNumber }
                val bestSlug = if (manga.chapters.size >= existing.chapters.size) manga.slug else existing.slug
                val bestCover = existing.coverPath ?: manga.coverPath
                val bestLastRead = maxOf(existing.lastReadChapter ?: 0.0, manga.lastReadChapter ?: 0.0).takeIf { it > 0 }
                merged[manga.title] = existing.copy(
                    slug = bestSlug,
                    coverPath = bestCover,
                    chapters = allChapters,
                    lastReadChapter = bestLastRead
                )
            }
        }

        return@coroutineScope merged.values.sortedBy { it.title }
    }

    fun saveLastReadChapter(mangaSlug: String, chapterNumber: Double, pageIndex: Int = 0) {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val resolvedSlug = resolveMangaSlug(baseDir, mangaSlug)
        val marker = File(baseDir, "$resolvedSlug/.lastread")
        marker.parentFile?.mkdirs()
        marker.writeText("$chapterNumber:$pageIndex")
    }

    fun getLastReadChapter(mangaSlug: String): Double? {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val resolvedSlug = resolveMangaSlug(baseDir, mangaSlug)
        val marker = File(baseDir, "$resolvedSlug/.lastread")
        if (!marker.exists()) return null
        val text = marker.readText().trim()
        return text.substringBefore(":").toDoubleOrNull()
    }

    fun getLastReadPageIndex(mangaSlug: String): Int {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val resolvedSlug = resolveMangaSlug(baseDir, mangaSlug)
        val marker = File(baseDir, "$resolvedSlug/.lastread")
        if (!marker.exists()) return 0
        val text = marker.readText().trim()
        return text.substringAfter(":").toIntOrNull() ?: 0
    }

    fun clearLastReadChapter(mangaSlug: String) {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val resolvedSlug = resolveMangaSlug(baseDir, mangaSlug)
        val marker = File(baseDir, "$resolvedSlug/.lastread")
        if (marker.exists()) marker.delete()
    }

    fun deleteManga(mangaSlug: String) {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val resolvedSlug = resolveMangaSlug(baseDir, mangaSlug)
        val dir = File(baseDir, resolvedSlug)
        if (dir.exists()) dir.deleteRecursively()
    }

    fun deleteChapter(mangaSlug: String, chapterNumber: Double) {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val resolvedSlug = resolveMangaSlug(baseDir, mangaSlug)
        val chapterDir = File(baseDir, "$resolvedSlug/chapter-$chapterNumber")
        if (chapterDir.exists()) chapterDir.deleteRecursively()
    }

    fun moveDownloadsToDirectory(newDir: String, onDone: () -> Unit = {}) {
        val oldDir = getDownloadDirectory()
        if (oldDir == newDir) { onDone(); return }
        val oldResolved = when {
            oldDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + oldDir.removePrefix("/tree/primary:")
            oldDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + oldDir.removePrefix("/document/primary:")
            else -> oldDir
        }
        val newResolved = when {
            newDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + newDir.removePrefix("/tree/primary:")
            newDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + newDir.removePrefix("/document/primary:")
            else -> newDir
        }
        val oldFile = File(oldResolved)
        val newFile = File(newResolved)
        if (!oldFile.exists() || oldFile == newFile) { onDone(); return }
        scope.launch {
            newFile.mkdirs()
            oldFile.listFiles()?.forEach { child ->
                val target = File(newFile, child.name)
                if (!child.renameTo(target)) {
                    copyRecursively(child, target)
                    child.deleteRecursively()
                }
            }
            if (oldFile.listFiles().isNullOrEmpty()) oldFile.delete()
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    private fun copyRecursively(src: File, dst: File) {
        if (src.isDirectory) {
            dst.mkdirs()
            src.listFiles()?.forEach { copyRecursively(it, File(dst, it.name)) }
        } else {
            dst.parentFile?.mkdirs()
            src.copyTo(dst, overwrite = true)
        }
    }

    private suspend fun downloadChapter(task: DownloadTask, imageUrls: List<String>, showNotification: Boolean = true) {
        updateTask(task.copy(status = DownloadStatus.DOWNLOADING, downloadedPages = 0))
        if (showNotification) {
            showDownloadNotification(task.id, "${task.mangaTitle} - Ch.${task.chapterNumber}", 0, imageUrls.size)
        }

        val dir = getChapterDirectory(task.mangaTitle, task.chapterNumber)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        var downloaded = 0

        suspend fun downloadMissingPages(): Int {
            var count = 0
            for ((index, url) in imageUrls.withIndex()) {
                try {
                    val fileName = "page-${String.format("%03d", index + 1)}.jpg"
                    val file = File(dir, fileName)
                    if (file.exists() && file.length() > 0) {
                        count++
                        continue
                    }
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "oni/1.0")
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            file.writeBytes(bytes)
                            count++
                        }
                    }
                } catch (_: Exception) {}
            }
            return count
        }

        downloaded = downloadMissingPages()
        updateTask(task.copy(downloadedPages = downloaded))
        if (showNotification) {
            showDownloadNotification(task.id, "${task.mangaTitle} - Ch.${task.chapterNumber}", downloaded, imageUrls.size)
        }

        var attempt = 1
        while (downloaded < imageUrls.size && attempt <= MAX_RETRIES) {
            kotlinx.coroutines.delay(1500)
            downloaded = downloadMissingPages()
            updateTask(task.copy(downloadedPages = downloaded))
            if (showNotification) {
                showDownloadNotification(task.id, "${task.mangaTitle} - Ch.${task.chapterNumber} (retry $attempt)", downloaded, imageUrls.size)
            }
            attempt++
        }

        if (downloaded == imageUrls.size) {
            updateTask(task.copy(status = DownloadStatus.COMPLETED, downloadedPages = downloaded))
            if (showNotification) {
                dismissDownloadNotification(task.id)
                showDownloadCompleteNotification("${task.mangaTitle} - Ch.${task.chapterNumber}")
            }
            Log.d(TAG, "Completed download: ${task.mangaTitle} Ch.${task.chapterNumber}")
        } else {
            updateTask(task.copy(
                status = DownloadStatus.FAILED,
                downloadedPages = downloaded,
                errorMessage = "Downloaded $downloaded/${imageUrls.size} pages after $attempt attempts"
            ))
            if (showNotification) {
                dismissDownloadNotification(task.id)
            }
        }
    }

    private fun getChapterDirectory(mangaTitle: String, chapterNumber: Double): File {
        var baseDir = getDownloadDirectory()
        baseDir = when {
            baseDir.startsWith("/tree/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/tree/primary:")
            baseDir.startsWith("/document/primary:") ->
                "/storage/emulated/0/" + baseDir.removePrefix("/document/primary:")
            else -> baseDir
        }
        val mangaSlug = sanitizeFileName(mangaTitle)
        val resolvedSlug = resolveMangaSlug(baseDir, mangaSlug)
        val chapterSlug = "chapter-${chapterNumber}"
        return File(baseDir, "$resolvedSlug/$chapterSlug")
    }

    private fun resolveMangaSlug(baseDir: String, mangaSlug: String): String {
        val mangaDir = File(baseDir, mangaSlug)
        if (mangaDir.exists()) return mangaSlug
        val existing = File(baseDir).listFiles()
            ?.firstOrNull { it.isDirectory && it.name.equals(mangaSlug, ignoreCase = true) }
        return existing?.name ?: mangaSlug
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
            .lowercase()
            .take(80)
    }

    private fun updateTask(task: DownloadTask) {
        val all = loadTasks().toMutableList()
        val index = all.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            all[index] = task
        } else {
            all.add(task)
        }
        saveTasks(all)
        _activeDownloads.value = all

        if (task.status == DownloadStatus.COMPLETED) {
            scope.launch {
                kotlinx.coroutines.delay(3000)
                removeTask(task.id)
            }
        }
    }

    private fun loadTasks(): List<DownloadTask> {
        val json = prefs.getString(KEY_DOWNLOADS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DownloadTask>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveTasks(tasks: List<DownloadTask>) {
        val json = gson.toJson(tasks)
        prefs.edit().putString(KEY_DOWNLOADS, json).apply()
    }
}
