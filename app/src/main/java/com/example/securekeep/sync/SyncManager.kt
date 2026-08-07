package com.example.securekeep.sync

import com.example.securekeep.data.local.Note
import kotlinx.coroutines.flow.Flow

interface SyncManager {

    suspend fun uploadNote(note: Note)

    suspend fun deleteNote(noteId: Int)

    fun observeNotes(): Flow<List<Note>>

    suspend fun performInitialSync()

    suspend fun performIncrementalSync()
}