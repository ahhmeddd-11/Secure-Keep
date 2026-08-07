package com.example.securekeep.sync.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling and cancellation of the daily background backup.
 */
object BackupScheduler {

    private const val WORK_NAME = "securekeep_daily_backup"

    /**
     * Schedules a daily backup job using WorkManager.
     * - Requires any network connection.
     * - Uses KEEP policy so an existing enqueued job is not replaced.
     * - Exponential back-off on failure (starting at 10 minutes).
     */
    fun scheduleDailyBackup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Cancels the daily backup — called when the user signs out.
     */
    fun cancelDailyBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
