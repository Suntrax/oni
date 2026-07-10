package com.blissless.oni.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AniListSearchResult(
    val id: Int,
    val title: String,
    val englishTitle: String? = null,
    val nativeTitle: String? = null,
    val coverUrl: String? = null,
    val coverExtraLarge: String? = null,
    val description: String? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val status: String? = null,
    val genres: List<String>? = null,
    val meanScore: Int? = null,
    val format: String? = null
)

data class AniListMangaEntry(
    val mediaId: Int,
    val title: String,
    val englishTitle: String? = null,
    val nativeTitle: String? = null,
    val coverUrl: String? = null,
    val siteUrl: String? = null,
    val chapters: Int? = null,
    val description: String? = null,
    val progress: Int = 0,
    val status: String = "",
    val score: Int? = null,
    val localMangaUrl: String? = null
) {
    fun toReadingStatus(): ReadingStatus = when (status) {
        "CURRENT", "REPEATING" -> ReadingStatus.READING
        "PLANNING" -> ReadingStatus.PLANNING
        "COMPLETED" -> ReadingStatus.COMPLETED
        "PAUSED" -> ReadingStatus.ON_HOLD
        "DROPPED" -> ReadingStatus.DROPPED
        else -> ReadingStatus.PLANNING
    }

    fun withLocalUrl(url: String?): AniListMangaEntry = copy(localMangaUrl = url)
}

data class AniListMangaDetail(
    val id: Int,
    val titleRomaji: String,
    val titleEnglish: String?,
    val titleNative: String?,
    val coverExtraLarge: String?,
    val coverLarge: String?,
    val bannerImage: String?,
    val description: String?,
    val genres: List<String>,
    val tags: List<AniListTag>,
    val status: String?,
    val format: String?,
    val chapters: Int?,
    val volumes: Int?,
    val averageScore: Int?,
    val popularity: Int?,
    val favourites: Int?,
    val trending: Int?,
    val startYear: Int?, val startMonth: Int?, val startDay: Int?,
    val endYear: Int?, val endMonth: Int?, val endDay: Int?,
    val source: String?,
    val countryOfOrigin: String?,
    val synonyms: List<String>,
    val siteUrl: String?,
    val staff: List<AniListStaffEntry>,
    val relations: List<AniListRelationEntry>,
    val recommendations: List<AniListSearchResult>,
    val rankings: List<AniListRankingEntry>,
    val characters: List<AniListCharacterEntry>,
    val externalLinks: List<AniListExternalLink>
)

data class AniListTag(val id: Int, val name: String, val description: String? = null, val rank: Int = 0)
data class AniListStaffEntry(val name: String, val role: String, val image: String? = null)
data class AniListRelationEntry(val id: Int, val title: String, val englishTitle: String?, val coverUrl: String?, val relationType: String, val format: String?)
data class AniListRankingEntry(val rank: Int, val type: String, val context: String, val season: String?, val year: Int?, val allTime: Boolean)
data class AniListCharacterEntry(val name: String, val image: String?, val role: String)
data class AniListExternalLink(val url: String, val site: String)

data class ExploreSection(
    val key: String,
    val title: String,
    val items: List<AniListSearchResult>
)

class AniListManager(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val PREFS_NAME = "anilist_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_SYNCED_MANGA = "synced_manga"

        const val AUTH_URL = "https://anilist.co/api/v2/oauth/authorize"
        const val GRAPHQL_URL = "https://graphql.anilist.co"
        const val REDIRECT_URI = "animescraper://success"
        const val CLIENT_ID = 36828

        private val GENRE_SECTIONS = listOf(
            "Action" to "Action",
            "Romance" to "Romance",
            "Fantasy" to "Fantasy",
            "Seinen" to "Seinen",
            "Horror" to "Horror",
            "Mystery" to "Mystery",
            "Comedy" to "Comedy",
            "Adventure" to "Adventure",
            "Drama" to "Drama",
            "Slice of Life" to "Slice of Life"
        )
    }

    fun getAuthUrl(): String {
        return "$AUTH_URL?client_id=$CLIENT_ID&response_type=token"
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun isLoggedIn(): Boolean = !prefs.getString(KEY_ACCESS_TOKEN, "").isNullOrBlank()

    private val MEDIA_CORE_FIELDS = """
        id
        title { romaji english native }
        coverImage { extraLarge large }
        description
        genres
        status
        format
        chapters
        volumes
        averageScore
        siteUrl
    """.trimIndent()

    suspend fun searchManga(query: String): Result<List<AniListSearchResult>> {
        val searchQuery = """
            query (${'$'}search: String) {
              Page(perPage: 50) {
                media(search: ${'$'}search, type: MANGA) {
                  $MEDIA_CORE_FIELDS
                }
              }
            }
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val variables = JSONObject().apply { put("search", query) }
                val result = executeGraphQLList(searchQuery, variables)
                result.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(it) }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun searchMangaAdvanced(
        search: String? = null,
        genres: List<String>? = null,
        format: String? = null,
        status: String? = null,
        sort: String? = null,
        page: Int = 1,
        perPage: Int = 30,
    ): Result<List<AniListSearchResult>> {
        val searchQuery = """
            query (${'$'}search: String, ${'$'}genres: [String], ${'$'}format: MediaFormat, ${'$'}status: MediaStatus, ${'$'}sort: [MediaSort], ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(search: ${'$'}search, genre_in: ${'$'}genres, format: ${'$'}format, status: ${'$'}status, sort: ${'$'}sort, type: MANGA) {
                  $MEDIA_CORE_FIELDS
                }
              }
            }
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val variables = JSONObject()
                if (!search.isNullOrBlank()) variables.put("search", search)
                if (!genres.isNullOrEmpty()) variables.put("genres", JSONArray(genres))
                if (format != null) variables.put("format", format)
                if (status != null) variables.put("status", status)
                variables.put("page", page)
                variables.put("perPage", perPage)
                val sortList = sort?.let { JSONArray(listOf(it)) } ?: JSONArray(listOf("SEARCH_MATCH"))
                variables.put("sort", sortList)
                executeGraphQLList(searchQuery, variables)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMediaDetail(mediaId: Int): Result<AniListMangaDetail> {
        val query = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: MANGA) {
                id
                title { romaji english native }
                coverImage { extraLarge large }
                bannerImage
                description
                genres
                tags { id name description rank }
                status
                format
                chapters
                volumes
                averageScore
                popularity
                favourites
                trending
                startDate { year month day }
                endDate { year month day }
                source
                countryOfOrigin
                synonyms
                siteUrl
                staff(perPage: 6, sort: ROLE) {
                  edges { role node { id name { full } image { large } } }
                }
                relations {
                  edges {
                    relationType
                    node { id title { romaji english } coverImage { extraLarge } format }
                  }
                }
                recommendations(perPage: 6, sort: RATING_DESC) {
                  nodes {
                    mediaRecommendation { id title { romaji english } coverImage { extraLarge } format averageScore }
                  }
                }
                rankings { id rank type context season year allTime }
                characters(perPage: 10, sort: ROLE) {
                  edges { role node { id name { full } image { large } } }
                }
                externalLinks { url site }
              }
            }
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val variables = JSONObject().apply { put("id", mediaId) }
                val result = executeGraphQLRaw(query, variables)
                result.fold(
                    onSuccess = { data ->
                        val media = data.optJSONObject("Media")
                        if (media != null) {
                            Result.success(parseMediaDetail(media))
                        } else {
                            Result.failure(Exception("Media not found"))
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getExploreSections(): Result<List<ExploreSection>> {
        val query = """
            query {
              trending: Page(perPage: 12) {
                media(sort: TRENDING_DESC, type: MANGA) { $MEDIA_CORE_FIELDS }
              }
              popular: Page(perPage: 12) {
                media(sort: POPULARITY_DESC, type: MANGA) { $MEDIA_CORE_FIELDS }
              }
              topRated: Page(perPage: 12) {
                media(sort: SCORE_DESC, type: MANGA) { $MEDIA_CORE_FIELDS }
              }
              favourites: Page(perPage: 12) {
                media(sort: FAVOURITES_DESC, type: MANGA) { $MEDIA_CORE_FIELDS }
              }
              action: Page(perPage: 12) {
                media(genre: "Action", sort: POPULARITY_DESC, type: MANGA) { $MEDIA_CORE_FIELDS }
              }
              romance: Page(perPage: 12) {
                media(genre: "Romance", sort: POPULARITY_DESC, type: MANGA) { $MEDIA_CORE_FIELDS }
              }
              fantasy: Page(perPage: 12) {
                media(genre: "Fantasy", sort: POPULARITY_DESC, type: MANGA) { $MEDIA_CORE_FIELDS }
              }
              seinen: Page(perPage: 12) {
                media(genre: "Seinen", sort: POPULARITY_DESC, type: MANGA) { $MEDIA_CORE_FIELDS }
              }
            }
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val result = executeGraphQLRaw(query, null)
                result.fold(
                    onSuccess = { data ->
                        val sections = mutableListOf<ExploreSection>()
                        val sectionDefs = listOf(
                            "trending" to "Trending Now",
                            "popular" to "Most Popular",
                            "topRated" to "Top Rated",
                            "favourites" to "Most Favourited",
                            "action" to "Action",
                            "romance" to "Romance",
                            "fantasy" to "Fantasy",
                            "seinen" to "Seinen"
                        )
                        for ((key, title) in sectionDefs) {
                            val page = data.optJSONObject(key)
                            val mediaArray = page?.optJSONArray("media")
                            if (mediaArray != null && mediaArray.length() > 0) {
                                val items = parseMediaArray(mediaArray)
                                if (items.isNotEmpty()) {
                                    sections.add(ExploreSection(key = key, title = title, items = items))
                                }
                            }
                        }
                        Result.success(sections)
                    },
                    onFailure = { Result.failure(it) }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMediaByGenre(genre: String, page: Int = 1, perPage: Int = 20): Result<List<AniListSearchResult>> {
        val query = """
            query (${'$'}genre: String, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(genre: ${'$'}genre, sort: POPULARITY_DESC, type: MANGA) {
                  $MEDIA_CORE_FIELDS
                }
              }
            }
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val variables = JSONObject().apply {
                    put("genre", genre)
                    put("page", page)
                    put("perPage", perPage)
                }
                executeGraphQLList(query, variables)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun executeGraphQLList(query: String, variables: JSONObject): Result<List<AniListSearchResult>> {
        val rawResult = executeGraphQLRaw(query, variables)
        return rawResult.fold(
            onSuccess = { data ->
                val page = data.optJSONObject("Page")
                val mediaArray = page?.optJSONArray("media")
                if (mediaArray != null) {
                    Result.success(parseMediaArray(mediaArray))
                } else {
                    Result.failure(Exception("No Page/media in response"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun executeGraphQLRaw(query: String, variables: JSONObject?): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonPayload = JSONObject().apply { put("query", query) }
                if (variables != null) {
                    jsonPayload.put("variables", variables)
                }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val requestBuilder = Request.Builder()
                    .url(GRAPHQL_URL)
                    .addHeader("Content-Type", "application/json")
                val token = getAccessToken()
                if (token != null) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                val request = requestBuilder.post(body).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val root = JSONObject(responseBody)
                    val data = root.optJSONObject("data")
                    if (data != null) Result.success(data)
                    else Result.failure(Exception("No data in response"))
                } else {
                    Result.failure(Exception("GraphQL error: ${response.code} $responseBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseMediaArray(mediaArray: JSONArray): List<AniListSearchResult> {
        val results = mutableListOf<AniListSearchResult>()
        for (i in 0 until mediaArray.length()) {
            val media = mediaArray.getJSONObject(i)
            val title = media.optJSONObject("title")
            val genresArray = media.optJSONArray("genres")
            val genres = if (genresArray != null) {
                (0 until genresArray.length()).map { genresArray.getString(it) }
            } else null
            val romaji = title?.optString("romaji") ?: ""
            val english = title?.optString("english")
            results.add(AniListSearchResult(
                id = media.optInt("id"),
                title = english?.takeIf { it.isNotBlank() } ?: romaji,
                englishTitle = english,
                nativeTitle = title?.optString("native"),
                coverExtraLarge = media.optJSONObject("coverImage")?.optString("extraLarge"),
                coverUrl = media.optJSONObject("coverImage")?.optString("extraLarge") ?: media.optJSONObject("coverImage")?.optString("large"),
                description = media.optString("description"),
                chapters = if (media.has("chapters") && !media.isNull("chapters")) media.optInt("chapters") else null,
                volumes = if (media.has("volumes") && !media.isNull("volumes")) media.optInt("volumes") else null,
                status = media.optString("status"),
                genres = genres,
                meanScore = if (media.has("averageScore") && !media.isNull("averageScore")) media.optInt("averageScore") else null,
                format = media.optString("format")
            ))
        }
        return results
    }

    private fun parseMediaDetail(media: JSONObject): AniListMangaDetail {
        val title = media.optJSONObject("title")
        val coverImage = media.optJSONObject("coverImage")

        val tagsList = mutableListOf<AniListTag>()
        val tagsArray = media.optJSONArray("tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                val tag = tagsArray.getJSONObject(i)
                tagsList.add(AniListTag(
                    id = tag.optInt("id"),
                    name = tag.optString("name"),
                    description = tag.optString("description"),
                    rank = tag.optInt("rank")
                ))
            }
        }

        val staffList = mutableListOf<AniListStaffEntry>()
        val staffContainer = media.optJSONObject("staff")
        val staffEdges = staffContainer?.optJSONArray("edges")
        if (staffEdges != null) {
            for (i in 0 until staffEdges.length()) {
                val edge = staffEdges.getJSONObject(i)
                val node = edge.optJSONObject("node")
                if (node != null) {
                    staffList.add(AniListStaffEntry(
                        name = node.optJSONObject("name")?.optString("full") ?: "",
                        role = edge.optString("role"),
                        image = node.optJSONObject("image")?.optString("large")
                    ))
                }
            }
        }

        val relationsList = mutableListOf<AniListRelationEntry>()
        val relationsContainer = media.optJSONObject("relations")
        val relationsEdges = relationsContainer?.optJSONArray("edges")
        if (relationsEdges != null) {
            for (i in 0 until relationsEdges.length()) {
                val edge = relationsEdges.getJSONObject(i)
                val node = edge.optJSONObject("node")
                if (node != null) {
                    val relTitle = node.optJSONObject("title")
                    val relCover = node.optJSONObject("coverImage")
                    relationsList.add(AniListRelationEntry(
                        id = node.optInt("id"),
                        title = relTitle?.optString("romaji") ?: "",
                        englishTitle = relTitle?.optString("english"),
                        coverUrl = relCover?.optString("extraLarge") ?: relCover?.optString("large"),
                        relationType = edge.optString("relationType"),
                        format = node.optString("format")
                    ))
                }
            }
        }

        val recsList = mutableListOf<AniListSearchResult>()
        val recsContainer = media.optJSONObject("recommendations")
        val recsNodes = recsContainer?.optJSONArray("nodes")
        if (recsNodes != null) {
            for (i in 0 until recsNodes.length()) {
                val rec = recsNodes.getJSONObject(i).optJSONObject("mediaRecommendation")
                if (rec != null) {
                    val recTitle = rec.optJSONObject("title")
                    val recCover = rec.optJSONObject("coverImage")
                    recsList.add(AniListSearchResult(
                        id = rec.optInt("id"),
                        title = recTitle?.optString("romaji") ?: "",
                        englishTitle = recTitle?.optString("english"),
                        coverUrl = recCover?.optString("extraLarge") ?: recCover?.optString("large"),
                        coverExtraLarge = recCover?.optString("extraLarge"),
                        format = rec.optString("format"),
                        meanScore = if (rec.has("averageScore") && !rec.isNull("averageScore")) rec.optInt("averageScore") else null
                    ))
                }
            }
        }

        val rankingsList = mutableListOf<AniListRankingEntry>()
        val rankingsArray = media.optJSONArray("rankings")
        if (rankingsArray != null) {
            for (i in 0 until rankingsArray.length()) {
                val r = rankingsArray.getJSONObject(i)
                rankingsList.add(AniListRankingEntry(
                    rank = r.optInt("rank"),
                    type = r.optString("type"),
                    context = r.optString("context"),
                    season = r.optString("season"),
                    year = if (r.has("year") && !r.isNull("year")) r.optInt("year") else null,
                    allTime = r.optBoolean("allTime")
                ))
            }
        }

        val charactersList = mutableListOf<AniListCharacterEntry>()
        val charsContainer = media.optJSONObject("characters")
        val charsEdges = charsContainer?.optJSONArray("edges")
        if (charsEdges != null) {
            for (i in 0 until charsEdges.length()) {
                val edge = charsEdges.getJSONObject(i)
                val node = edge.optJSONObject("node")
                if (node != null) {
                    charactersList.add(AniListCharacterEntry(
                        name = node.optJSONObject("name")?.optString("full") ?: "",
                        image = node.optJSONObject("image")?.optString("large"),
                        role = edge.optString("role")
                    ))
                }
            }
        }

        val linksList = mutableListOf<AniListExternalLink>()
        val linksArray = media.optJSONArray("externalLinks")
        if (linksArray != null) {
            for (i in 0 until linksArray.length()) {
                val link = linksArray.getJSONObject(i)
                linksList.add(AniListExternalLink(
                    url = link.optString("url"),
                    site = link.optString("site")
                ))
            }
        }

        val synopsisList = mutableListOf<String>()
        val synopsisArray = media.optJSONArray("synonyms")
        if (synopsisArray != null) {
            for (i in 0 until synopsisArray.length()) {
                synopsisList.add(synopsisArray.getString(i))
            }
        }

        val genresList = mutableListOf<String>()
        val genresArray = media.optJSONArray("genres")
        if (genresArray != null) {
            for (i in 0 until genresArray.length()) {
                genresList.add(genresArray.getString(i))
            }
        }

        val startDate = media.optJSONObject("startDate")
        val endDate = media.optJSONObject("endDate")

        return AniListMangaDetail(
            id = media.optInt("id"),
            titleRomaji = title?.optString("romaji") ?: "",
            titleEnglish = title?.optString("english"),
            titleNative = title?.optString("native"),
            coverExtraLarge = coverImage?.optString("extraLarge"),
            coverLarge = coverImage?.optString("large"),
            bannerImage = media.optString("bannerImage"),
            description = media.optString("description"),
            genres = genresList,
            tags = tagsList,
            status = media.optString("status"),
            format = media.optString("format"),
            chapters = if (media.has("chapters") && !media.isNull("chapters")) media.optInt("chapters") else null,
            volumes = if (media.has("volumes") && !media.isNull("volumes")) media.optInt("volumes") else null,
            averageScore = if (media.has("averageScore") && !media.isNull("averageScore")) media.optInt("averageScore") else null,
            popularity = if (media.has("popularity") && !media.isNull("popularity")) media.optInt("popularity") else null,
            favourites = if (media.has("favourites") && !media.isNull("favourites")) media.optInt("favourites") else null,
            trending = if (media.has("trending") && !media.isNull("trending")) media.optInt("trending") else null,
            startYear = startDate?.optInt("year"), startMonth = startDate?.optInt("month"), startDay = startDate?.optInt("day"),
            endYear = endDate?.optInt("year"), endMonth = endDate?.optInt("month"), endDay = endDate?.optInt("day"),
            source = media.optString("source"),
            countryOfOrigin = media.optString("countryOfOrigin"),
            synonyms = synopsisList,
            siteUrl = media.optString("siteUrl"),
            staff = staffList,
            relations = relationsList,
            recommendations = recsList,
            rankings = rankingsList,
            characters = charactersList,
            externalLinks = linksList
        )
    }

    // =============== Existing methods (auth, user lists, sync) ===============

    fun getLoggedInUser(): String? {
        val json = prefs.getString(KEY_USER_DATA, null) ?: return null
        return try {
            JSONObject(json).optString("name")
        } catch (e: Exception) { null }
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_USER_DATA)
            .remove(KEY_SYNCED_MANGA)
            .apply()
    }

    suspend fun getUserInfo(): Result<JSONObject> {
        val token = getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        val query = "query { Viewer { id name avatar { large } } }"
        return executeGraphQLRaw(query, null)
    }

    suspend fun fetchAndCacheUserInfo(): Result<String> {
        val result = getUserInfo()
        return result.map { data ->
            val viewer = data.optJSONObject("Viewer")
            if (viewer != null) {
                prefs.edit().putString(KEY_USER_DATA, viewer.toString()).apply()
            }
            viewer?.optString("name") ?: "Unknown"
        }
    }

    suspend fun getUserMangaLists(): Result<List<AniListMangaEntry>> {
        val token = getAccessToken() ?: return Result.failure(Exception("Not logged in"))

        val userResult = getUserInfo()
        val data = userResult.getOrNull() ?: return Result.failure(Exception("Failed to get user info"))
        val viewer = data.optJSONObject("Viewer") ?: return Result.failure(Exception("No Viewer data"))
        val userId = viewer.optInt("id", -1)
        if (userId < 0) return Result.failure(Exception("Invalid user ID"))

        val query = """
            query {
              MediaListCollection(userId: $userId, type: MANGA) {
                lists {
                  name
                  entries {
                    media {
                      id
                      title { romaji english native }
                      coverImage { extraLarge large }
                      siteUrl
                      chapters
                    }
                    progress
                    status
                    score
                  }
                }
              }
            }
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val jsonPayload = JSONObject().apply { put("query", query) }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(GRAPHQL_URL)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val root = JSONObject(responseBody)
                    val collection = root.optJSONObject("data")?.optJSONObject("MediaListCollection")
                    val lists = collection?.optJSONArray("lists")
                    val entries = mutableListOf<AniListMangaEntry>()

                    if (lists != null) {
                        for (i in 0 until lists.length()) {
                            val list = lists.getJSONObject(i)
                            val listEntries = list.optJSONArray("entries")
                            if (listEntries != null) {
                                for (j in 0 until listEntries.length()) {
                                    val entry = listEntries.getJSONObject(j)
                                    val media = entry.optJSONObject("media")
                                    val title = media?.optJSONObject("title")
                                    val romajiTitle = title?.optString("romaji") ?: ""
                                    val englishTitle = title?.optString("english")
                                    entries.add(AniListMangaEntry(
                                        mediaId = media?.optInt("id") ?: 0,
                                        title = englishTitle?.takeIf { it.isNotBlank() } ?: romajiTitle,
                                        englishTitle = englishTitle,
                                        nativeTitle = title?.optString("native"),
                                        coverUrl = media?.optJSONObject("coverImage")?.optString("extraLarge") ?: media?.optJSONObject("coverImage")?.optString("large"),
                                        siteUrl = media?.optString("siteUrl"),
                                        chapters = if (media?.has("chapters") == true && !media.isNull("chapters")) media.optInt("chapters") else null,
                                        progress = entry.optInt("progress", 0),
                                        status = entry.optString("status", ""),
                                        score = if (entry.has("score") && !entry.isNull("score")) entry.optInt("score") else null
                                    ))
                                }
                            }
                        }
                    }

                    saveSyncedManga(entries)
                    Result.success(entries)
                } else {
                    Result.failure(Exception("API error: ${response.code} $responseBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getSyncedManga(): List<AniListMangaEntry> {
        val json = prefs.getString(KEY_SYNCED_MANGA, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AniListMangaEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun saveSyncedManga(entries: List<AniListMangaEntry>) {
        prefs.edit().putString(KEY_SYNCED_MANGA, gson.toJson(entries)).apply()
    }

    fun updateSyncedMangaEntry(entry: AniListMangaEntry) {
        val all = getSyncedManga().toMutableList()
        val idx = all.indexOfFirst { it.mediaId == entry.mediaId }
        if (idx >= 0) all[idx] = entry else all.add(entry)
        saveSyncedManga(all)
    }

    fun createTrackingEntries(entries: List<AniListMangaEntry>, trackingManager: TrackingManager) {
        for (entry in entries) {
            val mangaId = "anilist_${entry.mediaId}"
            val existing = trackingManager.getMangaTracking(mangaId)
            if (existing != null) {
                val updated = existing.copy(
                    status = entry.toReadingStatus(),
                    currentChapterNumber = entry.progress,
                    currentChapterIndex = (entry.progress - 1).coerceAtLeast(0),
                    totalChapters = entry.chapters ?: existing.totalChapters,
                    lastReadTimestamp = System.currentTimeMillis(),
                    anilistMediaId = entry.mediaId
                )
                trackingManager.updateTracking(updated)
            } else {
                val track = MangaTrack(
                    mangaId = mangaId,
                    title = entry.title,
                    coverUrl = entry.coverUrl,
                    currentChapterIndex = (entry.progress - 1).coerceAtLeast(0),
                    currentChapterNumber = entry.progress,
                    currentChapterUrl = "",
                    totalChapters = entry.chapters ?: 0,
                    status = entry.toReadingStatus(),
                    lastReadTimestamp = System.currentTimeMillis(),
                    mangaUrl = entry.localMangaUrl ?: entry.siteUrl ?: "https://anilist.co/manga/${entry.mediaId}",
                    anilistMediaId = entry.mediaId
                )
                trackingManager.updateTracking(track)
            }
        }
    }

    suspend fun updateMediaListEntry(mediaId: Int, progress: Int, status: String): Result<Unit> {
        val token = getAccessToken() ?: return Result.failure(Exception("Not logged in"))

        val dollar = "${'$'}"
        val mutation = """
            mutation(${dollar}mediaId: Int, ${dollar}progress: Int, ${dollar}status: MediaListStatus) {
                SaveMediaListEntry(mediaId: ${dollar}mediaId, progress: ${dollar}progress, status: ${dollar}status) {
                    id
                    progress
                    status
                }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("mediaId", mediaId)
            put("progress", progress)
            put("status", status)
        }

        return executeGraphQLWithVariables(mutation, variables, token).map { }
    }

    suspend fun deleteMediaListEntry(mediaId: Int): Result<Unit> {
        val token = getAccessToken() ?: return Result.failure(Exception("Not logged in"))

        val dollar = "${'$'}"
        val mutation = """
            mutation(${dollar}mediaId: Int) {
                DeleteMediaListEntry(id: ${dollar}mediaId) {
                    deleted
                }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("mediaId", mediaId)
        }

        return executeGraphQLWithVariables(mutation, variables, token).map { }
    }

    private suspend fun executeGraphQLWithVariables(query: String, variables: JSONObject, token: String): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("query", query)
                    put("variables", variables)
                }
                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(GRAPHQL_URL)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val root = JSONObject(responseBody)
                    val data = root.optJSONObject("data")
                    if (data != null) Result.success(data)
                    else Result.failure(Exception("No data in response: $responseBody"))
                } else {
                    Result.failure(Exception("GraphQL error: ${response.code} $responseBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
