package com.example.feature_note.data.local.cache

import androidx.room.*
import com.example.feature_note.data.local.settings.SortType
import com.example.feature_note.domain.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    fun getAllNotes(searchQuery: String, sortType: SortType): Flow<List<Note>> =
        when (sortType) {
            SortType.ASCENDING -> {
                getAllNotesByAscending(searchQuery)
            }
            SortType.DESCENDING -> {
                getAllNotesByDescending(searchQuery)
            }
        }

    @Query("SELECT * FROM note_db WHERE title LIKE '%' || :searchQuery || '%' ORDER BY timestamp ASC")
    fun getAllNotesByAscending(searchQuery: String): Flow<List<Note>>

    @Query("SELECT * FROM note_db WHERE title LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    fun getAllNotesByDescending(searchQuery: String): Flow<List<Note>>

    @Delete
    suspend fun deleteNote(note: Note)

    @Insert
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)
}