package com.example.securekeep.sync.model

/**
 * Transport-only representation of a Note used inside the encrypted backup file.
 * No Room annotations — this is serialized to JSON by Gson.
 */
data class BackupNote(
    val uuid: String,
    val title: String,
    val content: String,
    val color: Long,
    val isPinned: Boolean,
    val isLocked: Boolean,
    val lockedName: String?,
    val isDeleted: Boolean,
    val deletedTimestamp: Long?,
    val imageUri: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val timestamp: Long,
    val version: Int,
    val checksum: String
)
