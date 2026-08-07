package com.example.securekeep.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class Note(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Global unique identifier for cross-device synchronization
    val uuid: String = UUID.randomUUID().toString(),

    val title: String,

    val content: String,

    val color: Long,

    val isPinned: Boolean = false,

    val isLocked: Boolean = false,

    val lockedName: String? = null,

    // Soft delete support
    val isDeleted: Boolean = false,

    val deletedTimestamp: Long? = null,

    val imageUri: String? = null,

    // Creation & modification timestamps
    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis(),

    // Local timestamp (kept for backward compatibility)
    val timestamp: Long = System.currentTimeMillis(),

    // Synchronization metadata
    val version: Int = 1,

    val syncState: SyncState = SyncState.LOCAL_ONLY,

    val checksum: String = ""
)