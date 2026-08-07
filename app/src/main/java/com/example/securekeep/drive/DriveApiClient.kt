package com.example.securekeep.drive

import android.content.Context
import com.example.securekeep.drive.DriveApiClient.Companion.BACKUP_FILENAME
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Handles all interactions with the Google Drive REST API.
 * Uses OkHttp for HTTP calls and play-services-auth Identity SDK for OAuth2 tokens.
 * All network calls run on [Dispatchers.IO].
 */
class DriveApiClient(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val BACKUP_FILENAME = "securekeep_backup.enc"
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
    }

    /**
     * Requests an OAuth2 access token with drive.appdata scope.
     * If user consent is required, throws [DriveAuthorizationNeededException].
     * The caller's UI layer should launch the pending intent, then call this again.
     */
    suspend fun getAccessToken(): String {
        return suspendCancellableCoroutine { continuation ->
            val authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
                .build()

            Identity.getAuthorizationClient(context)
                .authorize(authRequest)
                .addOnSuccessListener { result ->
                    when {
                        result.hasResolution() -> {
                            val pendingIntent = result.pendingIntent
                            if (pendingIntent != null) {
                                continuation.resumeWithException(
                                    DriveAuthorizationNeededException(pendingIntent)
                                )
                            } else {
                                continuation.resumeWithException(
                                    Exception("Drive authorization required but no PendingIntent provided")
                                )
                            }
                        }
                        result.accessToken != null -> {
                            continuation.resume(result.accessToken!!)
                        }
                        else -> {
                            continuation.resumeWithException(
                                Exception("Authorization succeeded but returned no access token")
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    /**
     * Finds the Drive appDataFolder file ID for the backup file, or null if not present.
     */
    suspend fun findBackupFileId(accessToken: String): String? = withContext(Dispatchers.IO) {
        val encodedQuery = java.net.URLEncoder.encode("name='$BACKUP_FILENAME'", "UTF-8")
        val url = "$DRIVE_API_BASE/files?spaces=appDataFolder&q=$encodedQuery&fields=files(id,name)"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            if (code == 401) throw Exception("Access token expired or revoked (401)")
            if (code == 403) throw Exception("Drive quota exceeded or permission denied (403)")
            return@withContext null
        }

        val body = response.body?.string() ?: return@withContext null
        val json = JSONObject(body)
        val files = json.getJSONArray("files")
        if (files.length() == 0) null else files.getJSONObject(0).getString("id")
    }

    /**
     * Uploads [encryptedData] to Google Drive appDataFolder as [BACKUP_FILENAME].
     * Creates the file if it doesn't exist; overwrites it if it does.
     */
    suspend fun uploadBackup(accessToken: String, encryptedData: ByteArray) = withContext(Dispatchers.IO) {
        val existingFileId = findBackupFileId(accessToken)

        val metadataJson = if (existingFileId == null) {
            JSONObject().apply {
                put("name", BACKUP_FILENAME)
                put("parents", JSONArray().put("appDataFolder"))
            }.toString()
        } else {
            JSONObject().apply { put("name", BACKUP_FILENAME) }.toString()
        }

        val boundary = "SecureKeep_Backup_Boundary_7f3a9b"
        val multipartBody = buildMultipartBody(boundary, metadataJson, encryptedData)
        val contentType = "multipart/related; boundary=$boundary".toMediaType()
        val requestBody = multipartBody.toRequestBody(contentType)

        val url = if (existingFileId != null) {
            "$DRIVE_UPLOAD_BASE/files/$existingFileId?uploadType=multipart"
        } else {
            "$DRIVE_UPLOAD_BASE/files?uploadType=multipart"
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .apply {
                if (existingFileId != null) patch(requestBody) else post(requestBody)
            }
            .build()

        val response = httpClient.newCall(request).execute()
        val code = response.code
        response.close()

        if (!response.isSuccessful) {
            when (code) {
                401 -> throw Exception("Access token expired or revoked (401)")
                403 -> throw Exception("Drive quota exceeded or permission denied (403)")
                404 -> throw Exception("Backup file not found for update (404)")
                else -> throw Exception("Drive upload failed with HTTP $code")
            }
        }
    }

    /**
     * Downloads and returns the raw encrypted backup bytes, or null if no backup exists.
     */
    suspend fun downloadBackup(accessToken: String): ByteArray? = withContext(Dispatchers.IO) {
        val fileId = findBackupFileId(accessToken) ?: return@withContext null

        val request = Request.Builder()
            .url("$DRIVE_API_BASE/files/$fileId?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            when (code) {
                401 -> throw Exception("Access token expired or revoked (401)")
                403 -> throw Exception("Drive access denied (403)")
                404 -> return@withContext null
                else -> throw Exception("Drive download failed with HTTP $code")
            }
        }
        response.body?.bytes()
    }

    /**
     * Builds a multipart/related body for the Drive upload API.
     * Format: --boundary\r\nContent-Type: application/json\r\n\r\n{metadata}\r\n--boundary\r\nContent-Type: application/octet-stream\r\n\r\n{data}\r\n--boundary--
     */
    private fun buildMultipartBody(boundary: String, metadataJson: String, fileData: ByteArray): ByteArray {
        val prefix = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/octet-stream\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)

        val suffix = "\r\n--$boundary--".toByteArray(Charsets.UTF_8)
        return prefix + fileData + suffix
    }
}
