package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.GeneratedNoteResult
import com.example.data.model.NoteLanguage
import com.example.data.model.NoteMode
import com.example.data.model.OfficialNote
import com.example.data.model.StructuredSection
import com.example.data.model.VideoNote
import com.example.data.remote.CameraAnalysisResult
import com.example.data.remote.GeminiService
import com.example.data.remote.OfflineNotesGenerator
import com.example.data.remote.YouTubeHelper
import com.example.data.repository.NoteRepository
import com.example.data.repository.OfficialNotesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed interface GenerationUiState {
    object Idle : GenerationUiState
    data class Loading(val message: String) : GenerationUiState
    data class Success(val note: VideoNote) : GenerationUiState
    data class Error(val message: String) : GenerationUiState
}

sealed interface CameraScanUiState {
    object Idle : CameraScanUiState
    data class Scanning(val message: String) : CameraScanUiState
    data class Success(val result: CameraAnalysisResult) : CameraScanUiState
    data class Error(val message: String) : CameraScanUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class NoteTubeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(NoteLanguage.ENGLISH)
    val selectedLanguage: StateFlow<NoteLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedNoteMode = MutableStateFlow(NoteMode.EXAM_TUTORIAL)
    val selectedNoteMode: StateFlow<NoteMode> = _selectedNoteMode.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    private val _cameraScanState = MutableStateFlow<CameraScanUiState>(CameraScanUiState.Idle)
    val cameraScanState: StateFlow<CameraScanUiState> = _cameraScanState.asStateFlow()

    private val _currentNote = MutableStateFlow<VideoNote?>(null)
    val currentNote: StateFlow<VideoNote?> = _currentNote.asStateFlow()

    private val _selectedOfficialNote = MutableStateFlow<OfficialNote?>(null)
    val selectedOfficialNote: StateFlow<OfficialNote?> = _selectedOfficialNote.asStateFlow()

    val officialNotes: StateFlow<List<OfficialNote>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        OfficialNotesRepository.searchNotes(query, category)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NoteRepository(database.videoNoteDao())
        seedSampleNotesIfEmpty()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<VideoNote>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        when {
            query.isNotBlank() -> repository.searchNotes(query)
            category.equals("Favorites", ignoreCase = true) -> repository.favoriteNotes
            category.isNotBlank() && !category.equals("All", ignoreCase = true) -> repository.getNotesByCategory(category)
            else -> repository.allNotes
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun setLanguage(language: NoteLanguage) {
        _selectedLanguage.value = language
    }

    fun setNoteMode(mode: NoteMode) {
        _selectedNoteMode.value = mode
    }

    fun setCurrentNote(note: VideoNote?) {
        _currentNote.value = note
    }

    fun selectNote(note: VideoNote?) {
        _currentNote.value = note
    }

    fun selectOfficialNote(note: OfficialNote?) {
        _selectedOfficialNote.value = note
    }

    fun clearGenerationState() {
        _generationState.value = GenerationUiState.Idle
    }

    fun clearCameraScanState() {
        _cameraScanState.value = CameraScanUiState.Idle
    }

    fun generateNotesForVideo(
        inputUrlOrId: String,
        additionalContext: String = "",
        forceOffline: Boolean = false,
        language: NoteLanguage = _selectedLanguage.value,
        mode: NoteMode = _selectedNoteMode.value
    ) {
        viewModelScope.launch {
            val videoId = YouTubeHelper.extractVideoId(inputUrlOrId)
            if (videoId == null) {
                _generationState.value = GenerationUiState.Error("Please enter a valid YouTube link or 11-character video ID.")
                return@launch
            }

            try {
                val isDeviceOnline = OfflineNotesGenerator.isOnline(getApplication())
                val shouldRunOffline = forceOffline || !isDeviceOnline

                _generationState.value = GenerationUiState.Loading("Extracting authentic title, channel & timed transcript...")
                val metadata = YouTubeHelper.fetchVideoMetadata(videoId)

                val combinedContext = buildString {
                    if (additionalContext.isNotBlank()) {
                        append("Student Target Focus:\n")
                        append(additionalContext.trim())
                    }
                    if (metadata.descriptionOrTranscript.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("Real Spoken Dialogue & Video Transcript:\n")
                        append(metadata.descriptionOrTranscript.take(4000))
                    }
                }

                val result = if (shouldRunOffline) {
                    _generationState.value = GenerationUiState.Loading("Synthesizing ${mode.title} in ${language.displayName} (Offline)...")
                    OfflineNotesGenerator.generateInDepthOfflineNotes(
                        videoId = videoId,
                        rawTitle = metadata.title,
                        channel = metadata.channel,
                        userContext = combinedContext,
                        language = language,
                        mode = mode
                    )
                } else {
                    _generationState.value = GenerationUiState.Loading("Generating high-efficiency ${mode.title} in ${language.displayName}...")
                    GeminiService.generateVideoNotes(
                        videoUrl = metadata.canonicalUrl,
                        videoId = metadata.videoId,
                        videoTitle = metadata.title,
                        channelName = metadata.channel,
                        userProvidedContext = combinedContext,
                        targetLanguage = language,
                        noteMode = mode
                    )
                }

                _generationState.value = GenerationUiState.Loading("Saving exam notes to library...")

                val takeawaysJson = JSONArray(result.keyTakeaways).toString()
                val bulletsJson = serializeSections(result.structuredBullets)
                val actionsJson = JSONArray(result.actionItems).toString()

                val newNote = VideoNote(
                    videoUrl = metadata.canonicalUrl,
                    videoId = metadata.videoId,
                    title = result.title,
                    channel = result.channel,
                    thumbnailUrl = metadata.thumbnailUrl,
                    category = result.category,
                    summary = result.summary,
                    keyTakeaways = takeawaysJson,
                    structuredBullets = bulletsJson,
                    actionItems = actionsJson,
                    userNotes = buildString {
                        append("Language: ${language.displayName} (${language.flag}) • Mode: ${mode.title}\n")
                        if (additionalContext.isNotBlank()) {
                            append("Student Focus Note:\n$additionalContext")
                        }
                    },
                    isFavorite = false,
                    createdAt = System.currentTimeMillis()
                )

                val insertedId = repository.insertNote(newNote)
                val savedNote = newNote.copy(id = insertedId)
                _currentNote.value = savedNote
                _generationState.value = GenerationUiState.Success(savedNote)
            } catch (e: Exception) {
                _generationState.value = GenerationUiState.Error("Generation failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Camera Note Analysis:
     * Analyzes image of handwritten notes, textbook equations, or exam questions
     */
    fun analyzeCameraImage(
        bitmap: Bitmap,
        language: NoteLanguage = _selectedLanguage.value,
        additionalHint: String = ""
    ) {
        viewModelScope.launch {
            _cameraScanState.value = CameraScanUiState.Scanning("Scanning handwritten notes, diagrams & formulas...")
            try {
                val analysis = GeminiService.analyzeNoteImage(bitmap, language, additionalHint)
                _cameraScanState.value = CameraScanUiState.Success(analysis)
            } catch (e: Exception) {
                _cameraScanState.value = CameraScanUiState.Error("Camera note analysis failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Converts a camera analysis result directly into a saved library note
     */
    fun saveCameraAnalysisAsNote(analysis: CameraAnalysisResult) {
        viewModelScope.launch {
            val takeawaysJson = JSONArray(analysis.keyPoints).toString()
            val structuredSections = listOf(
                StructuredSection(
                    heading = "1. Transcribed Notes & Equations",
                    points = analysis.extractedText.lines().filter { it.isNotBlank() }
                ),
                StructuredSection(
                    heading = "2. Core Academic Concepts",
                    points = analysis.keyPoints
                ),
                StructuredSection(
                    heading = "3. Exam Questions & Critical Formulas",
                    points = analysis.examQuestionsOrFormulas
                )
            )
            val bulletsJson = serializeSections(structuredSections)
            val actionsJson = JSONArray(analysis.actionItems).toString()

            val newNote = VideoNote(
                videoUrl = "camera://scanned_notes_${System.currentTimeMillis()}",
                videoId = "scan_${System.currentTimeMillis() % 100000}",
                title = analysis.title,
                channel = "Camera Note Scanner",
                thumbnailUrl = "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=500&auto=format&fit=crop&q=60",
                category = analysis.subject,
                summary = analysis.summary,
                keyTakeaways = takeawaysJson,
                structuredBullets = bulletsJson,
                actionItems = actionsJson,
                userNotes = "Transcribed and synthesized via Camera Note Vision Scanner.\n\nRaw Text:\n${analysis.extractedText}",
                isFavorite = false,
                createdAt = System.currentTimeMillis()
            )

            val insertedId = repository.insertNote(newNote)
            val savedNote = newNote.copy(id = insertedId)
            _currentNote.value = savedNote
            _cameraScanState.value = CameraScanUiState.Idle
        }
    }

    /**
     * Converts an official university/docs note into a saved library note
     */
    fun saveOfficialNoteToLibrary(officialNote: OfficialNote) {
        viewModelScope.launch {
            val takeawaysJson = JSONArray(officialNote.keyTakeaways).toString()
            val sections = listOf(
                StructuredSection(
                    heading = "1. Official Specification & Author Insights",
                    points = listOf(
                        "Source: ${officialNote.sourceName} (${officialNote.sourceUrl})",
                        "Author / Institution: ${officialNote.authorOrInstitution}",
                        "Document Class: ${officialNote.type.label}"
                    )
                ),
                StructuredSection(
                    heading = "2. Core Knowledge & Document Contents",
                    points = officialNote.contentMarkdown.lines()
                        .filter { it.isNotBlank() && !it.startsWith("#") }
                        .take(8)
                ),
                StructuredSection(
                    heading = "3. High-Yield Exam Takeaways",
                    points = officialNote.keyTakeaways
                )
            )
            val bulletsJson = serializeSections(sections)
            val actionsJson = JSONArray(listOf(
                "Review the full standard specification at ${officialNote.sourceUrl}",
                "Work through the practice code and derivation steps in Section 2",
                "Self-test the key definitions from ${officialNote.authorOrInstitution}"
            )).toString()

            val newNote = VideoNote(
                videoUrl = officialNote.sourceUrl,
                videoId = "doc_${officialNote.id}",
                title = officialNote.title,
                channel = officialNote.authorOrInstitution,
                thumbnailUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500&auto=format&fit=crop&q=60",
                category = officialNote.subject,
                summary = officialNote.description,
                keyTakeaways = takeawaysJson,
                structuredBullets = bulletsJson,
                actionItems = actionsJson,
                userNotes = "Saved from Official Academic Repository (${officialNote.sourceName}).\n\nFull Reference:\n${officialNote.contentMarkdown}",
                isFavorite = true,
                createdAt = System.currentTimeMillis()
            )

            val insertedId = repository.insertNote(newNote)
            val savedNote = newNote.copy(id = insertedId)
            _currentNote.value = savedNote
            _selectedOfficialNote.value = null
        }
    }

    fun toggleFavorite(note: VideoNote) {
        viewModelScope.launch {
            val updated = note.copy(isFavorite = !note.isFavorite)
            repository.updateNote(updated)
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = updated
            }
        }
    }

    fun updateUserNotes(noteId: Long, notes: String) {
        viewModelScope.launch {
            val existing = repository.getNoteByIdDirect(noteId) ?: return@launch
            val updated = existing.copy(userNotes = notes)
            repository.updateNote(updated)
            if (_currentNote.value?.id == noteId) {
                _currentNote.value = updated
            }
        }
    }

    fun deleteNote(note: VideoNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = null
            }
        }
    }

    private fun seedSampleNotesIfEmpty() {
        viewModelScope.launch {
            val count = repository.getNoteCount().first()
            if (count == 0) {
                val sample1 = VideoNote(
                    videoUrl = "https://www.youtube.com/watch?v=kGgAhy_e1n8",
                    videoId = "kGgAhy_e1n8",
                    title = "Deep Work: Rules for Focused Success in a Distracted World",
                    channel = "Cal Newport Masterclass",
                    thumbnailUrl = "https://img.youtube.com/vi/kGgAhy_e1n8/hqdefault.jpg",
                    category = "Productivity",
                    summary = "A comprehensive conceptual deep-dive into cognitive psychology, attention economics, and execution frameworks. Cal Newport argues that the ability to perform deep work is becoming increasingly rare at the exact same time it is becoming increasingly valuable in our economy.",
                    keyTakeaways = listOf(
                        "The Deep Work Hypothesis: The ability to perform deep work is becoming rare and valuable simultaneously; those who cultivate this skill will thrive.",
                        "Attention Residue Mechanics: Switching between tasks, even momentarily checking an email, leaves cognitive residue that degrades executive function for 20+ minutes.",
                        "The Metric Black Hole: In business, measuring shallow activity is easy while quantifying deep creative output is hard, leading organizations to default to visible busyness.",
                        "Deliberate Decompression: Professional high performers schedule strict work shutdown rituals to ensure biological neural consolidation and avoid burnout."
                    ).let { JSONArray(it).toString() },
                    structuredBullets = serializeSections(listOf(
                        StructuredSection(
                            heading = "1. Foundational Theory & Attention Economics",
                            points = listOf(
                                "Deep Work defined: Professional activities performed in a state of distraction-free concentration that push cognitive capabilities to their limit.",
                                "Shallow Work defined: Non-cognitively demanding, logistical tasks often performed while distracted; easy to replicate and creates little new value.",
                                "Attention Residue: When shifting from Task A to Task B, attention remains split; working in bursts on multiple items drastically reduces cognitive bandwidth."
                            )
                        ),
                        StructuredSection(
                            heading = "2. The Four Scheduling Philosophies",
                            points = listOf(
                                "Monastic: Eliminating or drastically minimizing all shallow obligations (e.g. Donald Knuth shutting down email).",
                                "Bimodal: Dividing time into clearly defined deep stretches (days or weeks) and leaving the rest for logistical obligations.",
                                "Rhythmic: Establishing a consistent, automated daily cadence (e.g. 90-minute blocks every morning from 7 to 8:30 AM).",
                                "Journalistic: Switching into deep work mode on short notice whenever free moments appear; requires high cognitive discipline."
                            )
                        ),
                        StructuredSection(
                            heading = "3. Rituals, Environmental Cues & Grand Gestures",
                            points = listOf(
                                "Location priming: Dedicating a quiet physical space exclusively to high-intensity creative output.",
                                "The Grand Gesture: Radically investing resources or time (e.g. booking a secluded cabin or flight) to reset psychological commitment.",
                                "Shutdown Rituals: Closing open loops before dinner with a definitive check of tasks and a deliberate closing phrase to prevent rumination."
                            )
                        ),
                        StructuredSection(
                            heading = "4. Exam & Practical Application Blueprint",
                            points = listOf(
                                "Key Rule: Schedule every minute of your day into time blocks; revise blocks as realities change rather than abandoning structure.",
                                "The 4DX Execution Engine: 1. Focus on the Wildly Important, 2. Act on Lead Measures, 3. Keep a Compelling Scoreboard, 4. Create a Cadence of Accountability."
                            )
                        )
                    )),
                    actionItems = listOf(
                        "Conduct an attention audit: track shallow vs deep hours for the next 48 hours.",
                        "Establish an immutable 90-minute deep work block tomorrow morning before checking notifications.",
                        "Draft a 4-step evening shutdown ritual to eliminate attention residue."
                    ).let { JSONArray(it).toString() },
                    userNotes = "Initial seed masterclass note.",
                    isFavorite = true,
                    createdAt = System.currentTimeMillis()
                )
                repository.insertNote(sample1)
            }
        }
    }

    companion object {
        fun serializeSections(sections: List<StructuredSection>): String {
            val array = JSONArray()
            sections.forEach { sec ->
                val obj = JSONObject().apply {
                    put("heading", sec.heading)
                    val pts = JSONArray()
                    sec.points.forEach { pts.put(it) }
                    put("points", pts)
                }
                array.put(obj)
            }
            return array.toString()
        }

        fun parseStringList(jsonStr: String): List<String> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                (0 until array.length()).map { array.getString(it) }
            } catch (_: Exception) {
                jsonStr.lines().filter { it.isNotBlank() }
            }
        }

        fun parseSections(jsonStr: String): List<StructuredSection> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    val heading = obj.optString("heading", "Section ${i + 1}")
                    val pointsArr = obj.optJSONArray("points") ?: JSONArray()
                    val points = (0 until pointsArr.length()).map { pointsArr.getString(it) }
                    StructuredSection(heading = heading, points = points)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
