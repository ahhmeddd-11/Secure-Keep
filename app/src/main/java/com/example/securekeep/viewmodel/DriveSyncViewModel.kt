package com.example.securekeep.viewmodel

import android.app.PendingIntent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securekeep.data.local.Note
import com.example.securekeep.drive.DriveAuthorizationNeededException
import com.example.securekeep.drive.DriveSyncRepository
import com.example.securekeep.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Represents the current state of any sync operation. */
sealed class SyncStatus {
    object Idle : SyncStatus()
    object BackingUp : SyncStatus()
    object Restoring : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Failed(val message: String) : SyncStatus()
}

/** Identifies which operation is pending authorization. */
enum class PendingOperation { BACKUP, RESTORE }

/**
 * Manages backup and restore operations, including Drive authorization flow.
 * Exposes:
 * - [syncStatus] for the UI to show loading / success / failure states.
 * - [pendingAuthIntent] — non-null when a Drive consent screen must be shown.
 * - [lastBackupTime] — live timestamp of the last successful backup.
 */
class DriveSyncViewModel(
    private val syncRepository: DriveSyncRepository,
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _pendingAuthIntent = MutableStateFlow<PendingIntent?>(null)
    val pendingAuthIntent: StateFlow<PendingIntent?> = _pendingAuthIntent.asStateFlow()

    private var pendingOperation: PendingOperation? = null

    val lastBackupTime: Flow<Long?> = syncRepository.lastBackupTime

    /**
     * Initiates a manual backup of all notes to Google Drive.
     * If Drive auth is needed, emits a [pendingAuthIntent] for the UI to handle.
     */
    fun backup() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.BackingUp
            try {
                val allNotes = withContext(Dispatchers.IO) { notesRepository.getAllNotes() }
                withContext(Dispatchers.IO) { syncRepository.backup(allNotes) }
                _syncStatus.value = SyncStatus.Success("Backup completed successfully")
                Log.d("DriveSyncVM", "Backup succeeded")
            } catch (e: DriveAuthorizationNeededException) {
                pendingOperation = PendingOperation.BACKUP
                _pendingAuthIntent.value = e.pendingIntent
                _syncStatus.value = SyncStatus.Idle
                Log.d("DriveSyncVM", "Drive auth required for backup")
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Failed(friendlyError(e))
                Log.e("DriveSyncVM", "Backup failed", e)
            }
        }
    }

    /**
     * Downloads the Drive backup, merges it with local notes, and saves the result.
     * If Drive auth is needed, emits a [pendingAuthIntent] for the UI to handle.
     */
    fun restore() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Restoring
            try {
                val localNotes = withContext(Dispatchers.IO) { notesRepository.getAllNotes() }
                val mergedNotes = withContext(Dispatchers.IO) { syncRepository.restore(localNotes) }

                if (mergedNotes == null) {
                    _syncStatus.value = SyncStatus.Failed("No backup found in Google Drive")
                    return@launch
                }

                withContext(Dispatchers.IO) { notesRepository.upsertNotes(mergedNotes) }
                _syncStatus.value = SyncStatus.Success("${mergedNotes.size} notes restored")
                Log.d("DriveSyncVM", "Restore succeeded — ${mergedNotes.size} notes")
            } catch (e: DriveAuthorizationNeededException) {
                pendingOperation = PendingOperation.RESTORE
                _pendingAuthIntent.value = e.pendingIntent
                _syncStatus.value = SyncStatus.Idle
                Log.d("DriveSyncVM", "Drive auth required for restore")
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Failed(friendlyError(e))
                Log.e("DriveSyncVM", "Restore failed", e)
            }
        }
    }

    /**
     * Called by the UI after the Drive consent screen activity returns successfully.
     * Retries whichever operation was pending before auth was required.
     */
    fun onAuthorizationCompleted() {
        _pendingAuthIntent.value = null
        when (pendingOperation) {
            PendingOperation.BACKUP -> backup()
            PendingOperation.RESTORE -> restore()
            null -> { /* Nothing pending */ }
        }
        pendingOperation = null
    }

    /**
     * Called when the user dismisses the auth flow without granting permission.
     */
    fun onAuthorizationCancelled() {
        _pendingAuthIntent.value = null
        pendingOperation = null
        _syncStatus.value = SyncStatus.Failed("Google Drive authorization was cancelled")
    }

    /** Resets the status back to Idle after the UI has displayed a result. */
    fun clearStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    /**
     * Persists the signed-in account email so WorkManager can perform background backups.
     */
    fun onUserSignedIn(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            syncRepository.saveAccountEmail(email)
        }
    }

    /**
     * Clears the stored account email when the user signs out.
     */
    fun onUserSignedOut() {
        viewModelScope.launch(Dispatchers.IO) {
            syncRepository.clearSyncData()
        }
    }

    private fun friendlyError(e: Exception): String {
        val msg = e.message ?: "Unknown error"
        return when {
            "401" in msg -> "Authorization expired — please sign in again"
            "403" in msg -> "Google Drive access denied or quota exceeded"
            "404" in msg -> "Backup file not found on Drive"
            "No network" in msg || "UnknownHost" in msg -> "No internet connection"
            "corrupted" in msg -> "Backup file is corrupted or from a different device"
            "quota" in msg -> "Google Drive quota exceeded"
            else -> msg
        }
    }
}
