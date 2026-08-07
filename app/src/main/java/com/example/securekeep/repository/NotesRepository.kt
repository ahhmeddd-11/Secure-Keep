package com.example.securekeep.repository

import com.example.securekeep.data.local.Note
import com.example.securekeep.data.local.NoteDao
import com.example.securekeep.data.local.SyncState
import com.example.securekeep.sync.ChecksumUtil
import kotlinx.coroutines.flow.Flow

class NotesRepository(private val noteDao: NoteDao) {

    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotes()

    val deletedNotes: Flow<List<Note>> = noteDao.getDeletedNotes()

    suspend fun insert(note: Note): Long {

        val now = System.currentTimeMillis()

        val syncReadyNote = note.copy(
            updatedAt = now,
            version = note.version + 1,
            syncState = SyncState.DIRTY,
            checksum = ChecksumUtil.generate(
                note.title,
                note.content
            )
        )

        return noteDao.insertNote(syncReadyNote)
    }

    suspend fun delete(note: Note) {

        val updatedNote = note.copy(
            isDeleted = true,
            deletedTimestamp = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = note.version + 1,
            syncState = SyncState.DIRTY,
            checksum = ChecksumUtil.generate(
                note.title,
                note.content
            )
        )

        noteDao.updateNote(updatedNote)
    }
    suspend fun deletePermanently(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun update(note: Note) {

        val updatedNote = note.copy(
            updatedAt = System.currentTimeMillis(),
            version = note.version + 1,
            syncState = SyncState.DIRTY,
            checksum = ChecksumUtil.generate(
                note.title,
                note.content
            )
        )

        noteDao.updateNote(updatedNote)
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

    suspend fun getAllNotes(): List<Note> {
        return noteDao.getAllNotes()
    }

    suspend fun getNoteByUuid(uuid: String): Note? {
        return noteDao.getNoteByUuid(uuid)
    }

    suspend fun upsertNotes(notes: List<Note>) {
        noteDao.upsertNotes(notes)
    }
}