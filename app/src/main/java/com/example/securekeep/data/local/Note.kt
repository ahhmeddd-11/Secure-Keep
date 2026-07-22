package com.example.securekeep.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val color: Long,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val lockedName: String? = null,
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long? = null,
    val imageUri: String? = null,
    val timestamp: Long
)
