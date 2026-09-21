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

        // Prompt directing Gemini to ground notes in actual video data with warm, clear, humanized writing
        val prompt = buildString {
            append("You are NoteTube AI, an attentive student and expert note-taker who writes clear, engaging, and genuinely humanized study notes.\n")
            append("Analyze this YouTube video using its exact real-world subject, spoken content, transcript, and creator data:\n")
            append("- Video URL: $videoUrl\n")
            append("- YouTube Video ID: $videoId\n")
            if (videoTitle.isNotBlank()) {
                append("- Video Title: $videoTitle\n")
            }
            if (channelName.isNotBlank()) {
                append("- Creator / Channel: $channelName\n")
            }
            if (userProvidedContext.isNotBlank()) {
                append("- Spoken Content / Transcript / User Focus:\n$userProvidedContext\n")
            }

            append("\nCRITICAL INSTRUCTIONS FOR REAL VIDEO GROUNDING & HUMANIZED WRITING:\n")
            append("1. GROUND IN REAL VIDEO DATA:\n")
            append("   - Use the ACTUAL title, creator, topic, and specific examples discussed in this specific video. Do NOT produce generic placeholder filler.\n")
            append("   - Mention the creator by name naturally (e.g., 'As Mosh walks through...', 'Grant Sanderson visualizes...', 'Cal Newport illustrates...').\n")
            append("   - Reference the real analogies, tools, stories, or live demos given in the video.\n")
            append("2. WRITE LIKE A HUMAN (NO ROBOTIC AI-JARGON):\n")
            append("   - Avoid robotic buzzwords like 'systematic deconstruction', 'holistic paradigm', 'synergistic integration', or empty academic fluff.\n")
            append("   - Write in a clean, punchy, conversational yet authoritative tone—like smart, handwritten notes from an honors student or peer who actually watched and understood the lesson.\n")
            append("   - Use active verbs, natural rhythm, clear 'why this matters' framing, and intuitive analogies.\n")
            append("3. CODE & TECH EXTRACTION (IF PROGRAMMING/TECH):\n")
            append("   - Extract the real code, functions, or commands taught in this video.\n")
            append("   - Add friendly inline human comments explaining tricky lines (e.g. '# Watch out: this mutates the original list!').\n")
            append("4. MATH & FORMULAS (IF MATH/SCIENCE):\n")
            append("   - Extract the real equations or formulas with simple, intuitive plain-English translations for each variable.\n")
            append("5. STRUCTURE: At least 4-5 focused sections under structuredBullets. Keep bullet points crisp, insightful, and readable.\n")
            append("\nReturn strictly valid JSON with this exact schema:\n")
            append("{\n")
            append("  \"title\": \"Real, clean video title (e.g., Python Tutorial for Beginners)\",\n")
            append("  \"channel\": \"Real creator name\",\n")
            append("  \"category\": \"Category name (e.g. Technology, Productivity, Science, Business, Education, Design)\",\n")
            append("  \"summary\": \"A 3-4 sentence humanized executive summary that explains what the creator teaches, why they made the video, and the big takeaway in conversational language.\",\n")
            append("  \"keyTakeaways\": [\n")
            append("    \"First key takeaway explained in a clear, punchy sentence.\",\n")
            append("    \"Second practical takeaway detailing a technique or rule of thumb.\",\n")
            append("    \"Third takeaway highlighting a counter-intuitive mistake to avoid.\",\n")
            append("    \"Fourth takeaway showing how to apply this in your own work.\"\n")
            append("  ],\n")
            append("  \"structuredBullets\": [\n")
            append("    {\n")
            append("      \"heading\": \"1. What This Video Is Really About\",\n")
            append("      \"points\": [\n")
            append("        \"Clear explanation of the problem the creator tackles and their unique angle.\",\n")
            append("        \"Key terms and mental models introduced early in the video.\"\n")
            append("      ]\n")
            append("    },\n")
            append("    {\n")
            append("      \"heading\": \"2. Step-by-Step Breakdown & Real Examples\",\n")
            append("      \"points\": [\n")
            append("        \"Walkthrough of the creator's workflow, code snippet, formula, or live demo.\",\n")
            append("        \"Practical explanation of how each step works behind the scenes.\"\n")
            append("      ]\n")
            append("    },\n")
            append("    {\n")
            append("      \"heading\": \"3. Common Traps & Nuances to Watch Out For\",\n")
            append("      \"points\": [\n")
            append("        \"Mistakes beginners often make when trying this out.\",\n")
            append("        \"Subtle edge cases or performance caveats highlighted by the presenter.\"\n")
            append("      ]\n")
            append("    },\n")
            append("    {\n")
            append("      \"heading\": \"4. Putting It Into Practice\",\n")
            append("      \"points\": [\n")
            append("        \"How to apply these principles immediately in real life or projects.\",\n")
            append("        \"The creator's recommended next steps or practice exercises.\"\n")
            append("      ]\n")
            append("    }\n")
            append("  ],\n")
            append("  \"actionItems\": [\n")
            append("    \"Try the core exercise or code snippet yourself in 10 minutes.\",\n")
            append("    \"Audit your current workflow against the rules taught in the video.\",\n")
            append("    \"Review and test your understanding with a quick practice problem.\"\n")
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
