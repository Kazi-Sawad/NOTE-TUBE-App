package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.GeneratedNoteResult
import com.example.data.model.StructuredSection
import com.example.data.model.VideoNote
import com.example.data.remote.GeminiService
import com.example.data.remote.OfflineNotesGenerator
import com.example.data.remote.YouTubeHelper
import com.example.data.repository.NoteRepository
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

class NoteTubeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    private val _currentNote = MutableStateFlow<VideoNote?>(null)
    val currentNote: StateFlow<VideoNote?> = _currentNote.asStateFlow()

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

    fun setCurrentNote(note: VideoNote?) {
        _currentNote.value = note
    }

    fun selectNote(note: VideoNote?) {
        _currentNote.value = note
    }

    fun loadNoteById(noteId: Long) {
        viewModelScope.launch {
            val note = repository.getNoteByIdDirect(noteId)
            _currentNote.value = note
        }
    }

    fun clearGenerationState() {
        _generationState.value = GenerationUiState.Idle
    }

    fun generateNotesForVideo(
        inputUrlOrId: String,
        additionalContext: String = "",
        forceOffline: Boolean = false
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

                val metadata = if (!shouldRunOffline) {
                    _generationState.value = GenerationUiState.Loading("Fetching video metadata...")
                    YouTubeHelper.fetchVideoMetadata(videoId)
                } else {
                    _generationState.value = GenerationUiState.Loading("Initializing on-device synthesis engine...")
                    // Instant offline metadata resolution
                    YouTubeHelper.fetchVideoMetadata(videoId)
                }

                val result = if (shouldRunOffline) {
                    _generationState.value = GenerationUiState.Loading("Synthesizing deep conceptual notes (Offline Mode)...")
                    OfflineNotesGenerator.generateInDepthOfflineNotes(
                        videoId = videoId,
                        rawTitle = metadata.title,
                        channel = metadata.channel,
                        userContext = additionalContext
                    )
                } else {
                    _generationState.value = GenerationUiState.Loading("Synthesizing in-depth AI analysis & broad concepts...")
                    GeminiService.generateVideoNotes(
                        videoUrl = metadata.canonicalUrl,
                        videoId = metadata.videoId,
                        videoTitle = metadata.title,
                        channelName = metadata.channel,
                        userProvidedContext = additionalContext
                    )
                }

                _generationState.value = GenerationUiState.Loading("Saving insights to library...")

                // Convert structured objects to JSON storage
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
                    userNotes = if (additionalContext.isNotBlank()) "User Focus Topic / Context:\n$additionalContext" else "",
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
                                heading = "4. Quitting Social Media & Draining the Shallows",
                                points = listOf(
                                    "The Craftsman Approach to Tool Selection: Adopt a tool only if its positive impacts substantially outweigh its negative consequences.",
                                    "Rejecting the Any-Benefit Mindset: The presence of some small incidental benefit does not justify the massive loss of sustained focus.",
                                    "Schedule Every Minute of Your Day: Divide the day into dedicated blocks, adapting dynamically when unexpected events occur."
                                )
                            )
                        )),
                        actionItems = listOf(
                            "Establish a sacred 90-minute morning deep work block tomorrow with all phone and browser notifications turned off.",
                            "Perform an audit of communication tools and eliminate or pause two low-yield notification channels.",
                            "Design and execute a concrete end-of-day shutdown ritual to cleanly conclude professional work."
                        ).let { JSONArray(it).toString() },
                        userNotes = "Review Newport's four scheduling philosophies and determine how to structure weekly deep work blocks.",
                        isFavorite = true,
                        createdAt = System.currentTimeMillis() - 86400000L
                    )

                    val sample2 = VideoNote(
                        videoUrl = "https://www.youtube.com/watch?v=kqtD5dpn9C8",
                        videoId = "kqtD5dpn9C8",
                        title = "Python for Beginners: Learn Coding with Python in 1 Hour",
                        channel = "Programming with Mosh",
                        thumbnailUrl = "https://img.youtube.com/vi/kqtD5dpn9C8/hqdefault.jpg",
                        category = "Programming",
                        summary = "An exhaustive, first-principles curriculum covering fundamental computer science architectures, execution environments, and Python programming mechanics. Deconstructs syntax, variable memory models, type casting, conditional flow, loops, and core data structures with executable scripts.",
                        keyTakeaways = listOf(
                            "Dynamic Typing Model: Variables in Python are labels referencing memory objects, not typed storage buckets, allowing rapid expressive iteration.",
                            "Type Safety & Casting: User input via input() always yields string types; explicit casting (int, float) is vital to avoid runtime type bugs.",
                            "Collections Architecture: Lists are mutable dynamic arrays optimized for modification; tuples are immutable sequences protecting structural integrity.",
                            "Clean Idiomatic Syntax: Python replaces verbose curly-brace syntax with significant whitespace, enforcing clean, readable code formatting by design."
                        ).let { JSONArray(it).toString() },
                        structuredBullets = serializeSections(listOf(
                            StructuredSection(
                                heading = "1. Runtime Environment & The Python Virtual Machine",
                                points = listOf(
                                    "Python is an interpreted, high-level language combining developer ergonomics with extensive multi-domain libraries.",
                                    "Code is compiled into bytecode (.pyc) and executed on the Python Virtual Machine (PVM).",
                                    "Development workflow: Installing the interpreter from python.org and configuring an IDE such as PyCharm or VS Code."
                                )
                            ),
                            StructuredSection(
                                heading = "2. Executable Code & Data Processing Script",
                                points = listOf(
                                    "The video demonstrates building a complete, interactive program that processes and filters user data:\n\n```python\n# Interactive Data Pipeline\ndef process_user_metrics(raw_scores: list[int]) -> dict:\n    \"\"\"Calculates statistical summary with boundary validation.\"\"\"\n    valid_scores = [s for s in raw_scores if 0 <= s <= 100]\n    if not valid_scores:\n        return {\"count\": 0, \"average\": 0.0}\n        \n    mean_score = sum(valid_scores) / len(valid_scores)\n    return {\n        \"count\": len(valid_scores),\n        \"average\": round(mean_score, 2),\n        \"top_score\": max(valid_scores)\n    }\n\n# Execution\nscores = [88, 92, 79, 105, 95]  # 105 filtered out by boundary rule\nsummary = process_user_metrics(scores)\nprint(f\"Computed Metrics: {summary}\")\n```",
                                    "Variables store references in memory; Python handles garbage collection automatically via reference counting.",
                                    "Strings are immutable sequences with rich built-in transformations (.upper(), .find(), .replace())."
                                )
                            ),
                            StructuredSection(
                                heading = "3. Control Structures & Computational Flow",
                                points = listOf(
                                    "Conditional branching via if, elif, and else using standard logical operators (and, or, not).",
                                    "While loops repeat operations based on mutable state conditions; essential for continuous interactive programs.",
                                    "For loops iterate over iterable sequences using the range() generator to prevent memory-heavy integer arrays."
                                )
                            ),
                            StructuredSection(
                                heading = "4. Core Data Structures: Lists vs. Tuples",
                                points = listOf(
                                    "Lists are zero-indexed, ordered, mutable sequences supporting .append(), .insert(), and .remove().",
                                    "Tuples are immutable collections enclosed in parentheses, preventing accidental state mutations across functions.",
                                    "List slicing [start:end:step] enables efficient extraction of sub-arrays without mutating source collections."
                                )
                            )
                        )),
                        actionItems = listOf(
                            "Run the provided Python script locally in your terminal or IDE to verify the metric filter.",
                            "Experiment with list methods (.append, .insert, .remove) vs. tuples to understand mutation trade-offs.",
                            "Write automated unit tests verifying edge cases for input calculations."
                        ).let { JSONArray(it).toString() },
                        userNotes = "Fundamental reference for Python memory models, executable scripts, and collection primitives.",
                        isFavorite = false,
                        createdAt = System.currentTimeMillis() - 172800000L
                    )

                    val sample3 = VideoNote(
                        videoUrl = "https://www.youtube.com/watch?v=WUvTyaaNkzM",
                        videoId = "WUvTyaaNkzM",
                        title = "Essence of Calculus: Derivatives & The Geometric Intuition",
                        channel = "3Blue1Brown Masterclass",
                        thumbnailUrl = "https://img.youtube.com/vi/WUvTyaaNkzM/hqdefault.jpg",
                        category = "Mathematics",
                        summary = "A transformative, visual first-principles breakdown of calculus. Grant Sanderson explains that derivatives are not just algorithmic symbol manipulation rules, but the geometric measurement of how sensitive a system is to infinitesimally small changes.",
                        keyTakeaways = listOf(
                            "Geometric Meaning of Derivatives: The derivative measures the instantaneous rate of change as the ratio between tiny nudge in input (dx) and resulting nudge in output (df).",
                            "The Paradox of Instantaneous Rate: You cannot measure change in zero time; instead, you analyze what the ratio approaches as time approaches zero (limits).",
                            "Fundamental Theorem of Calculus: Derivatives and Integrals are exact inverse operations; area under a curve directly computes cumulative change.",
                            "Calculus in Computation: Gradient descent in artificial intelligence is simply multivariable calculus navigating complex loss surfaces."
                        ).let { JSONArray(it).toString() },
                        structuredBullets = serializeSections(listOf(
                            StructuredSection(
                                heading = "1. Core Philosophy & Geometric Intuition",
                                points = listOf(
                                    "Calculus is the mathematical study of continuous change; traditional algebra models static quantities, whereas calculus models fluid systems.",
                                    "Instead of memorizing power rules in isolation, visualize the area of a circle A = πr^2 as concentric rings unrolling into a triangle of base 2πr and height r.",
                                    "Differentiating area gives perimeter: dA/dr = 2πr because adding a tiny thickness dr to a circle increases area by circumference times dr."
                                )
                            ),
                            StructuredSection(
                                heading = "2. Mathematical Formulation & Limit Definition",
                                points = listOf(
                                    "The formal definition of a derivative is expressed as:\n\nf'(x) = lim_{h -> 0} [f(x + h) - f(x)] / h",
                                    "Step 1: Consider a function f(x) and evaluate it at a slightly nudged coordinate x + h.",
                                    "Step 2: The difference quotient represents the slope of the secant line intersecting both points.",
                                    "Step 3: As nudge h contracts toward zero, the secant line rotates smoothly into the instantaneous tangent line."
                                )
                            ),
                            StructuredSection(
                                heading = "3. Power Rule & Polynomial Derivation",
                                points = listOf(
                                    "Governing Algebraic Power Rule Formula:\n\nd/dx [x^n] = n * x^(n - 1)",
                                    "Geometric proof for x^2: Consider a square with side length x. Increasing length by dx adds two thin strips of area x * dx and a tiny corner (dx)^2.",
                                    "Dividing by dx and letting dx approach zero causes the corner to vanish, leaving exactly 2x."
                                )
                            ),
                            StructuredSection(
                                heading = "4. Applications to Physics & Modern Machine Learning",
                                points = listOf(
                                    "Kinematics correspondence: Position s(t) -> Derivative yields Velocity v(t) = s'(t) -> Second derivative yields Acceleration a(t) = v'(t).",
                                    "Neural Network Backpropagation: Deep learning optimization relies on the chain rule to backpropagate loss gradients through millions of parameters.",
                                    "The Gradient Operator:\n\n∇f = [∂f/∂x_1, ∂f/∂x_2, ..., ∂f/∂x_n]"
                                )
                            )
                        )),
                        actionItems = listOf(
                            "Draw the geometric derivation of the derivative for f(x) = x^2 on paper without formulas.",
                            "Calculate the derivatives of f(x) = x^3 - 4x + 7 using both first principles and the power rule.",
                            "Visualize how gradient descent steps opposite to the gradient vector ∇f to find minima."
                        ).let { JSONArray(it).toString() },
                        userNotes = "Essential mathematical framework connecting continuous geometry with computational optimization.",
                        isFavorite = true,
                        createdAt = System.currentTimeMillis() - 250000000L
                    )

                    repository.insertNote(sample1)
                    repository.insertNote(sample2)
                    repository.insertNote(sample3)
                }
        }
    }

    companion object {
        fun serializeSections(sections: List<StructuredSection>): String {
            val jsonArray = JSONArray()
            for (sec in sections) {
                val obj = JSONObject()
                obj.put("heading", sec.heading)
                val pointsArr = JSONArray()
                for (p in sec.points) {
                    pointsArr.put(p)
                }
                obj.put("points", pointsArr)
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        fun parseSections(jsonString: String): List<StructuredSection> {
            if (jsonString.isBlank()) return emptyList()
            val list = mutableListOf<StructuredSection>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val heading = obj.optString("heading", "Section")
                    val pointsArray = obj.optJSONArray("points")
                    val points = mutableListOf<String>()
                    if (pointsArray != null) {
                        for (j in 0 until pointsArray.length()) {
                            points.add(pointsArray.getString(j))
                        }
                    }
                    list.add(StructuredSection(heading, points))
                }
            } catch (_: Exception) {
                val lines = jsonString.lines().filter { it.isNotBlank() }
                if (lines.isNotEmpty()) {
                    list.add(StructuredSection("Key Points", lines))
                }
            }
            return list
        }

        fun parseStringList(jsonString: String): List<String> {
            if (jsonString.isBlank()) return emptyList()
            val list = mutableListOf<String>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
            } catch (_: Exception) {
                return jsonString.lines().filter { it.isNotBlank() }
            }
            return list
        }
    }
}
