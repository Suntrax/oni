package com.blissless.oni.data

data class MangaSearchResult(
    val title: String,
    val url: String,
    val coverUrl: String? = null,
    val mangaId: String? = null
)

data class ChapterInfo(
    val url: String,
    val title: String? = null
)

data class ChapterImages(
    val chapterUrl: String,
    val images: List<String>
)
