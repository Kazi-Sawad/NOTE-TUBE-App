package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoNote
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoNoteDao {
    @Query("SELECT * FROM video_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<VideoNote>>

    @Query("SELECT * FROM video_notes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteNotes(): Flow<List<VideoNote>>

    @Query("SELECT * FROM video_notes WHERE LOWER(category) = LOWER(:category) ORDER BY createdAt DESC")
    fun getNotesByCategory(category: String): Flow<List<VideoNote>>

    @Query("SELECT * FROM video_notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<VideoNote?>

    @Query("SELECT * FROM video_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdDirect(id: Long): VideoNote?

    @Query("SELECT COUNT(*) FROM video_notes")
    fun getNoteCount(): Flow<Int>

    @Query("SELECT * FROM video_notes WHERE title LIKE '%' || :query || '%' OR channel LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchNotes(query: String): Flow<List<VideoNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VideoNote): Long

    @Update
    suspend fun updateNote(note: VideoNote)

    @Delete
    suspend fun deleteNote(note: VideoNote)

    @Query("DELETE FROM video_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE video_notes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE video_notes SET userNotes = :userNotes WHERE id = :id")
    suspend fun updateUserNotes(id: Long, userNotes: String)
}
