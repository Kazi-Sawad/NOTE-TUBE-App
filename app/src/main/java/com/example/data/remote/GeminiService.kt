package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GeneratedNoteResult
import com.example.data.model.StructuredSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun generateVideoNotes(
        videoUrl: String,
        videoId: String,
        videoTitle: String,
        channelName: String,
        userProvidedContext: String = ""
    ): GeneratedNoteResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "GEMINI_API_KEY is not configured. Using in-depth offline synthesis engine.")
            return@withContext OfflineNotesGenerator.generateInDepthOfflineNotes(
                videoId = videoId,
                rawTitle = videoTitle,
                channel = channelName,
                userContext = userProvidedContext
            )
        }

        // Broad, comprehensive, in-depth prompt directing Gemini to unpack rich conceptual frameworks
        val prompt = buildString {
            append("You are NoteTube AI, a master educational analyst and executive knowledge synthesizer.\n")
            append("Perform an exhaustive, broad, in-depth analysis of this YouTube video:\n")
            append("- Video URL: $videoUrl\n")
            append("- YouTube Video ID: $videoId\n")
            if (videoTitle.isNotBlank()) {
                append("- Video Title: $videoTitle\n")
            }
            if (channelName.isNotBlank()) {
                append("- Creator / Channel: $channelName\n")
            }
            if (userProvidedContext.isNotBlank()) {
                append("- User Focus / Topic / Transcript Snippet:\n$userProvidedContext\n")
            }

            append("\nANALYSIS REQUIREMENTS (BROAD & IN-DEPTH WITH CODE & MATH FOCUS):\n")
            append("1. PROGRAMMING & CODE EXTRACTION (CRITICAL):\n")
            append("   - If the video involves programming, coding, software development, or algorithms: you MUST extract the actual programs, scripts, or code snippets discussed, demonstrated, or written in the video.\n")
            append("   - Provide clean, runnable code blocks with comments explaining line-by-line mechanics. Include language identifier (e.g. ```python, ```javascript, ```kotlin, ```cpp, ```sql).\n")
            append("   - Explain the underlying programming concepts: data structures, time/space complexity (Big O), design patterns, and debugging tips.\n")
            append("2. MATHEMATICS & FORMULAS EXTRACTION (CRITICAL):\n")
            append("   - If the video involves mathematics, statistics, calculus, linear algebra, or physics: you MUST extract and write out the mathematical equations, theorems, formulas, and derivations (e.g., f(x) = ..., matrices, integrals).\n")
            append("   - Explain the intuitive geometric or physical meaning of every variable and formula step-by-step.\n")
            append("3. IN-DEPTH CONCEPTUAL EXPLANATION:\n")
            append("   - Thoroughly explain *how* and *why* the concepts work. Avoid superficial summaries. Bridge the gap between high-level theory and implementation.\n")
            append("4. BROAD STRUCTURED SECTIONS: Provide at least 4-5 detailed sections under structuredBullets. Include dedicated sections like 'Source Code & Implementation Mechanics' or 'Mathematical Formulation & Proofs' whenever applicable.\n")
            append("5. ACCURATE IDENTIFICATION: Identify the real video, its syllabus, instructor, and precise technologies/theorems taught.\n")
            append("6. COGNITIVE RETENTION: Formulate 4-6 rich key takeaways (mental models & programming/math axioms).\n")
            append("7. ACTIONABLE ROADMAP: Provide practical coding exercises, mathematical problem sets, or terminal commands to run.\n")
            append("\nReturn strictly valid JSON with this exact schema:\n")
            append("{\n")
            append("  \"title\": \"Accurate and descriptive video title\",\n")
            append("  \"channel\": \"Real creator or channel name\",\n")
            append("  \"category\": \"Category name (e.g. Technology, Productivity, Science, Business, Education, Design)\",\n")
            append("  \"summary\": \"A thorough, multi-sentence executive summary detailing the primary thesis, problem space, and ultimate conclusions of the video.\",\n")
            append("  \"keyTakeaways\": [\n")
            append("    \"First in-depth takeaway explaining a core conceptual paradigm or principle.\",\n")
            append("    \"Second takeaway breaking down practical methodologies and key insights.\",\n")
            append("    \"Third takeaway exploring trade-offs, nuances, or counter-intuitive findings.\",\n")
            append("    \"Fourth takeaway framing the overarching system or lifelong application.\"\n")
            append("  ],\n")
            append("  \"structuredBullets\": [\n")
            append("    {\n")
            append("      \"heading\": \"1. Conceptual Foundations & Problem Space\",\n")
            append("      \"points\": [\n")
            append("        \"Detailed point explaining the historical or industry context and core motivations.\",\n")
            append("        \"Deep dive into foundational definitions, theoretical axioms, and baseline assumptions.\",\n")
            append("        \"Critical analysis of common misconceptions and why traditional approaches fail.\"\n")
            append("      ]\n")
            append("    },\n")
            append("    {\n")
            append("      \"heading\": \"2. Architecture, Mechanics & Core Methodology\",\n")
            append("      \"points\": [\n")
            append("        \"Exhaustive explanation of primary workflows, algorithms, or practical techniques.\",\n")
            append("        \"Step-by-step breakdown of how key components interact and compound.\",\n")
            append("        \"Specific real-world examples, case studies, or code/execution illustrations discussed in the video.\"\n")
            append("      ]\n")
            append("    },\n")
            append("    {\n")
            append("      \"heading\": \"3. Advanced Nuances, Pitfalls & Optimization\",\n")
            append("      \"points\": [\n")
            append("        \"Detailed treatment of edge cases, failure states, and performance trade-offs.\",\n")
            append("        \"Nuanced comparisons between competing paradigms or alternative strategies.\",\n")
            append("        \"Expert diagnostics on how to measure progress and troubleshoot issues effectively.\"\n")
            append("      ]\n")
            append("    },\n")
            append("    {\n")
            append("      \"heading\": \"4. Synthesis & Long-Term Integration\",\n")
            append("      \"points\": [\n")
            append("        \"Connecting the curriculum to broader trends, future evolutions, and adjacent disciplines.\",\n")
            append("        \"Sustainable practices to maintain and scale these capabilities over time.\",\n")
            append("        \"Key philosophical or mindset shift required for true mastery.\"\n")
            append("      ]\n")
            append("    }\n")
            append("  ],\n")
            append("  \"actionItems\": [\n")
            append("    \"Concrete 15-minute hands-on drill to implement the most critical concept right away.\",\n")
            append("    \"Specific workflow or environmental modification to reduce friction in daily practice.\",\n")
            append("    \"Long-term project milestone or review exercise to solidify the learnings.\"\n")
            append("  ]\n")
            append("}\n")
        }

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.2)
                put("topP", 0.95)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", generationConfig)
        }

        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error HTTP ${response.code}: $responseBody. Falling back to in-depth offline synthesis.")
                return@withContext OfflineNotesGenerator.generateInDepthOfflineNotes(videoId, videoTitle, channelName, userProvidedContext)
            }

            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (rawText.isBlank()) {
                return@withContext OfflineNotesGenerator.generateInDepthOfflineNotes(videoId, videoTitle, channelName, userProvidedContext)
            }

            return@withContext parseResponseJson(rawText, videoId, videoTitle, channelName, userProvidedContext)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API: ${e.message}. Using offline synthesis.", e)
            return@withContext OfflineNotesGenerator.generateInDepthOfflineNotes(videoId, videoTitle, channelName, userProvidedContext)
        }
    }

    private fun parseResponseJson(
        rawText: String,
        videoId: String,
        defaultTitle: String,
        defaultChannel: String,
        userContext: String
    ): GeneratedNoteResult {
        var cleaned = rawText.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }

        try {
            val root = JSONObject(cleaned)
            val parsedTitle = root.optString("title").trim()
            val title = if (parsedTitle.isNotBlank()) parsedTitle else defaultTitle.ifBlank { "Comprehensive Video Analysis" }

            val parsedChannel = root.optString("channel").trim()
            val channel = if (parsedChannel.isNotBlank()) parsedChannel else defaultChannel.ifBlank { "Subject Masterclass" }

            val category = root.optString("category", "Technology")
            val summary = root.optString("summary", "In-depth conceptual study and structured review of key principles.")

            val keyTakeawaysList = mutableListOf<String>()
            val takeawaysArray = root.optJSONArray("keyTakeaways")
            if (takeawaysArray != null) {
                for (i in 0 until takeawaysArray.length()) {
                    val item = takeawaysArray.optString(i).trim()
                    if (item.isNotBlank()) keyTakeawaysList.add(item)
                }
            }

            val structuredSections = mutableListOf<StructuredSection>()
            val bulletsArray = root.optJSONArray("structuredBullets")
            if (bulletsArray != null) {
                for (i in 0 until bulletsArray.length()) {
                    val secObj = bulletsArray.optJSONObject(i)
                    if (secObj != null) {
                        val heading = secObj.optString("heading", "Core Analysis").trim()
                        val pointsList = mutableListOf<String>()
                        val pointsArr = secObj.optJSONArray("points")
                        if (pointsArr != null) {
                            for (j in 0 until pointsArr.length()) {
                                val p = pointsArr.optString(j).trim()
                                if (p.isNotBlank()) pointsList.add(p)
                            }
                        }
                        if (pointsList.isNotEmpty()) {
                            structuredSections.add(StructuredSection(heading, pointsList))
                        }
                    }
                }
            }

            val actionItemsList = mutableListOf<String>()
            val actionsArray = root.optJSONArray("actionItems")
            if (actionsArray != null) {
                for (i in 0 until actionsArray.length()) {
                    val item = actionsArray.optString(i).trim()
                    if (item.isNotBlank()) actionItemsList.add(item)
                }
            }

            return GeneratedNoteResult(
                title = title,
                channel = channel,
                category = category,
                summary = summary,
                keyTakeaways = if (keyTakeawaysList.isNotEmpty()) keyTakeawaysList else listOf("Gained deep conceptual understanding of $title."),
                structuredBullets = if (structuredSections.isNotEmpty()) structuredSections else listOf(StructuredSection("1. Key Principles", listOf("Mastered core methodologies and architectural models."))),
                actionItems = if (actionItemsList.isNotEmpty()) actionItemsList else listOf("Apply high-leverage principles in a focused practical session.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response JSON: ${e.message}", e)
            return OfflineNotesGenerator.generateInDepthOfflineNotes(videoId, defaultTitle, defaultChannel, userContext)
        }
    }
}
