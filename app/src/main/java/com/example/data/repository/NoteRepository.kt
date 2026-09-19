package com.example.data.repository

import com.example.data.local.VideoNoteDao
import com.example.data.model.VideoNote
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val videoNoteDao: VideoNoteDao) {
    val allNotes: Flow<List<VideoNote>> = videoNoteDao.getAllNotes()
    val favoriteNotes: Flow<List<VideoNote>> = videoNoteDao.getFavoriteNotes()

    fun getNotesByCategory(category: String): Flow<List<VideoNote>> =
        videoNoteDao.getNotesByCategory(category)

    fun searchNotes(query: String): Flow<List<VideoNote>> =
        videoNoteDao.searchNotes(query)

    fun getNoteById(id: Long): Flow<VideoNote?> =
        videoNoteDao.getNoteById(id)

    suspend fun getNoteByIdDirect(id: Long): VideoNote? =
        videoNoteDao.getNoteByIdDirect(id)

    fun getNoteCount(): Flow<Int> =
        videoNoteDao.getNoteCount()

    suspend fun insertNote(note: VideoNote): Long =
        videoNoteDao.insertNote(note)

    suspend fun updateNote(note: VideoNote) =
        videoNoteDao.updateNote(note)

    suspend fun deleteNote(note: VideoNote) =
        videoNoteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Long) =
        videoNoteDao.deleteNoteById(id)

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) =
        videoNoteDao.updateFavorite(id, !currentStatus)

    suspend fun updateUserNotes(id: Long, notes: String) =
        videoNoteDao.updateUserNotes(id, notes)
}
