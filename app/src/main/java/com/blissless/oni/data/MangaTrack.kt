package com.blissless.oni.data

data class MangaTrack(
    val mangaId: String,
    val title: String,
    val coverUrl: String?,
    val currentChapterIndex: Int,
    val currentChapterNumber: Int = -1,
    val currentChapterUrl: String,
    val totalChapters: Int,
    val status: ReadingStatus,
    val lastReadTimestamp: Long,
    val mangaUrl: String,
    val anilistMediaId: Int? = null,
    val scrollProgress: Float = 0f
)

enum class ReadingStatus {
    READING,
    PLANNING,
    COMPLETED,
    ON_HOLD,
    DROPPED
}
