package com.mugarutv.data.repository

import com.mugarutv.data.model.SongRegistrationPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class SongRegistrationRepository {
    companion object {
        const val GOOGLE_APPS_SCRIPT_ENDPOINT = SCHOOL_APPS_SCRIPT_ENDPOINT
        private const val NETWORK_TIMEOUT_MS = 90_000
    }

    suspend fun submitRegistration(payload: SongRegistrationPayload): Result<Unit> = withContext(Dispatchers.IO) {
        if (!GOOGLE_APPS_SCRIPT_ENDPOINT.startsWith("http")) {
            return@withContext Result.failure(IllegalStateException("Endpoint Google Apps Script belum diatur."))
        }

        runCatching {
            val connection = (URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject().apply {
                put("type", "live_registration")
                put("title", payload.title)
                put("year", payload.year)
                put("deskripsi", payload.synopsis)
                put("synopsis", payload.synopsis)
                put("kategori", payload.kategori)
                put("category", payload.kategori)
                put("video_url", payload.youtubeUrl)
                put("videoUrl", payload.youtubeUrl)
                put("youtube_url", payload.youtubeUrl)
                put("youtubeUrl", payload.youtubeUrl)
                put("live_url", payload.youtubeUrl)
                put("liveUrl", payload.youtubeUrl)
                put("stream_url", payload.youtubeUrl)
                put("streamUrl", payload.youtubeUrl)
                put("upload_link", payload.youtubeUrl)
                put("link_upload", payload.youtubeUrl)
                put("copyrightAgreement", payload.copyrightAgreement)
                if (!payload.coverImageBase64.isNullOrBlank()) {
                    put("coverImageBase64", payload.coverImageBase64)
                    put("coverImageFileName", payload.coverImageFileName.orEmpty())
                    put("coverImageMimeType", payload.coverImageMimeType.orEmpty())
                }
            }.toString()

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                throw IllegalStateException("Gagal kirim data (HTTP $responseCode). $errorBody")
            }
            val responseBody = connection.inputStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (responseBody.trimStart().startsWith("<")) {
                throw IllegalStateException("Endpoint backend mengembalikan HTML, bukan JSON. Redeploy Apps Script dan pastikan Web App aktif.")
            }
            val responseJson = if (responseBody.isNotBlank()) JSONObject(responseBody) else JSONObject()
            val success = responseJson.optBoolean("success", false)

            if (!success) {
                val message = responseJson.optString("message").ifBlank {
                    "Registrasi film gagal diproses."
                }
                throw IllegalStateException(message)
            }
            connection.disconnect()
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.message ?: "Terjadi kesalahan saat mengirim registrasi.")
        }
    }

    suspend fun importYoutubeChannel(channelUrl: String): Result<ChannelImportResult> = withContext(Dispatchers.IO) {
        if (!GOOGLE_APPS_SCRIPT_ENDPOINT.startsWith("http")) {
            return@withContext Result.failure(IllegalStateException("Endpoint Google Apps Script belum diatur."))
        }

        runCatching {
            val normalizedUrl = channelUrl.trim()
            if (normalizedUrl.isBlank()) throw IllegalStateException("Link channel wajib diisi.")

            val connection = (URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject().apply {
                put("type", "channel_import")
                put("channel_url", normalizedUrl)
                put("channelUrl", normalizedUrl)
                put("kategori", "YouTube")
                put("category", "YouTube")
            }.toString()

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use(BufferedReader::readText)
                .orEmpty()
            connection.disconnect()

            if (responseCode !in 200..299) {
                throw IllegalStateException("Gagal import channel (HTTP $responseCode). $responseBody")
            }
            if (responseBody.trimStart().startsWith("<")) {
                throw IllegalStateException("Endpoint backend mengembalikan HTML, bukan JSON. Redeploy Apps Script dan pastikan Web App aktif.")
            }

            val responseJson = if (responseBody.isNotBlank()) JSONObject(responseBody) else JSONObject()
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Import channel gagal diproses." })
            }

            ChannelImportResult(
                imported = responseJson.optInt("imported", 0),
                skipped = responseJson.optInt("skipped", 0),
                total = responseJson.optInt("total", 0),
                message = responseJson.optString("message").ifBlank { "Import channel selesai." }
            )
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.message ?: "Terjadi kesalahan saat import channel.")
        }
    }
    suspend fun updateActiveLiveStream(liveUrl: String, username: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!GOOGLE_APPS_SCRIPT_ENDPOINT.startsWith("http")) {
            return@withContext Result.failure(IllegalStateException("Endpoint Google Apps Script belum diatur."))
        }

        runCatching {
            val normalizedUrl = liveUrl.trim()
            val normalizedUsername = username.trim()
            if (normalizedUrl.isBlank()) throw IllegalStateException("Link live wajib diisi.")
            if (normalizedUsername.isBlank()) throw IllegalStateException("Login admin diperlukan.")

            val connection = (URL(GOOGLE_APPS_SCRIPT_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject().apply {
                put("type", "live_stream_update")
                put("username", normalizedUsername)
                put("userId", normalizedUsername)
                put("title", "Live Streaming")
                put("kategori", "Live")
                put("category", "Live")
                put("video_url", normalizedUrl)
                put("videoUrl", normalizedUrl)
                put("youtube_url", normalizedUrl)
                put("youtubeUrl", normalizedUrl)
                put("live_url", normalizedUrl)
                put("liveUrl", normalizedUrl)
                put("stream_url", normalizedUrl)
                put("streamUrl", normalizedUrl)
                put("upload_link", normalizedUrl)
                put("link_upload", normalizedUrl)
            }.toString()

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use(BufferedReader::readText)
                .orEmpty()
            connection.disconnect()

            if (responseCode !in 200..299) {
                throw IllegalStateException("Gagal simpan live (HTTP $responseCode). $responseBody")
            }
            if (responseBody.trimStart().startsWith("<")) {
                throw IllegalStateException("Endpoint backend mengembalikan HTML, bukan JSON. Redeploy Apps Script dan pastikan Web App aktif.")
            }

            val responseJson = if (responseBody.isNotBlank()) JSONObject(responseBody) else JSONObject()
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Live gagal disimpan." })
            }
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.message ?: "Terjadi kesalahan saat menyimpan live.")
        }
    }
    suspend fun fetchYoutubeMetadata(youtubeUrl: String): Result<YoutubeMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUrl = youtubeUrl.trim()
            if (normalizedUrl.isBlank()) throw IllegalStateException("Link YouTube kosong.")

            val requestUrl = buildString {
                append(GOOGLE_APPS_SCRIPT_ENDPOINT)
                append("?type=youtube_meta")
                append("&url=")
                append(java.net.URLEncoder.encode(normalizedUrl, "UTF-8"))
            }

            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use(BufferedReader::readText)
                .orEmpty()
            connection.disconnect()

            if (responseCode !in 200..299) {
                throw IllegalStateException("Gagal mengambil metadata YouTube (HTTP $responseCode). $responseBody")
            }

            if (responseBody.trimStart().startsWith("<")) {
                throw IllegalStateException("Endpoint backend mengembalikan HTML, bukan JSON. Redeploy Apps Script dan pastikan Web App aktif.")
            }
            val responseJson = if (responseBody.isNotBlank()) JSONObject(responseBody) else JSONObject()
            if (!responseJson.optBoolean("success", false)) {
                throw IllegalStateException(responseJson.optString("message").ifBlank { "Metadata YouTube tidak tersedia." })
            }

            YoutubeMetadata(
                title = responseJson.optString("title"),
                description = responseJson.optString("description"),
                thumbnailUrl = responseJson.optString("thumbnail_url").ifBlank { responseJson.optString("thumbnailUrl") },
                videoId = responseJson.optString("video_id").ifBlank { responseJson.optString("videoId") }
            )
        }
    }
}

data class ChannelImportResult(
    val imported: Int = 0,
    val skipped: Int = 0,
    val total: Int = 0,
    val message: String = ""
)
data class YoutubeMetadata(
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val videoId: String = ""
)

