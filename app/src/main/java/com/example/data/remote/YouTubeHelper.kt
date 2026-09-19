package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class YouTubeMetadata(
    val videoId: String,
    val canonicalUrl: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val isRecognized: Boolean
)

object YouTubeHelper {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Robust regular expression matching all standard YouTube URL formats:
    // https://www.youtube.com/watch?v=VIDEO_ID
    // https://youtu.be/VIDEO_ID
    // https://m.youtube.com/watch?v=VIDEO_ID
    // https://www.youtube.com/shorts/VIDEO_ID
    // https://www.youtube.com/live/VIDEO_ID
    // https://www.youtube.com/embed/VIDEO_ID
    // or bare 11-character video ID
    private val urlPatterns = listOf(
        Pattern.compile("(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=|shorts\\/|live\\/))([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^[a-zA-Z0-9_-]{11}$")
    )

    fun extractVideoId(urlOrText: String): String? {
        val trimmed = urlOrText.trim()
        if (trimmed.isEmpty()) return null

        // 1. Check if raw input is an 11-character ID
        if (trimmed.length == 11 && trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }

        // 2. Search for 11-character ID within standard YouTube URL structures
        val matcher = urlPatterns[0].matcher(trimmed)
        if (matcher.find()) {
            val candidate = matcher.group(1)
            if (!candidate.isNullOrBlank() && candidate.length == 11) {
                return candidate
            }
        }

        // 3. Fallback check for query param v= in complex URLs
        val queryMatcher = Pattern.compile("[?&]v=([a-zA-Z0-9_-]{11})").matcher(trimmed)
        if (queryMatcher.find()) {
            val candidate = queryMatcher.group(1)
            if (!candidate.isNullOrBlank() && candidate.length == 11) {
                return candidate
            }
        }

        return null
    }

    fun getThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }

    fun getCanonicalUrl(videoId: String): String {
        return "https://www.youtube.com/watch?v=$videoId"
    }

    suspend fun fetchVideoMetadata(videoId: String): YouTubeMetadata = withContext(Dispatchers.IO) {
        val canonicalUrl = getCanonicalUrl(videoId)
        val defaultThumbnail = getThumbnailUrl(videoId)

        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=$canonicalUrl&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val rawTitle = json.optString("title", "").trim()
                    val rawChannel = json.optString("author_name", "").trim()
                    val thumbnail = json.optString("thumbnail_url", defaultThumbnail)

                    if (rawTitle.isNotBlank()) {
                        return@withContext YouTubeMetadata(
                            videoId = videoId,
                            canonicalUrl = canonicalUrl,
                            title = rawTitle,
                            channel = if (rawChannel.isNotBlank()) rawChannel else "YouTube Creator",
                            thumbnailUrl = thumbnail,
                            isRecognized = true
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Handled gracefully below
        }

        // Default metadata when oEmbed is restricted or unavailable;
        // Gemini will identify and ground the actual content using the videoId and URL
        YouTubeMetadata(
            videoId = videoId,
            canonicalUrl = canonicalUrl,
            title = "",
            channel = "",
            thumbnailUrl = defaultThumbnail,
            isRecognized = false
        )
    }
}
