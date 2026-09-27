package com.mugarutv.data.repository

import android.util.Log
import com.mugarutv.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

private const val SONGS_ENDPOINT_URL = SCHOOL_APPS_SCRIPT_ENDPOINT

class ApiSongRepository {
    private var cachedSongs: List<Song> = emptyList()
    private var lastFetchAtMs: Long = 0L
    private var cachedLikeUserId: String? = null

    suspend fun fetchSongs(likeUserId: String? = null, forceRefresh: Boolean = false): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            if (SONGS_ENDPOINT_URL.isBlank()) return@runCatching fetchSchoolChannelVideos()
            val now = System.currentTimeMillis()
            val normalizedLikeUserId = likeUserId?.trim().orEmpty().ifBlank { null }
            if (!forceRefresh && cachedSongs.isNotEmpty() && cachedLikeUserId == normalizedLikeUserId && now - lastFetchAtMs < CACHE_TTL_MS) {
                Log.d(TAG, "Using in-memory cache. count=${cachedSongs.size}")
                return@runCatching cachedSongs
            }

            val requestUrl = buildString {
                append(SONGS_ENDPOINT_URL)
                append("?ts=${System.currentTimeMillis()}")
                if (!normalizedLikeUserId.isNullOrBlank()) {
                    append("&user_id=${java.net.URLEncoder.encode(normalizedLikeUserId, "UTF-8")}")
                }
            }

            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }

            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code saat mengambil data film.")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            if (body.trimStart().startsWith("<")) {
                Log.w(TAG, "Backend returned HTML for songs; using school channel feed.")
                return@runCatching fetchSchoolChannelVideos()
            }

            val root = JSONObject(body)
            if (!root.optBoolean("success", false)) throw IllegalStateException("Response API tidak sukses.")
            val songsArray = root.optJSONArray("films")
                ?: root.optJSONArray("movies")
                ?: root.optJSONArray("songs")
                ?: root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("films")
                ?: root.optJSONObject("data")?.optJSONArray("songs")
                ?: throw IllegalStateException("Field films tidak ditemukan.")

            Log.d(TAG, "Raw films from API: ${songsArray.length()}")
            var activeCount = 0
            val songs = buildList<Song> {
                for (i in 0 until songsArray.length()) {
                    val item = songsArray.optJSONObject(i) ?: continue
                    val title = readString(item, "title", "film_title", "movie_title", "judul_film", "song_title", "judul_lagu", "judul")
                    val rawYear = readString(item, "year", "release", "releaseYear", "release_year", "tahun_release", "tahun_pembuatan")
                    val youtubeUrl = readString(item, "video_url", "videoUrl", "youtube_url", "youtubeUrl", "movie_url", "movieUrl")
                    val videoId = readString(item, "video_id", "videoId")
                    val thumbnailUrl = readString(item, "thumbnail_url", "thumbnailUrl", "cover_url", "coverUrl", "poster_url", "posterUrl")
                    val status = readString(item, "status").ifBlank { "approved" }
                    if (!status.equals("approved", ignoreCase = true)) continue
                    activeCount++

                    val rawId = readString(item, "id", "film_id", "movie_id", "song_id")
                    val createdAt = readString(item, "created_at", "createdAt", "uploaded_at", "uploadedAt", "updated_at")
                    val kategori = readString(item, "category", "genre", "kategori")
                    val resolvedId = rawId.ifBlank {
                        listOf(title, rawYear, createdAt, i.toString()).joinToString("#")
                    }
                    val resolvedYoutubeUrl = youtubeUrl.ifBlank {
                        if (videoId.isNotBlank()) "https://www.youtube.com/watch?v=$videoId" else ""
                    }
                    val year = rawYear.ifBlank { yearFromCreatedAt(createdAt) }
                    val synopsis = readString(item, "synopsis", "description", "deskripsi", "desc", "notes").ifBlank {
                        if (resolvedYoutubeUrl.isNotBlank()) {
                            fetchYoutubeMetadataFromBackend(resolvedYoutubeUrl).description.ifBlank {
                                fetchYoutubeDescriptionFromYoutube(resolvedYoutubeUrl, videoId)
                            }
                        } else {
                            ""
                        }
                    }
                    val resolvedThumbnail = thumbnailUrl.ifBlank { youtubeThumbnailUrl(videoId) }

                    Log.d(TAG, "Film from API title=$title youtube_url=$resolvedYoutubeUrl thumbnail_url=$resolvedThumbnail")

                    add(
                        Song(
                            id = resolvedId,
                            title = title,
                            year = year,
                            synopsis = synopsis,
                            youtubeUrl = resolvedYoutubeUrl,
                            videoId = videoId,
                            thumbnailUrl = resolvedThumbnail,
                            createdAt = createdAt,
                            status = status,
                            views = readString(item, "views").toIntOrNull() ?: 0,
                            likes = readString(item, "likes").toIntOrNull() ?: 0,
                            alreadyLiked = item.optBoolean("already_liked", false),
                            artist = kategori.ifBlank { year.ifBlank { "Live" } },
                            genre = kategori.ifBlank { year.ifBlank { "Live" } },
                            category = kategori,
                            description = synopsis,
                            coverUrl = resolvedThumbnail,
                            movieUrl = resolvedYoutubeUrl,
                            audioUrl = resolvedYoutubeUrl,
                            uploadedAt = createdAt,
                            releaseYear = year
                        )
                    )
                }
            }
                .filter { it.title.isNotBlank() }
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<Song> { parseCreatedAtMillis(it.createdAt.ifBlank { it.uploadedAt }) }
                        .thenBy { it.title.lowercase(Locale.US) }
                )

            Log.d(TAG, "Active films after status filter: $activeCount")
            Log.d(TAG, "Films after validation/dedupe/sort: ${songs.size}")
            val channelVideos = runCatching { fetchSchoolChannelVideos() }.getOrDefault(emptyList())
            val channelById = channelVideos.associateBy { it.videoId.ifBlank { it.id } }
            val combinedSongs = (songs + channelVideos)
                .groupBy { it.videoId.ifBlank { it.id } }
                .map { (id, versions) ->
                    val selected = versions.first()
                    val fromChannel = channelById[id]
                    selected.copy(channelCategories = (selected.channelCategories +
                        listOfNotNull(selected.category.takeIf { it.isNotBlank() }) +
                        fromChannel?.channelCategories.orEmpty()).distinct())
                }
                .sortedWith(compareByDescending<Song> { parseCreatedAtMillis(it.createdAt.ifBlank { it.uploadedAt }) }
                    .thenBy { it.title.lowercase(Locale.US) })
            cachedSongs = combinedSongs
            lastFetchAtMs = now
            cachedLikeUserId = normalizedLikeUserId
            combinedSongs
        }.recoverCatching { error ->
            Log.w(TAG, "School backend unavailable; using channel feed.", error)
            fetchSchoolChannelVideos()
        }
    }

    fun getGenres(songs: List<Song>): List<String> = listOf("Semua") + songs.flatMap {
        it.channelCategories.ifEmpty { listOf(it.category) }
    }.filter { it.isNotBlank() }.distinct().sorted()
    fun getArtists(songs: List<Song>): List<String> = songs.map { it.year }.filter { it.isNotBlank() }.distinct().sortedDescending()

    fun getFallbackSongs(): List<Song> = SCHOOL_YOUTUBE_CATALOG.map { item ->
            val url = "https://www.youtube.com/watch?v=${item.id}"
            val thumbnail = youtubeThumbnailUrl(item.id)
            Song(id = item.id, title = item.title, year = item.publishedAt.take(4),
                youtubeUrl = url, videoId = item.id, thumbnailUrl = thumbnail,
                coverUrl = thumbnail, movieUrl = url, createdAt = item.publishedAt,
                uploadedAt = item.publishedAt, status = "approved",
                category = item.categories.firstOrNull() ?: "Video Channel",
                channelCategories = item.categories.ifEmpty { listOf("Video Channel") })
        }

    private suspend fun fetchSchoolChannelVideos(): List<Song> = coroutineScope {
        val sources = listOf("Video Terbaru" to "channel_id=$SCHOOL_YOUTUBE_CHANNEL_ID") +
            SCHOOL_YOUTUBE_PLAYLISTS.map { (name, id) -> name to "playlist_id=$id" }
        val batches = sources.map { (category, query) ->
            async(Dispatchers.IO) {
                runCatching { fetchYoutubeFeed(query, category) }
                    .onFailure { Log.w(TAG, "YouTube feed $category gagal dimuat", it) }
                    .getOrDefault(emptyList())
            }
        }.awaitAll()
        val bundledVideos = getFallbackSongs()
        val grouped = (batches.flatten() + bundledVideos).groupBy { it.videoId }
        grouped.values.map { variants ->
            val first = variants.first()
            val allCategories = variants.flatMap { it.channelCategories.ifEmpty { listOf(it.category) } }.distinct()
            val playlistCategories = allCategories.filter { it != "Video Terbaru" && it != "Video Channel" }
            first.copy(category = playlistCategories.firstOrNull() ?: allCategories.firstOrNull().orEmpty(),
                channelCategories = allCategories)
        }.sortedWith(compareByDescending<Song> { parseCreatedAtMillis(it.createdAt) }
            .thenBy { it.title.lowercase(Locale.US) })
    }

    private fun fetchYoutubeFeed(query: String, category: String): List<Song> {
        val connection = (URL("https://www.youtube.com/feeds/videos.xml?$query")
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("Video sekolah gagal dimuat.")
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            val entries = factory.newDocumentBuilder().parse(connection.inputStream)
                .getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry")
            return (0 until entries.length).mapNotNull { index ->
                val entry = entries.item(index) as? org.w3c.dom.Element ?: return@mapNotNull null
                val videoId = entry.getElementsByTagNameNS("http://www.youtube.com/xml/schemas/2015", "videoId")
                    .item(0)?.textContent.orEmpty()
                val title = entry.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title")
                    .item(0)?.textContent.orEmpty()
                val published = entry.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "published")
                    .item(0)?.textContent.orEmpty()
                if (videoId.isBlank() || title.isBlank()) return@mapNotNull null
                val url = "https://www.youtube.com/watch?v=$videoId"
                val thumbnail = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                Song(id = videoId, title = title, year = published.take(4), youtubeUrl = url,
                    videoId = videoId, thumbnailUrl = thumbnail, coverUrl = thumbnail,
                    movieUrl = url, createdAt = published, uploadedAt = published,
                    status = "approved", category = category)
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun updateFilmLike(filmId: String, username: String): Result<FilmLikeResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (filmId.trim().isBlank()) throw IllegalStateException("film_id kosong.")
            if (username.trim().isBlank()) {
                throw IllegalStateException("Login diperlukan untuk Like.")
            }
            val connection = (URL(SONGS_ENDPOINT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val body = JSONObject().apply {
                put("type", "film_like")
                put("film_id", filmId)
                put("userId", username)
                put("username", username)
            }.toString()
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            if (responseBody.trimStart().startsWith("<")) {
                throw IllegalStateException("Endpoint backend mengembalikan HTML, bukan JSON. Redeploy Apps Script dan pastikan URL web app masih aktif.")
            }
            val responseJson = JSONObject(responseBody)
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Gagal update like." })
            }
            cachedSongs = emptyList()
            lastFetchAtMs = 0L
            FilmLikeResult(
                totalLikes = responseJson.optInt("total_likes", responseJson.optInt("likes", 0)),
                alreadyLiked = responseJson.optBoolean("already_liked", true)
            )
        }
    }

    suspend fun updateFilmView(filmId: String?, movieUrl: String?): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(SONGS_ENDPOINT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val body = JSONObject().apply {
                put("type", "film_view")
                if (!filmId.isNullOrBlank()) put("film_id", filmId)
                if (!movieUrl.isNullOrBlank()) {
                    put("video_url", movieUrl)
                    put("videoUrl", movieUrl)
                    put("youtube_url", movieUrl)
                    put("youtubeUrl", movieUrl)
                    put("movie_url", movieUrl)
                }
            }.toString()
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            if (responseBody.trimStart().startsWith("<")) {
                throw IllegalStateException("Endpoint backend mengembalikan HTML, bukan JSON. Redeploy Apps Script dan pastikan URL web app masih aktif.")
            }
            val responseJson = JSONObject(responseBody)
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Gagal update view." })
            }
            responseJson.optInt("views", 0)
        }
    }

    suspend fun updateFilmMetadata(
        filmId: String,
        username: String,
        title: String,
        year: String,
        deskripsi: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (filmId.trim().isBlank()) throw IllegalStateException("film_id kosong.")
            if (username.trim().isBlank()) throw IllegalStateException("Login admin diperlukan.")
            if (title.trim().isBlank()) throw IllegalStateException("Judul wajib diisi.")

            val connection = (URL(SONGS_ENDPOINT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val body = JSONObject().apply {
                put("type", "film_update")
                put("film_id", filmId)
                put("username", username)
                put("title", title)
                put("year", year)
                put("deskripsi", deskripsi)
            }.toString()
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            if (responseBody.trimStart().startsWith("<")) {
                throw IllegalStateException("Endpoint backend mengembalikan HTML, bukan JSON. Redeploy Apps Script dan pastikan URL web app masih aktif.")
            }
            val responseJson = JSONObject(responseBody)
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Gagal update metadata film." })
            }
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal update metadata film.")
        }
    }
    suspend fun deleteFilm(
        filmId: String,
        username: String,
        videoId: String = "",
        videoUrl: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (filmId.trim().isBlank()) throw IllegalStateException("film_id kosong.")
            if (username.trim().isBlank()) throw IllegalStateException("Login admin diperlukan.")

            val connection = (URL(SONGS_ENDPOINT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val body = JSONObject().apply {
                put("type", "film_delete")
                put("film_id", filmId)
                put("movie_id", filmId)
                put("id", filmId)
                put("username", username)
                if (videoId.isNotBlank()) {
                    put("video_id", videoId)
                    put("videoId", videoId)
                }
                if (videoUrl.isNotBlank()) {
                    put("video_url", videoUrl)
                    put("videoUrl", videoUrl)
                    put("youtube_url", videoUrl)
                    put("youtubeUrl", videoUrl)
                    put("movie_url", videoUrl)
                }
            }.toString()
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            if (responseBody.trimStart().startsWith("<")) {
                throw IllegalStateException("Endpoint backend mengembalikan HTML, bukan JSON. Redeploy Apps Script dan pastikan URL web app masih aktif.")
            }
            val responseJson = JSONObject(responseBody)
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Gagal menghapus film." })
            }
            cachedSongs = emptyList()
            lastFetchAtMs = 0L
        }.recoverCatching {
            throw IllegalStateException(it.message ?: "Gagal menghapus film.")
        }
    }

    companion object {
        private const val TAG = "ApiSongRepository"
        private const val CACHE_TTL_MS = 60_000L
    }
}

data class FilmLikeResult(
    val totalLikes: Int,
    val alreadyLiked: Boolean
)

private fun readString(item: JSONObject, vararg keys: String): String {
    keys.forEach { key ->
        if (item.has(key)) {
            val value = item.opt(key)
            if (value != null && value != JSONObject.NULL) {
                val text = value.toString().trim()
                if (text.isNotBlank()) return text
            }
        }
    }
    return ""
}

private fun parseCreatedAtMillis(value: String): Long {
    if (value.isBlank()) return Long.MIN_VALUE
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(value)?.time
        }.getOrNull()
    } ?: Long.MIN_VALUE
}

private fun youtubeThumbnailUrl(videoId: String): String {
    return if (videoId.isBlank()) "" else "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
}

private fun fetchYoutubeMetadataFromBackend(youtubeUrl: String): YoutubeMetaFallback {
    val normalizedUrl = youtubeUrl.trim()
    if (normalizedUrl.isBlank()) return YoutubeMetaFallback()

    return runCatching {
        val requestUrl = buildString {
            append(SONGS_ENDPOINT_URL)
            append("?type=youtube_meta&url=")
            append(java.net.URLEncoder.encode(normalizedUrl, "UTF-8"))
            append("&ts=")
            append(System.currentTimeMillis())
        }
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
        }
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            return@runCatching YoutubeMetaFallback()
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        if (body.trimStart().startsWith("<")) return@runCatching YoutubeMetaFallback()
        val root = JSONObject(body)
        YoutubeMetaFallback(
            title = readString(root, "title"),
            description = readString(root, "description"),
            thumbnailUrl = readString(root, "thumbnail_url", "thumbnailUrl")
        )
    }.getOrElse { YoutubeMetaFallback() }
}

private fun fetchYoutubeDescriptionFromYoutube(youtubeUrl: String, videoId: String): String {
    val normalizedUrl = youtubeUrl.trim().ifBlank {
        if (videoId.isBlank()) "" else "https://www.youtube.com/watch?v=$videoId"
    }
    if (normalizedUrl.isBlank()) return ""

    return runCatching {
        val pageUrl = if (normalizedUrl.contains("?")) "$normalizedUrl&hl=id" else "$normalizedUrl?hl=id"
        val connection = (URL(pageUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            useCaches = false
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
        }
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            return@runCatching ""
        }
        val html = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val metaDescription = Regex("""<meta[^>]+property=["']og:description["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
        decodeHtmlEntities(metaDescription.trim())
    }.getOrDefault("")
}

private fun yearFromCreatedAt(createdAt: String): String {
    val millis = parseCreatedAtMillis(createdAt)
    if (millis == Long.MIN_VALUE) return ""
    return runCatching {
        java.text.SimpleDateFormat("yyyy", Locale.US).format(Date(millis))
    }.getOrDefault("")
}

private fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

private data class YoutubeMetaFallback(
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = ""
)

