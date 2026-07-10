package com.blissless.oni.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MangaDexManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_URL = "https://api.mangadex.org"
    }

    suspend fun getChapterCount(title: String, aniListId: Int): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val mangaUuid = findMangaByTitle(title, aniListId) ?: return@withContext null
                getLatestChapterNumber(mangaUuid)
            } catch (_: Exception) { null }
        }
    }

    private fun findMangaByTitle(title: String, aniListId: Int): String? {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val url = "$BASE_URL/manga?limit=5&title=$encodedTitle&order%5Brelevance%5D=desc"
        val request = Request.Builder().url(url).header("User-Agent", "oni/1.0").get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        val data = json.optJSONArray("data") ?: return null
        for (i in 0 until data.length()) {
            val manga = data.getJSONObject(i)
            val links = manga.optJSONObject("attributes")?.optJSONObject("links") ?: continue
            if (links.optString("al") == aniListId.toString()) {
                return manga.optString("id")
            }
        }
        return null
    }

    private fun getLatestChapterNumber(mangaUuid: String): Int? {
        val url = "$BASE_URL/manga/$mangaUuid/feed?limit=1&order%5Bchapter%5D=desc&contentRating%5B%5D=safe&includeFuturePublishAt=0&includeEmptyChapter=0"
        val request = Request.Builder().url(url).header("User-Agent", "oni/1.0").get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        val data = json.optJSONArray("data")
        if (data == null || data.length() == 0) return null
        val chapterStr = data.getJSONObject(0)
            .optJSONObject("attributes")?.optString("chapter")
        if (chapterStr.isNullOrBlank()) return null
        val chapterFloat = chapterStr.toFloatOrNull() ?: return null
        return chapterFloat.toInt()
    }
}
