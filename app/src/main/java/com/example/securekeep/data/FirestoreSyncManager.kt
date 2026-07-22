package com.example.securekeep.data

import com.example.securekeep.data.local.Note
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager {
    private val db = FirebaseFirestore.getInstance()
    private val notesCollection = db.collection("notes")

    // Sync local note to Firestore
    suspend fun syncNote(note: Note) {
        val noteMap = mapOf(
            "id" to note.id,
            "title" to note.title,
            "content" to note.content,
            "color" to note.color,
            "isPinned" to note.isPinned,
            "isLocked" to note.isLocked,
            "lockedName" to note.lockedName,
            "isDeleted" to note.isDeleted,
            "deletedTimestamp" to note.deletedTimestamp,
            "imageUri" to note.imageUri,
            "timestamp" to note.timestamp
        )
        // Use ID as document name for easy syncing
        notesCollection.document(note.id.toString()).set(noteMap).await()
    }

    // Delete note from Firestore
    suspend fun deleteNote(noteId: Int) {
        notesCollection.document(noteId.toString()).delete().await()
    }

    // Real-time listener for updates from Web or other devices
    fun getNotesFlow(): Flow<List<Note>> = callbackFlow {
        val subscription = notesCollection.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val notes = snapshot.documents.mapNotNull { doc ->
                        try {
                            Note(
                                id = (doc.getLong("id") ?: 0L).toInt(),
                                title = doc.getString("title") ?: "",
                                content = doc.getString("content") ?: "",
                                color = doc.getLong("color") ?: 0L,
                                isPinned = doc.getBoolean("isPinned") ?: false,
                                isLocked = doc.getBoolean("isLocked") ?: false,
                                lockedName = doc.getString("lockedName"),
                                isDeleted = doc.getBoolean("isDeleted") ?: false,
                                deletedTimestamp = doc.getLong("deletedTimestamp"),
                                imageUri = doc.getString("imageUri"),
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(notes)
                }
            }
        awaitClose { subscription.remove() }
    }
}
