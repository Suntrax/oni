package com.blissless.oni.data

data class MangaSearchResult(
    val title: String,
    val url: String,
    val coverUrl: String? = null,
    val mangaId: String? = null
)

data class ChapterInfo(
    val url: String,
    val title: String? = null,
    /**
     * Optional MangaDex chapter UUID. Populated when this chapter came from the
     * MangaDex aggregate endpoint. The [url] field will be `mangadex:<chapterId>`.
     * Kept as a separate field so callers that need just the UUID (e.g. for the
     * at-home server) don't have to re-parse the URL.
     */
    val chapterId: String? = null,
    /**
     * Optional volume label (e.g. "1", "2", or "none") from the MangaDex aggregate.
     * Used by the chapter list UI to group chapters by volume.
     */
    val volume: String? = null,
    /** Optional scanlation-group names that worked on this chapter. */
    val groups: List<String> = emptyList()
)

data class ChapterImages(
    val chapterUrl: String,
    val images: List<String>
)
