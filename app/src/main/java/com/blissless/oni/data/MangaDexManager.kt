package com.blissless.oni.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of looking up a manga on MangaDex.
 *
 * The MangaDex API does NOT support filtering `/manga` by `links[al]` directly
 * (the parameter is rejected with a 400 - see saved fixtures mdbyal.json / mdbyal2.json),
 * so we search by title and verify the AniList ID by inspecting each result's
 * `attributes.links.al` field. If no result claims the AniList ID, we fall back
 * to the most relevant title match.
 */
data class MangaDexLookupResult(
    val mangaDexId: String,
    val title: String,
    val matchedBy: MatchType
)

enum class MatchType {
    /** Matched by verifying the AniList ID is present in attributes.links.al */
    ANILIST_ID,
    /** Fallback: best relevance match by title only */
    TITLE
}

/** A single chapter entry inside a MangaDex volume aggregate. */
data class MangaDexVolumeChapter(
    val chapter: String,
    val id: String,
    val isUnavailable: Boolean,
    val others: List<String>
)

/** A volume bucket returned by the MangaDex aggregate endpoint. */
data class MangaDexVolume(
    val volume: String,
    val count: Int,
    val chapters: List<MangaDexVolumeChapter>
)

/** Full aggregate structure for a manga: volumes + flattened chapter count. */
data class MangaDexAggregate(
    val mangaId: String,
    val totalChapters: Int,
    val totalVolumes: Int,
    val volumes: List<MangaDexVolume>
)

class MangaDexManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "MangaDex"
        private const val BASE_URL = "https://api.mangadex.org"
    }

    // ======================== Manga lookup ========================

    /**
     * Find a MangaDex manga UUID using [title] and (optionally) verify it against [aniListId].
     *
     * Returns null only if the search itself fails or returns no results at all.
     * If results come back but none of them carry the AniList link, the top relevance
     * hit is returned with [MatchType.TITLE] so the caller can still try to load chapters.
     *
     * We deliberately do NOT filter the aggregate by language later — many popular
     * manga (e.g. Blue Lock) have NO English chapters on MangaDex because they're
     * licensed, but do have hundreds of chapters in other languages. Filtering by
     * `en` would return only 2 chapters (the official volume-1 sample) and break
     * the home + detail screens.
     */
    suspend fun findMangaByAniListId(title: String, aniListId: Int): MangaDexLookupResult? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedTitle = URLEncoder.encode(title, "UTF-8")
                val url = "$BASE_URL/manga?limit=10&title=$encodedTitle&order%5Brelevance%5D=desc"
                val request = Request.Builder().url(url).header("User-Agent", "oni/1.0").get().build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "findMangaByAniListId: HTTP ${response.code} for '$title'")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return@withContext null
                Log.d(TAG, "=== findMangaByAniListId: searching '$title' (aniListId=$aniListId) ===")
                Log.d(TAG, "Search returned ${data.length()} results")
                if (data.length() == 0) return@withContext null

                // Log all search results for debugging
                for (i in 0 until data.length()) {
                    val m = data.getJSONObject(i)
                    val a = m.optJSONObject("attributes")
                    val t = a?.optJSONObject("title")?.optString("en") ?: "?"
                    val al = a?.optJSONObject("links")?.optString("al", "none") ?: "none"
                    Log.d(TAG, "  result[$i]: id=${m.optString("id")} title='$t' al=$al")
                }

                // First pass: prefer an exact AniList link match.
                for (i in 0 until data.length()) {
                    val manga = data.getJSONObject(i)
                    val attrs = manga.optJSONObject("attributes") ?: continue
                    val links = attrs.optJSONObject("links") ?: continue
                    val al = links.optString("al", "")
                    if (al.isNotBlank() && al == aniListId.toString()) {
                        val resolvedTitle = resolveDisplayTitle(attrs, title)
                        Log.d(TAG, "Matched '$title' -> ${manga.optString("id")} via AniList ID $aniListId")
                        return@withContext MangaDexLookupResult(
                            mangaDexId = manga.optString("id"),
                            title = resolvedTitle,
                            matchedBy = MatchType.ANILIST_ID
                        )
                    }
                }

                // Second pass: fall back to the most relevant result. We still trust it
                // because MangaDex search already ranks by title similarity.
                val manga = data.getJSONObject(0)
                val attrs = manga.optJSONObject("attributes")
                val resolvedTitle = attrs?.let { resolveDisplayTitle(it, title) } ?: title
                Log.d(TAG, "Fallback match '$title' -> ${manga.optString("id")} (no AniList link)")
                MangaDexLookupResult(
                    mangaDexId = manga.optString("id"),
                    title = resolvedTitle,
                    matchedBy = MatchType.TITLE
                )
            } catch (e: Exception) {
                Log.e(TAG, "findMangaByAniListId failed for '$title'", e)
                null
            }
        }
    }

    /**
     * Find a MangaDex manga UUID by title only (no AniList verification).
     *
     * Used when we have a manga name but no AniList ID (e.g. legacy tracking entries).
     */
    suspend fun findMangaByTitle(title: String): MangaDexLookupResult? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedTitle = URLEncoder.encode(title, "UTF-8")
                val url = "$BASE_URL/manga?limit=5&title=$encodedTitle&order%5Brelevance%5D=desc"
                val request = Request.Builder().url(url).header("User-Agent", "oni/1.0").get().build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return@withContext null
                if (data.length() == 0) return@withContext null
                val manga = data.getJSONObject(0)
                val attrs = manga.optJSONObject("attributes")
                val resolvedTitle = attrs?.let { resolveDisplayTitle(it, title) } ?: title
                MangaDexLookupResult(
                    mangaDexId = manga.optString("id"),
                    title = resolvedTitle,
                    matchedBy = MatchType.TITLE
                )
            } catch (e: Exception) {
                Log.e(TAG, "findMangaByTitle failed for '$title'", e)
                null
            }
        }
    }

    /**
     * Pick the best display title from a MangaDex manga attributes object.
     * Preference order: en > en-* > romaji (ja-ro) > first altTitle > fallback.
     */
    private fun resolveDisplayTitle(attrs: JSONObject, fallback: String): String {
        val titleObj = attrs.optJSONObject("title") ?: return fallback
        // Primary: English title.
        titleObj.optString("en").takeIf { it.isNotBlank() }?.let { return it }
        // Any English variant in the title object.
        val titleKeys = titleObj.keys()
        while (titleKeys.hasNext()) {
            val k = titleKeys.next()
            if (k.startsWith("en", ignoreCase = true)) {
                titleObj.optString(k).takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        // Alt titles.
        val altTitles = attrs.optJSONArray("altTitles") ?: return fallback
        for (i in 0 until altTitles.length()) {
            val alt = altTitles.getJSONObject(i)
            alt.optString("en").takeIf { it.isNotBlank() }?.let { return it }
            val altKeys = alt.keys()
            while (altKeys.hasNext()) {
                val k = altKeys.next()
                if (k.startsWith("en", ignoreCase = true)) {
                    alt.optString(k).takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return fallback
    }

    // ======================== Aggregate (volumes + chapters) ========================

    /**
     * Fetch the volume/chapter aggregate for a MangaDex manga.
     *
     * Uses `/manga/{id}/aggregate` which returns a compact volumes -> chapters map
     * with real chapter UUIDs we can later feed to the at-home server.
     *
     * We deliberately do NOT filter by `translatedLanguage` here. Many popular
     * licensed manga (Blue Lock, JJK, One Piece, ...) have NO English chapters on
     * MangaDex because the English version is on official readers — filtering by
     * `en` would return only 1-2 official sample chapters and break the home +
     * detail screens. The at-home server happily serves chapters in ANY language,
     * so the unfiltered aggregate is both the chapter list AND a loadable source.
     *
     * The `languages` parameter is kept for API compatibility but ignored.
     */
    suspend fun fetchAggregate(
        mangaUuid: String,
        @Suppress("UNUSED_PARAMETER") languages: List<String> = emptyList()
    ): MangaDexAggregate? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/manga/$mangaUuid/aggregate"
                val request = Request.Builder().url(url)
                    .header("User-Agent", "oni/1.0").get().build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchAggregate: HTTP ${response.code} for $mangaUuid")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                Log.d(TAG, "=== fetchAggregate raw response for $mangaUuid (first 1000 chars) ===")
                Log.d(TAG, body.take(1000))
                val json = JSONObject(body)
                val volumesObj = json.optJSONObject("volumes") ?: return@withContext null

                val volumes = mutableListOf<MangaDexVolume>()
                var totalChapters = 0  // will hold the LATEST chapter number (max), not the count

                val keys = volumesObj.keys()
                while (keys.hasNext()) {
                    val volKey = keys.next()
                    val volObj = volumesObj.getJSONObject(volKey)
                    val volume = volObj.optString("volume")
                    val count = volObj.optInt("count")
                    val chaptersObj = volObj.optJSONObject("chapters") ?: continue
                    val chapterList = mutableListOf<MangaDexVolumeChapter>()

                    val chKeys = chaptersObj.keys()
                    while (chKeys.hasNext()) {
                        val chKey = chKeys.next()
                        val chObj = chaptersObj.getJSONObject(chKey)
                        val othersArr = chObj.optJSONArray("others") ?: JSONArray()
                        val othersList = mutableListOf<String>()
                        for (i in 0 until othersArr.length()) {
                            othersList.add(othersArr.getString(i))
                        }
                        chapterList.add(
                            MangaDexVolumeChapter(
                                chapter = chObj.optString("chapter"),
                                id = chObj.optString("id"),
                                isUnavailable = chObj.optBoolean("isUnavailable"),
                                others = othersList
                            )
                        )
                        // Track the latest (highest) chapter number, not the count.
                        // This is what gets used as the total chapter count everywhere.
                        val chNum = chObj.optString("chapter").toFloatOrNull()?.toInt() ?: 0
                        if (chNum > totalChapters) totalChapters = chNum
                    }
                    volumes.add(MangaDexVolume(volume = volume, count = count, chapters = chapterList))
                }

                MangaDexAggregate(
                    mangaId = mangaUuid,
                    totalChapters = totalChapters,
                    totalVolumes = maxVolumeNumber(volumes),
                    volumes = volumes
                )
            } catch (e: Exception) {
                Log.e(TAG, "fetchAggregate failed for $mangaUuid", e)
                null
            }
        }
    }

    /**
     * Get the highest chapter number from an aggregate.
     * This is the "latest chapter" — e.g. 353 for Blue Lock.
     * Used as the total chapter count when AniList doesn't have one.
     */
    fun latestChapterNumber(aggregate: MangaDexAggregate): Int {
        return aggregate.volumes
            .flatMap { it.chapters }
            .mapNotNull { it.chapter.toFloatOrNull()?.toInt() }
            .maxOrNull() ?: 0
    }

    /**
     * Get the highest numeric volume number from the aggregate's volume list.
     * The "none" volume (unfiled chapters) is excluded.
     * Falls back to [volumes] list size if no numeric volumes exist.
     */
    private fun maxVolumeNumber(volumes: List<MangaDexVolume>): Int {
        val maxNum = volumes
            .mapNotNull { it.volume.toIntOrNull() }
            .maxOrNull() ?: 0
        return if (maxNum > 0) maxNum else volumes.size
    }

    /**
     * Build a flat, deduplicated, OLDEST-FIRST list of [ChapterInfo] from an aggregate.
     *
     * Each chapter number appears once. The URL is prefixed with `mangadex:` so
     * [MainViewModel.loadChapterImages] can route it to the at-home server.
     * (Note: this fallback is only used when buildFullChapterList can't determine
     * a total — normally buildFullChapterList is preferred.)
     */
    fun buildChapterList(aggregate: MangaDexAggregate): List<ChapterInfo> {
        val seen = mutableSetOf<Pair<String, String>>()
        val flat = mutableListOf<Triple<Float, String, MangaDexVolumeChapter>>()
        for (vol in aggregate.volumes) {
            for (ch in vol.chapters) {
                if (ch.isUnavailable) continue
                if (!seen.add(ch.chapter to vol.volume)) continue
                val num = ch.chapter.toFloatOrNull() ?: 0f
                flat.add(Triple(num, vol.volume, ch))
            }
        }
        flat.sortBy { it.first }
        return flat.map { (num, volLabel, ch) ->
            val display = if (num == num.toInt().toFloat()) num.toInt().toString() else num.toString()
            ChapterInfo(
                url = "mangadex:${ch.id}",
                title = "Chapter $display",
                chapterId = ch.id,
                volume = volLabel
            )
        }
    }

    /**
     * Build a FULL chapter list for a manga, filling gaps with extension-loadable chapters.
     *
     * Many popular manga on MangaDex have chapters missing due to DMCA/licensing
     * (e.g. Blue Lock has chapters 1-2 and 282-353, but 3-281 are gone). This
     * method builds a complete 1..N list so the chapter screen looks right.
     *
     * For each chapter number:
     *   - If MangaDex has it → real [ChapterInfo] with `mangadex:<uuid>` URL
     *     (loads via the at-home server).
     *   - If MangaDex doesn't have it → [ChapterInfo] with `extension:<mediaId>:<n>`
     *     URL (loads via the user's selected extension, e.g. atsumaru-extension).
     *
     * Sub-chapters (e.g. 327.1, 327.2) from MangaDex are inserted in ASCENDING
     * order after their parent integer chapter (so 346 → 346.1 → 346.2, not
     * 346 → 346.2 → 346.1).
     *
     * @param aggregate The MangaDex aggregate to pull available chapters from.
     * @param totalChapters The authoritative total chapter count (from AniList).
     *        If null or <= 0, uses the latest chapter number from the MangaDex
     *        aggregate (e.g. 353 for Blue Lock) so the list is always 1..N.
     *        Only INTEGER chapter numbers count toward this total — sub-chapters
     *        like 346.1, 346.2 are inserted as extras and don't inflate the count.
     * @param mediaId The AniList media ID, used to construct extension-loadable URLs.
     * @param extensionChapterCount The total chapter count reported by the user's
     *        selected extension (e.g. atsumaru-extension). Chapters 1..N where
     *        N <= this count AND MangaDex has them are marked as available.
     *        Chapters MangaDex has but the extension doesn't (N > extensionChapterCount)
     *        are greyed out. null or 0 means no extension — ALL chapters greyed out.
     */
    fun buildFullChapterList(
        aggregate: MangaDexAggregate,
        totalChapters: Int?,
        mediaId: Int? = null,
        extensionChapterCount: Int? = null
    ): List<ChapterInfo> {
        // Determine the total: prefer AniList, then extension, then MangaDex latest.
        val resolvedTotal = when {
            totalChapters != null && totalChapters > 0 -> totalChapters
            extensionChapterCount != null && extensionChapterCount > 0 ->
                maxOf(extensionChapterCount, latestChapterNumber(aggregate))
            else -> latestChapterNumber(aggregate)
        }
        if (resolvedTotal <= 0) {
            return buildChapterList(aggregate)
        }
        val extCount = extensionChapterCount ?: 0
        val hasExtension = extCount > 0

        // Collect available chapter numbers from MangaDex (integers + sub-chapters).
        val availableNums = mutableSetOf<Float>()
        val subChapters = mutableMapOf<Float, MangaDexVolumeChapter>()
        val seen = mutableSetOf<String>()
        for (vol in aggregate.volumes) {
            for (ch in vol.chapters) {
                if (ch.isUnavailable) continue
                if (!seen.add(ch.chapter)) continue
                val num = ch.chapter.toFloatOrNull() ?: continue
                if (num == num.toInt().toFloat()) {
                    availableNums.add(num)
                } else {
                    if (num !in subChapters) subChapters[num] = ch
                }
            }
        }

        // Build the full 1..N list OLDEST-FIRST (chapter 1 at index 0, chapter N
        // at the end). This makes read-progress indexing correct: if the user
        // read chapters 1-67, indices 0-66 are marked as read.
        val result = mutableListOf<ChapterInfo>()
        for (n in 1..resolvedTotal) {
            val floatN = n.toFloat()
            val mangaDexHasIt = floatN in availableNums
            val extensionHasIt = hasExtension && n <= extCount
            if (mangaDexHasIt && extensionHasIt) {
                // Both have it — loadable via extension.
                val extUrl = if (mediaId != null) "anilist_${mediaId}_ch_$n" else "extension:$n"
                result.add(ChapterInfo(url = extUrl, title = "Chapter $n"))
            } else {
                // Either MangaDex doesn't have it, or the extension doesn't.
                // Grey it out.
                result.add(ChapterInfo(url = "mangadex:unavailable:$n", title = "Chapter $n (unavailable)"))
            }
            // Insert sub-chapters that belong to this integer chapter (e.g. 346.1,
            // 346.2 go right after 346) in ASCENDING order.
            if (hasExtension && extensionHasIt) {
                subChapters.keys
                    .filter { subNum -> subNum > floatN && subNum < (floatN + 1f) }
                    .sorted()
                    .forEach { subNum ->
                        val ch = subChapters[subNum] ?: return@forEach
                        val display = if (subNum == subNum.toInt().toFloat()) subNum.toInt().toString() else subNum.toString()
                        result.add(ChapterInfo(
                            url = if (mediaId != null) "anilist_${mediaId}_ch_$display" else "extension:$display",
                            title = "Chapter $display",
                            chapterId = ch.id
                        ))
                    }
            }
        }
        return result
    }

    // ======================== Convenience: chapter count ========================

    /**
     * Get just the total chapter count for a manga. Used by the home screen and
     * the manga detail screen when AniList's `chapters` field is null.
     *
     * Returns null if the manga can't be found OR the aggregate can't be fetched.
     */
    suspend fun getChapterCount(title: String, aniListId: Int): Int? {
        val lookup = findMangaByAniListId(title, aniListId) ?: return null
        val aggregate = fetchAggregate(lookup.mangaDexId) ?: return null
        return aggregate.totalChapters
    }

    /**
     * Get the total volume count. Returns null if lookup or aggregate fails.
     */
    suspend fun getVolumeCount(title: String, aniListId: Int): Int? {
        val lookup = findMangaByAniListId(title, aniListId) ?: return null
        val aggregate = fetchAggregate(lookup.mangaDexId) ?: return null
        return aggregate.totalVolumes
    }

    /**
     * Convenience: look up + fetch aggregate in one call.
     * Returns null on any failure.
     */
    suspend fun fetchAggregateForAniList(title: String, aniListId: Int): MangaDexAggregate? {
        val lookup = findMangaByAniListId(title, aniListId) ?: return null
        return fetchAggregate(lookup.mangaDexId)
    }

    /**
     * Convenience: look up by title only + fetch aggregate.
     * Used for legacy tracking entries that don't have an AniList media ID.
     */
    suspend fun fetchAggregateForTitle(title: String): MangaDexAggregate? {
        val lookup = findMangaByTitle(title) ?: return null
        return fetchAggregate(lookup.mangaDexId)
    }

    // ======================== Chapter images (at-home server) ========================

    /**
     * Fetch the actual image URLs for a MangaDex chapter via the at-home server.
     *
     * Endpoint: `GET /at-home/server/{chapterId}`
     * Response: `{ baseUrl, chapter: { hash, data: [...filenames...] } }`
     * Final image URL: `{baseUrl}/data/{hash}/{filename}`
     *
     * Returns null on any failure. Returns an empty list if the chapter has no pages.
     */
    suspend fun fetchChapterImages(chapterId: String): List<String>? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/at-home/server/$chapterId")
                    .header("User-Agent", "oni/1.0")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchChapterImages: HTTP ${response.code} for $chapterId")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val baseUrl = json.optString("baseUrl")
                if (baseUrl.isBlank()) return@withContext null
                val chapter = json.optJSONObject("chapter") ?: return@withContext null
                val hash = chapter.optString("hash")
                if (hash.isBlank()) return@withContext null
                val data = chapter.optJSONArray("data") ?: return@withContext null
                val images = mutableListOf<String>()
                for (i in 0 until data.length()) {
                    val filename = data.getString(i)
                    images.add("$baseUrl/data/$hash/$filename")
                }
                Log.d(TAG, "Fetched ${images.size} images for chapter $chapterId")
                images
            } catch (e: Exception) {
                Log.e(TAG, "fetchChapterImages failed for $chapterId", e)
                null
            }
        }
    }

    /**
     * Extract the bare chapter UUID from a `mangadex:<uuid>` URL.
     * Returns null if the URL is not a real MangaDex chapter (e.g. the
     * `mangadex:unavailable:<n>` placeholder used for chapters that neither
     * MangaDex nor the extension can provide), so [MainViewModel.loadChapterImages]
     * falls through to the error path for those.
     */
    fun extractChapterId(chapterUrl: String): String? {
        val prefix = "mangadex:"
        if (!chapterUrl.startsWith(prefix)) return null
        val rest = chapterUrl.removePrefix(prefix)
        if (rest.isBlank()) return null
        // Placeholder for unavailable chapters — not a real UUID.
        if (rest.startsWith("unavailable:")) return null
        return rest
    }
}
