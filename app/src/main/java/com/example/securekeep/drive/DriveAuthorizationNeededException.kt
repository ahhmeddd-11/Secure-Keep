package com.example.securekeep.drive

import android.app.PendingIntent

/**
 * Thrown when Google Drive authorization requires user interaction (consent screen).
 * The UI layer catches this, launches the [pendingIntent] via ActivityResultLauncher,
 * then retries the operation once authorization is complete.
 */
class DriveAuthorizationNeededException(
    val pendingIntent: PendingIntent
) : Exception("Google Drive authorization required — user consent needed")
