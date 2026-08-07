package com.example.securekeep.sync.model

/**
 * Root structure of the encrypted backup file stored in Google Drive appDataFolder.
 * The [version] field allows future schema migrations without losing data.
 */
data class BackupFile(
    val version: Int = 1,
    val backupTime: Long,
    val appId: String = "com.example.securekeep",
    val notes: List<BackupNote>
)
