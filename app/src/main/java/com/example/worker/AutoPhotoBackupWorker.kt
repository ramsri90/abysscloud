package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.SyncState
import com.example.data.TelegramConfigManager
import com.example.data.VaultDatabase
import com.example.data.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AutoPhotoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private companion object {
        const val NOTIFICATION_ID = 8889
        const val CHANNEL_ID = "abyss_cloud_backup_channel"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createNotification(
            applicationContext,
            "Abyss Cloud Auto-Sync",
            "Scanning device folders for new photos & videos...",
            indeterminate = true
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(
        context: Context,
        title: String,
        content: String,
        progress: Int = 0,
        maxProgress: Int = 100,
        indeterminate: Boolean = false,
        ongoing: Boolean = true
    ): android.app.Notification {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Abyss Cloud Backup",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing background cloud backup status for photos and videos"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setProgress(maxProgress, progress, indeterminate)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun updateNotification(
        context: Context,
        title: String,
        content: String,
        progress: Int = 0,
        maxProgress: Int = 100,
        indeterminate: Boolean = false,
        ongoing: Boolean = true
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = createNotification(context, title, content, progress, maxProgress, indeterminate, ongoing)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w("AutoPhotoBackupWorker", "Error updating notification: ${e.message}")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.w("AutoPhotoBackupWorker", "Could not start foreground service: ${e.message}")
        }

        val configManager = TelegramConfigManager(context)
        val enrolledFolders = configManager.getAutoBackupFolders()

        if (enrolledFolders.isEmpty()) {
            return@withContext Result.success()
        }

        val db = VaultDatabase.getDatabase(context)
        val dao = db.vaultDao()
        val allItems = dao.getAllItemsStatic() // We need a static getter
        val existingFilenames = allItems.map { it.filename }.toSet()
        val rejectedFilenames = configManager.getRejectedFilenames()
        val contentResolver = context.contentResolver

        val mode = configManager.getBackupMediaType()

        var importedCount = 0

        // 1. Scan Images
        if (mode != "VIDEOS_ONLY") {
            val imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val imageProjection = mutableListOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATA
            )
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                imageProjection.add(MediaStore.Images.Media.RELATIVE_PATH)
            }
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val MAX_MEDIA_SIZE_BYTES = 50L * 1024L * 1024L

            try {
                contentResolver.query(imagesUri, imageProjection.toTypedArray(), null, null, sortOrder)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val bucketIdx = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val relIdx = if (android.os.Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH) else -1

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIdx) ?: continue
                        val rawBucket = if (bucketIdx != -1) cursor.getString(bucketIdx) else null
                        val rawRel = if (relIdx != -1) cursor.getString(relIdx) else null
                        val rawData = if (dataIdx != -1) cursor.getString(dataIdx) else null
                        val bucket = com.example.ui.VaultViewModel.resolveFolderName(rawBucket, rawRel, rawData, "Camera")

                        val isEnrolled = enrolledFolders.any { it.equals(bucket, ignoreCase = true) }

                        if (isEnrolled && !existingFilenames.contains(name) && !rejectedFilenames.contains(name)) {
                            val id = cursor.getLong(idIdx)
                            val size = cursor.getLong(sizeIdx)
                            val mime = cursor.getString(mimeIdx) ?: "image/jpeg"
                            
                            // Strict check: only images below 50 MB
                            if (size in 1..MAX_MEDIA_SIZE_BYTES && mime.startsWith("image/")) {
                                val uri = ContentUris.withAppendedId(imagesUri, id)
                                importItem(context, dao, uri, name, size, mime, bucket)
                                importedCount++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AutoPhotoBackup", "Error scanning images", e)
            }
        }

        // 2. Scan Videos (strictly below 50 MB)
        if (mode != "IMAGES_ONLY") {
            val videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val videoProjection = mutableListOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DATA
            )
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                videoProjection.add(MediaStore.Video.Media.RELATIVE_PATH)
            }
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val MAX_MEDIA_SIZE_BYTES = 50L * 1024L * 1024L
            try {
                val videoSelection = "${MediaStore.Video.Media.SIZE} > 0 AND ${MediaStore.Video.Media.SIZE} <= $MAX_MEDIA_SIZE_BYTES"
                contentResolver.query(videosUri, videoProjection.toTypedArray(), videoSelection, null, sortOrder)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                    val bucketIdx = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    val dataIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val relIdx = if (android.os.Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH) else -1

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIdx) ?: continue
                        val rawBucket = if (bucketIdx != -1) cursor.getString(bucketIdx) else null
                        val rawRel = if (relIdx != -1) cursor.getString(relIdx) else null
                        val rawData = if (dataIdx != -1) cursor.getString(dataIdx) else null
                        val bucket = com.example.ui.VaultViewModel.resolveFolderName(rawBucket, rawRel, rawData, "Videos")

                        val isEnrolled = enrolledFolders.any { it.equals(bucket, ignoreCase = true) }

                        if (isEnrolled && !existingFilenames.contains(name) && !rejectedFilenames.contains(name)) {
                            val id = cursor.getLong(idIdx)
                            val size = cursor.getLong(sizeIdx)
                            val mime = cursor.getString(mimeIdx) ?: "video/mp4"

                            // Strict check: only videos below 50 MB
                            if (size in 1..MAX_MEDIA_SIZE_BYTES && mime.startsWith("video/")) {
                                val uri = ContentUris.withAppendedId(videosUri, id)
                                importItem(context, dao, uri, name, size, mime, bucket)
                                importedCount++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AutoPhotoBackup", "Error scanning videos", e)
            }
        }

        if (configManager.isConfigured() && configManager.isAutoSyncEnabled()) {
            val requiredNetwork = if (configManager.isWifiOnlySyncEnabled()) {
                androidx.work.NetworkType.UNMETERED
            } else {
                androidx.work.NetworkType.CONNECTED
            }

            if (importedCount > 0) {
                updateNotification(
                    context,
                    "Abyss Cloud Auto-Sync",
                    "Found $importedCount new photo(s) & video(s). Uploading to cloud...",
                    progress = 100
                )

                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(requiredNetwork)
                    .build()
                
                val uploadRequest = OneTimeWorkRequest.Builder(TelegramBackupWorker::class.java)
                    .setConstraints(constraints)
                    .addTag("TelegramUpload")
                    .build()
                
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "TelegramUploadRoutine",
                    androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                    uploadRequest
                )
            }

            // Re-enqueue the observer
            val contentUriConstraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(requiredNetwork)
                .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
                .build()

            val instantWorkRequest = androidx.work.OneTimeWorkRequestBuilder<AutoPhotoBackupWorker>()
                .setConstraints(contentUriConstraints)
                .addTag("InstantAutoSync")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "InstantAutoSync",
                androidx.work.ExistingWorkPolicy.REPLACE,
                instantWorkRequest
            )
        }

        Result.success()
    }

    private suspend fun importItem(
        context: Context,
        dao: com.example.data.VaultDao,
        uri: android.net.Uri,
        filename: String,
        size: Long,
        mimeType: String,
        folder: String
    ) {
        try {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}

            var finalSize = size
            if (finalSize <= 0L) {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        finalSize = pfd.statSize
                    }
                } catch (_: Exception) {}
            }

            if (finalSize > 50L * 1024L * 1024L || finalSize <= 0L) {
                Log.w("AutoPhotoBackup", "Skipping $filename exceeding 50 MB: $finalSize bytes")
                return
            }

            var tags = ""
            if (mimeType.startsWith("image/")) {
                try {
                    val originalBitmap = com.example.gemini.GeminiTagger.decodeSampledBitmapFromUri(context, uri, 512, 512)
                    if (originalBitmap != null) {
                        tags = com.example.gemini.GeminiTagger.generateTags(originalBitmap)
                        originalBitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.e("AutoPhotoBackup", "Error generating tags", e)
                }
            }

            val newItem = VaultItem(
                localPath = uri.toString(),
                filename = filename,
                fileSize = finalSize,
                mimeType = mimeType,
                syncState = SyncState.PENDING,
                folder = folder,
                tags = tags
            )

            dao.insertItem(newItem)
            Log.d("AutoPhotoBackup", "Enrolled $filename (content URI reference, 0 bytes duplicate storage)")
        } catch (e: Exception) {
            Log.e("AutoPhotoBackup", "Error importing item", e)
        }
    }
}
