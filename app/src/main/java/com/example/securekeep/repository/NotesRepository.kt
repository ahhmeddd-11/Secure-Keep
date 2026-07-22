package com.example.securekeep.repository

import com.example.securekeep.data.local.Note
import com.example.securekeep.data.local.NoteDao
import kotlinx.coroutines.flow.Flow

class NotesRepository(private val noteDao: NoteDao) {
    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotes()
    val deletedNotes: Flow<List<Note>> = noteDao.getDeletedNotes()

    suspend fun insert(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun delete(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun cleanupOldNotes(threshold: Long) {
        noteDao.deleteOldNotes(threshold)
    }
}
