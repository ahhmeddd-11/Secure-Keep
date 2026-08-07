package com.example.securekeep.sync.worker

import android.content.Context
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
                return Result.success()
            }

            // Fetch all notes (active + deleted)
            val allNotes = notesRepository.getAllNotes()

            // Skip backup if nothing has changed
            val lastBackupTime = syncRepository.lastBackupTime.first() ?: 0L
            if (!syncRepository.hasChangedSinceLastBackup(allNotes, lastBackupTime)) {
                return Result.success()
            }

            syncRepository.backup(allNotes)
            Result.success()

        } catch (e: DriveAuthorizationNeededException) {
            // User must interact to re-authorize — don't retry automatically
            Result.failure()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
