package com.example.securekeep.drive

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.securekeep.data.local.Note
import com.example.securekeep.data.local.SyncState
import com.example.securekeep.sync.SyncEngine
import com.example.securekeep.sync.crypto.BackupCrypto
import com.example.securekeep.sync.model.BackupFile
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_settings")

/**
 * Orchestrates the full backup and restore lifecycle:
 * 1. Authorise → 2. Serialize → 3. Encrypt → 4. Upload  (backup)
 * 1. Authorise → 2. Download → 3. Decrypt → 4. Parse    (restore)
 *
 * Does NOT interact with Room directly; callers provide notes and receive merged lists.
 */
class DriveSyncRepository(
    private val context: Context,
    private val driveApiClient: DriveApiClient,
    private val gson: Gson
) {

    companion object {
        private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time_ms")
        private val LAST_BACKUP_ACCOUNT = stringPreferencesKey("last_backup_account")
    }

    /** Emits the epoch-millisecond timestamp of the last successful backup, or null. */
    val lastBackupTime: Flow<Long?> = context.syncDataStore.data
        .map { prefs -> prefs[LAST_BACKUP_TIME] }

    /**
     * Returns true if any note's [Note.updatedAt] is newer than [lastBackupMs].
     * This prevents unnecessary uploads when nothing has changed.
     */
    fun hasChangedSinceLastBackup(notes: List<Note>, lastBackupMs: Long): Boolean {
        return notes.any { it.updatedAt > lastBackupMs }
    }

    /**
     * Backs up all [notes] (including soft-deleted ones) to Google Drive appDataFolder.
     * Throws [DriveAuthorizationNeededException] if user consent is needed.
     * Throws generic [Exception] on upload/encryption failure.
     */
    suspend fun backup(notes: List<Note>): Unit = withContext(Dispatchers.IO) {
        val accessToken = driveApiClient.getAccessToken()

        val backupNotes = notes.map { SyncEngine.toBackupNote(it) }
        val backupFile = BackupFile(
            backupTime = System.currentTimeMillis(),
            notes = backupNotes
        )

        val json = gson.toJson(backupFile)
        val encryptedData = BackupCrypto.encrypt(json.toByteArray(Charsets.UTF_8))

        driveApiClient.uploadBackup(accessToken, encryptedData)

        // Persist last backup time after successful upload
        context.syncDataStore.edit { prefs ->
            prefs[LAST_BACKUP_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * Downloads and decrypts the Drive backup, then merges it with [localNotes].
     * Returns the merged list to be upserted into Room by the caller.
     * Returns null if no backup file exists in Drive.
     * Throws [DriveAuthorizationNeededException] if user consent is needed.
     */
    suspend fun restore(localNotes: List<Note>): List<Note>? = withContext(Dispatchers.IO) {
        val accessToken = driveApiClient.getAccessToken()

        val encryptedData = driveApiClient.downloadBackup(accessToken) ?: return@withContext null

        val json = try {
            BackupCrypto.decrypt(encryptedData).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            throw Exception("Backup decryption failed — file may be corrupted or from a different device", e)
        }

        val backupFile = try {
            gson.fromJson(json, BackupFile::class.java)
        } catch (e: Exception) {
            throw Exception("Backup file is not valid JSON — it may be corrupted", e)
        }

        if (backupFile.appId != "com.example.securekeep") {
            throw Exception("Backup file belongs to a different app: ${backupFile.appId}")
        }

        // Merge remote notes with local — SyncEngine handles conflict resolution
        val mergedNotes = SyncEngine.mergeNotes(localNotes, backupFile.notes)

        // Update last backup time to reflect the remote backup's timestamp
        context.syncDataStore.edit { prefs ->
            prefs[LAST_BACKUP_TIME] = backupFile.backupTime
        }

        mergedNotes
    }

    /**
     * Saves the currently signed-in account email for WorkManager to use.
     */
    suspend fun saveAccountEmail(email: String) {
        context.syncDataStore.edit { prefs ->
            prefs[LAST_BACKUP_ACCOUNT] = email
        }
    }

    /** Returns the stored account email, or null if none saved. */
    val savedAccountEmail: Flow<String?> = context.syncDataStore.data
        .map { prefs -> prefs[LAST_BACKUP_ACCOUNT] }

    /** Clears sync preferences when the user signs out. */
    suspend fun clearSyncData() {
        context.syncDataStore.edit { prefs ->
            prefs.remove(LAST_BACKUP_ACCOUNT)
        }
    }
}
