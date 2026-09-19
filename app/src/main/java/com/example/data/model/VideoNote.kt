package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_notes")
data class VideoNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoUrl: String,
    val videoId: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val category: String = "General",
    val summary: String,
    val keyTakeaways: String, // Stored as JSON or newline-separated strings
    val structuredBullets: String, // Stored as JSON or markdown sections
    val actionItems: String, // Stored as JSON or newline-separated strings
    val userNotes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class StructuredSection(
    val heading: String,
    val points: List<String>
)

data class GeneratedNoteResult(
    val title: String,
    val channel: String,
    val category: String,
    val summary: String,
    val keyTakeaways: List<String>,
    val structuredBullets: List<StructuredSection>,
    val actionItems: List<String>
)
