package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "official_notes")
data class OfficialNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val sourceName: String,
    val sourceUrl: String,
    val authorOrInstitution: String,
    val type: OfficialNoteType,
    val description: String,
    val contentMarkdown: String,
    val keyTakeaways: List<String>,
    val tags: List<String>,
    val isVerified: Boolean = true,
    val downloadUrl: String? = null
)

enum class OfficialNoteType(val label: String, val badgeColorHex: String) {
    OFFICIAL_DOCS("Official Documentation", "#007AFF"),
    ACADEMIC_CHEATSHEET("University Cheatsheet", "#5856D6"),
    HANDWRITTEN_LECTURE("Handwritten Lecture Notes", "#FF9500"),
    EXAM_BLUEPRINT("Official Exam Blueprint", "#34C759"),
    STANDARD_SPEC("W3C / RFC Spec", "#AF52DE")
}
