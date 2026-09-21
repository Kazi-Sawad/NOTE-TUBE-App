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
    val isRecognized: Boolean,
    val descriptionOrTranscript: String = ""
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
        var scrapedTitle = ""
        var scrapedChannel = ""

        // 1. Primary: Try YouTube oEmbed API for official video title & creator name
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
                        scrapedTitle = rawTitle
                        scrapedChannel = if (rawChannel.isNotBlank()) rawChannel else "YouTube Creator"

                        // Also attempt to fetch video subtitles/captions or page description snippet
                        val captionsOrDescription = fetchVideoCaptionsSnippet(videoId)

                        return@withContext YouTubeMetadata(
                            videoId = videoId,
                            canonicalUrl = canonicalUrl,
                            title = scrapedTitle,
                            channel = scrapedChannel,
                            thumbnailUrl = thumbnail,
                            isRecognized = true,
                            descriptionOrTranscript = captionsOrDescription
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Handled gracefully by falling back to page scraper
        }

        // 2. Secondary: Fallback to HTML meta tags scraping if oEmbed fails or is blocked
        try {
            val pageRequest = Request.Builder()
                .url(canonicalUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .build()

            val pageResponse = okHttpClient.newCall(pageRequest).execute()
            if (pageResponse.isSuccessful) {
                val html = pageResponse.body?.string() ?: ""
                val titlePattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE)
                val matcher = titlePattern.matcher(html)
                if (matcher.find()) {
                    val raw = matcher.group(1)?.replace("- YouTube", "")?.trim() ?: ""
                    if (raw.isNotBlank() && raw != "YouTube") {
                        scrapedTitle = raw
                    }
                }

                val authorPattern = Pattern.compile("<link itemprop=\"name\" content=\"(.*?)\">")
                val authorMatcher = authorPattern.matcher(html)
                if (authorMatcher.find()) {
                    scrapedChannel = authorMatcher.group(1)?.trim() ?: ""
                }

                if (scrapedTitle.isNotBlank()) {
                    val captions = fetchVideoCaptionsSnippet(videoId)
                    return@withContext YouTubeMetadata(
                        videoId = videoId,
                        canonicalUrl = canonicalUrl,
                        title = scrapedTitle,
                        channel = if (scrapedChannel.isNotBlank()) scrapedChannel else "YouTube Creator",
                        thumbnailUrl = defaultThumbnail,
                        isRecognized = true,
                        descriptionOrTranscript = captions
                    )
                }
            }
        } catch (_: Exception) {
            // Handled gracefully below
        }

        // 3. Fallback when network is offline or video is restricted
        YouTubeMetadata(
            videoId = videoId,
            canonicalUrl = canonicalUrl,
            title = scrapedTitle,
            channel = scrapedChannel,
            thumbnailUrl = defaultThumbnail,
            isRecognized = scrapedTitle.isNotBlank(),
            descriptionOrTranscript = ""
        )
    }

    /**
     * Attempts to fetch real transcript / timed captions lines from YouTube's timedtext endpoint.
     */
    private fun fetchVideoCaptionsSnippet(videoId: String): String {
        return try {
            val timedTextUrl = "https://www.youtube.com/api/timedtext?lang=en&v=$videoId"
            val request = Request.Builder()
                .url(timedTextUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val xml = response.body?.string() ?: ""
                if (xml.contains("<text")) {
                    val pattern = Pattern.compile("<text[^>]*>(.*?)</text>")
                    val matcher = pattern.matcher(xml)
                    val lines = mutableListOf<String>()
                    var count = 0
                    while (matcher.find() && count < 60) {
                        val text = matcher.group(1)
                            ?.replace("&amp;", "&")
                            ?.replace("&quot;", "\"")
                            ?.replace("&#39;", "'")
                            ?.replace("&lt;", "<")
                            ?.replace("&gt;", ">")
                            ?.trim()
                        if (!text.isNullOrBlank()) {
                            lines.add(text)
                            count++
                        }
                    }
                    if (lines.isNotEmpty()) {
                        lines.joinToString(" ")
                    } else ""
                } else ""
            } else ""
        } catch (_: Exception) {
            ""
        }
    }
}
