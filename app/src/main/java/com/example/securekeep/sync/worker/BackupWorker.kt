package com.example.securekeep.sync.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.securekeep.data.local.DatabaseProvider
import com.example.securekeep.drive.DriveApiClient
import com.example.securekeep.drive.DriveAuthorizationNeededException
import com.example.securekeep.drive.DriveSyncRepository
import com.example.securekeep.repository.NotesRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

/**
 * Daily background backup worker.
 * - Runs only when network is available (enforced by WorkManager constraints).
 * - Skips upload if no notes have changed since the last backup.
 * - Returns [Result.retry] on transient errors (will retry with exponential back-off).
 * - Returns [Result.failure] on auth errors (no point retrying without user interaction).
 */
class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        val dao = DatabaseProvider.provideDatabase(context).noteDao()
        val notesRepository = NotesRepository(dao)

        val driveApiClient = DriveApiClient(context)
        val syncRepository = DriveSyncRepository(context, driveApiClient, Gson())

        return try {
            // Check if a user account is linked
            val accountEmail = syncRepository.savedAccountEmail.first()
            if (accountEmail.isNullOrBlank()) {
                Log.d("BackupWorker", "No account linked — skipping backup")
                return Result.success()
            }

            // Fetch all notes (active + deleted)
            val allNotes = notesRepository.getAllNotes()

            // Skip backup if nothing has changed
            val lastBackupTime = syncRepository.lastBackupTime.first() ?: 0L
            if (!syncRepository.hasChangedSinceLastBackup(allNotes, lastBackupTime)) {
                Log.d("BackupWorker", "No changes since last backup — skipping")
                return Result.success()
            }

            syncRepository.backup(allNotes)
            Log.d("BackupWorker", "Backup completed successfully")
            Result.success()

        } catch (e: DriveAuthorizationNeededException) {
            // User must interact to re-authorize — don't retry automatically
            Log.w("BackupWorker", "Drive auth required — cannot auto-backup")
            Result.failure()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Backup failed — will retry", e)
            Result.retry()
        }
    }
}
