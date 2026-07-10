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
    val scrollProgress: Float = 0f,
    /**
     * Cached MangaDex manga UUID. We look this up once via [MangaDexManager.findMangaByAniListId]
     * and store it so subsequent opens skip the title search and go straight to the
     * aggregate endpoint.
     */
    val mangaDexId: String? = null,
    /**
     * Total volume count from MangaDex. Cached alongside [mangaDexId] so the
     * manga detail screen can show a volume count even when AniList has none.
     */
    val mangaDexVolumeCount: Int? = null
)

enum class ReadingStatus {
    READING,
    PLANNING,
    COMPLETED,
    ON_HOLD,
    DROPPED
}
