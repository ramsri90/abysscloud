package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.SyncState
import com.example.data.TelegramConfigManager
import com.example.data.VaultDatabase
import com.example.data.VaultItem
import com.example.network.ProgressRequestBody
import com.example.network.TelegramClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class TelegramBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private companion object {
        const val NOTIFICATION_ID = 8888
        const val CHANNEL_ID = "abyss_cloud_backup_channel"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createNotification(
            applicationContext,
            "Abyss Cloud Backup",
            "Preparing background upload...",
            progress = 0,
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
            Log.w("TelegramBackupWorker", "Error updating notification: ${e.message}")
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val configManager = TelegramConfigManager(context)

        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.w("TelegramBackupWorker", "Could not start foreground service: ${e.message}")
        }

        if (!configManager.isConfigured()) {
            Log.e("TelegramBackupWorker", "Telegram Bot Credentials are not configured.")
            return Result.failure()
        }

        val uploadTargets = configManager.getAvailableBotUploadTargets().filter { it.isEnabled && it.token.isNotBlank() }
        if (uploadTargets.isEmpty()) {
            Log.e("TelegramBackupWorker", "No active bot tokens found in pool or user profiles.")
            return Result.failure()
        }
        val defaultChatId = configManager.getChatId()

        Log.i("TelegramBackupWorker", "Active bot upload pool has ${uploadTargets.size} bot target(s): ${uploadTargets.map { "${it.name} (${it.source})" }.joinToString(", ")}")

        val db = VaultDatabase.getDatabase(context)
        val dao = db.vaultDao()

        val attemptedIds = mutableSetOf<String>()
        var currentItemId = inputData.getString("ITEM_ID")
        var overallResult = Result.success()
        var uploadedCount = 0

        while (true) {
            var item: VaultItem? = null
            if (currentItemId != null) {
                for (i in 1..5) {
                    item = dao.getItemById(currentItemId)
                    if (item != null) break
                    kotlinx.coroutines.delay(200)
                }
                currentItemId = null // clear so next iteration fetches from DB
                
                // Skip if this specific item has already been successfully synced by a previous worker iteration
                if (item != null && item.syncState == SyncState.SYNCED) {
                    item = null 
                }
                
                if (item != null) {
                    attemptedIds.add(item.id)
                }
            } 
            
            if (item == null) {
                val items = dao.getUnsyncedItems()
                item = items.firstOrNull { it.id !in attemptedIds }
                if (item != null) {
                    attemptedIds.add(item.id)
                }
            }

            if (item == null) {
                // Done with all items
                break
            }

            dao.updateItem(item.copy(syncState = SyncState.UPLOADING))

            var stagingFile: File? = null
            val lastProgress = java.util.concurrent.atomic.AtomicInteger(-1)
            val lastBytesTransferred = java.util.concurrent.atomic.AtomicLong(0L)
            var finalContentLength = item.fileSize

            try {
                var streamProvider: (() -> InputStream?)? = null

                // 1. Check content:// URI or local file staging
                val tempStage = File(context.cacheDir, "backup_stage_${System.currentTimeMillis()}_${item.id}.tmp")
                var copySuccess = false

                if (item.localPath.startsWith("content://")) {
                    val contentUri = Uri.parse(item.localPath)
                    try {
                        context.contentResolver.openInputStream(contentUri)?.use { input ->
                            FileOutputStream(tempStage).use { output ->
                                input.copyTo(output)
                            }
                            copySuccess = true
                        }
                    } catch (e: Exception) {
                        Log.w("TelegramBackupWorker", "Content URI stream staging failed for ${item.filename}: ${e.message}")
                    }
                } else if (item.localPath.isNotEmpty()) {
                    val directFile = File(item.localPath)
                    if (directFile.exists() && directFile.length() > 0) {
                        try {
                            directFile.inputStream().use { input ->
                                FileOutputStream(tempStage).use { output ->
                                    input.copyTo(output)
                                }
                                copySuccess = true
                            }
                        } catch (e: Exception) {
                            Log.w("TelegramBackupWorker", "Direct file staging failed for ${item.filename}: ${e.message}")
                        }
                    }
                }

                // 2. Fallback: Query MediaStore for display name
                if (!copySuccess) {
                    val mediaUri = findMediaStoreUriForFilename(context, item.filename)
                    if (mediaUri != null) {
                        try {
                            context.contentResolver.openInputStream(mediaUri)?.use { input ->
                                FileOutputStream(tempStage).use { output ->
                                    input.copyTo(output)
                                }
                                copySuccess = true
                            }
                        } catch (e: Exception) {
                            Log.w("TelegramBackupWorker", "MediaStore Uri stream staging failed for ${item.filename}: ${e.message}")
                        }
                    }
                }

                if (copySuccess && tempStage.exists() && tempStage.length() > 0) {
                    stagingFile = tempStage
                    finalContentLength = tempStage.length()
                    streamProvider = { FileInputStream(tempStage) }
                }

                if (streamProvider == null || finalContentLength <= 0L) {
                    Log.e("TelegramBackupWorker", "Target file stream does not exist on device disk or MediaStore: ${item.filename}")
                    dao.updateItem(item.copy(syncState = SyncState.FAILED, errorMessage = "Local media copy missing on device"))
                    continue
                }

                val actualSize = finalContentLength

                if (actualSize > 50L * 1024L * 1024L) {
                    Log.e("TelegramBackupWorker", "Item exceeds 50 MB limit: ${item.filename} ($actualSize bytes)")
                    dao.updateItem(item.copy(syncState = SyncState.FAILED, errorMessage = "File exceeds 50 MB limit (${formatBytes(actualSize)}). App supports media below 50 MB only."))
                    continue
                }

                val mediaType = item.mimeType.toMediaTypeOrNull()
                val plainTextType = "text/plain".toMediaTypeOrNull()

                val isImage = item.mimeType.startsWith("image/")
                val isVideo = item.mimeType.startsWith("video/")
                // Telegram Bot API sendPhoto limit is 10 MB. Anything larger or non-media must use sendDocument.
                val useAsDocument = actualSize > 48L * 1024 * 1024 || (!isImage && !isVideo) || (isImage && actualSize > 10L * 1024 * 1024)

                val targetCount = uploadTargets.size
                // Distribute consecutive uploads across all connected bots in a round-robin rotation
                val startIdx = configManager.getNextRotatedTargetIndex(targetCount)

                var uploadSuccessful = false
                var lastErrorMessage = "Unknown upload failure"
                var lastHttpCode: Int? = null

                for (targetAttempt in 0 until targetCount) {
                    if (isStopped) {
                        break
                    }

                    val activeTarget = uploadTargets[(startIdx + targetAttempt) % targetCount]
                    val activeToken = TelegramConfigManager.sanitizeBotToken(activeTarget.token)
                    val targetChatId = activeTarget.chatId.trim().ifEmpty {
                        defaultChatId.trim().ifEmpty {
                            configManager.getActiveUser().chatId.trim()
                        }
                    }

                    if (targetChatId.isEmpty()) {
                        lastErrorMessage = "Telegram Chat ID is missing. Please configure Chat ID in Settings."
                        Log.w("TelegramBackupWorker", lastErrorMessage)
                        continue
                    }

                    val chatIdBody = targetChatId.toRequestBody(plainTextType)
                    val captionBody = "Abyss Cloud Backup: ${item.filename}".toRequestBody(plainTextType)

                    Log.i("TelegramBackupWorker", "Uploading ${item.filename} (${formatBytes(actualSize)}) using bot '${activeTarget.name}' (attempt ${targetAttempt + 1}/$targetCount) to chat $targetChatId")

                    val progressBody = ProgressRequestBody(
                        contentType = mediaType,
                        contentLength = finalContentLength,
                        onBytesProgress = { bytesRead, totalBytes, progress ->
                            lastBytesTransferred.set(bytesRead)
                            val last = lastProgress.get()
                            if (progress - last >= 1 || progress == 100 || (last == -1 && progress >= 0)) {
                                if (lastProgress.compareAndSet(last, progress)) {
                                    try {
                                        setProgressAsync(
                                            workDataOf(
                                                "PROGRESS" to progress,
                                                "BYTES_READ" to bytesRead,
                                                "BYTES_TOTAL" to (if (totalBytes > 0) totalBytes else finalContentLength),
                                                "ITEM_ID" to item.id
                                            )
                                        )
                                        updateNotification(
                                            context,
                                            "Abyss Cloud Backup",
                                            "Backing up ${item.filename} (${formatBytes(bytesRead)} / ${formatBytes(if (totalBytes > 0) totalBytes else finalContentLength)})",
                                            progress = progress
                                        )
                                    } catch (e: Exception) {
                                        Log.w("TelegramBackupWorker", "Failed to set progress: ${e.message}")
                                    }
                                }
                            }
                        },
                        openInputStream = {
                            streamProvider()
                        }
                    )

                    try {
                        var response = if (useAsDocument) {
                            val documentPart = MultipartBody.Part.createFormData(
                                "document",
                                item.filename,
                                progressBody
                            )
                            TelegramClient.service.sendDocument(
                                token = activeToken,
                                chatId = chatIdBody,
                                document = documentPart,
                                caption = captionBody
                            )
                        } else if (isImage) {
                            val photoPart = MultipartBody.Part.createFormData(
                                "photo",
                                item.filename,
                                progressBody
                            )
                            TelegramClient.service.sendPhoto(
                                token = activeToken,
                                chatId = chatIdBody,
                                photo = photoPart,
                                caption = captionBody
                            )
                        } else {
                            val videoPart = MultipartBody.Part.createFormData(
                                "video",
                                item.filename,
                                progressBody
                            )
                            val streamingBody = "true".toRequestBody(plainTextType)
                            TelegramClient.service.sendVideo(
                                token = activeToken,
                                chatId = chatIdBody,
                                video = videoPart,
                                caption = captionBody,
                                supportsStreaming = streamingBody
                            )
                        }

                        // Fallback to sendDocument if photo or video is rejected due to size/dimensions/format
                        if (!response.isSuccessful && !useAsDocument && (isImage || isVideo)) {
                            val errorBodyStr = try { response.errorBody()?.string() } catch(e:Exception) { null }
                            Log.w("TelegramBackupWorker", "Media upload failed (${response.code()}), falling back to sendDocument. Error: $errorBodyStr")
                            
                            val fallbackProgressBody = ProgressRequestBody(
                                contentType = mediaType,
                                contentLength = finalContentLength,
                                onBytesProgress = { bytesRead, totalBytes, progress ->
                                    lastBytesTransferred.set(bytesRead)
                                    val last = lastProgress.get()
                                    if (progress - last >= 1 || progress == 100 || (last == -1 && progress >= 0)) {
                                        if (lastProgress.compareAndSet(last, progress)) {
                                            try {
                                                setProgressAsync(
                                                    workDataOf(
                                                        "PROGRESS" to progress,
                                                        "BYTES_READ" to bytesRead,
                                                        "BYTES_TOTAL" to (if (totalBytes > 0) totalBytes else finalContentLength),
                                                        "ITEM_ID" to item.id
                                                    )
                                                )
                                            } catch (e: Exception) {
                                                Log.w("TelegramBackupWorker", "Failed to set fallback progress: ${e.message}")
                                            }
                                        }
                                    }
                                },
                                openInputStream = {
                                    streamProvider()
                                }
                            )
                            val fallbackDocumentPart = MultipartBody.Part.createFormData(
                                "document",
                                item.filename,
                                fallbackProgressBody
                            )
                            response = TelegramClient.service.sendDocument(
                                token = activeToken,
                                chatId = chatIdBody,
                                document = fallbackDocumentPart,
                                caption = captionBody
                            )
                        }

                        if (response.isSuccessful && response.body()?.ok == true) {
                            val messageResult = response.body()?.result
                            val telegramFileId = messageResult?.document?.fileId
                                ?: messageResult?.photo?.lastOrNull()?.fileId
                                ?: messageResult?.video?.fileId
                                ?: messageResult?.audio?.fileId
                                ?: messageResult?.voice?.fileId
                                ?: messageResult?.videoNote?.fileId
                                ?: messageResult?.animation?.fileId

                            val messageId = messageResult?.messageId
                            val resultChatId = messageResult?.chat?.id?.toString() ?: targetChatId
                            
                            if (telegramFileId != null) {
                                var finalLocalPath = item.localPath
                                if (!finalLocalPath.startsWith("content://") && finalLocalPath.startsWith(context.filesDir.absolutePath)) {
                                    try {
                                        val stagingCopy = File(finalLocalPath)
                                        if (stagingCopy.exists()) {
                                            stagingCopy.delete()
                                            Log.i("TelegramBackupWorker", "Freed staging copy for ${item.filename} after cloud sync.")
                                        }
                                    } catch (e: Exception) {
                                        Log.w("TelegramBackupWorker", "Failed to delete staging copy: ${e.message}", e)
                                    }
                                    finalLocalPath = ""
                                }

                                dao.updateItem(
                                    item.copy(
                                        localPath = finalLocalPath,
                                        fileSize = actualSize,
                                        syncState = SyncState.SYNCED,
                                        telegramFileId = telegramFileId,
                                        telegramMessageId = messageId,
                                        telegramChatId = resultChatId,
                                        errorMessage = null
                                    )
                                )
                                Log.i("TelegramBackupWorker", "Successfully uploaded ${item.filename} (${formatBytes(actualSize)}) using bot '${activeTarget.name}' to chat $resultChatId")
                                uploadSuccessful = true
                                uploadedCount++
                                updateNotification(
                                    context,
                                    "Abyss Cloud Backup",
                                    "Backed up ${item.filename} successfully!",
                                    progress = 100
                                )
                                break // Completed upload successfully!
                            } else {
                                lastErrorMessage = "Upload API returned success but no file ID was detected."
                            }
                        } else {
                            val code = response.code()
                            lastHttpCode = code
                            val errStr = try {
                                val body = response.errorBody()?.string()
                                if (!body.isNullOrEmpty()) {
                                    try { org.json.JSONObject(body).optString("description", body) } catch (e: Exception) { body }
                                } else {
                                    response.body()?.description ?: "HTTP $code"
                                }
                            } catch (e: Exception) {
                                "HTTP $code"
                            }
                            lastErrorMessage = errStr
                            Log.w("TelegramBackupWorker", "Bot target attempt ${targetAttempt + 1} (${activeTarget.name}) failed ($code: $errStr)")

                            // If 429 Too Many Requests or 5xx server error, rotate to next bot immediately
                            if ((code == 429 || code in 500..599) && targetAttempt < targetCount - 1) {
                                Log.i("TelegramBackupWorker", "Rate limit/server error on bot ${activeTarget.name}. Rotating to next bot in pool...")
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException || isStopped) {
                            throw e
                        }
                        lastErrorMessage = e.localizedMessage ?: e.message ?: "Network error"
                        Log.w("TelegramBackupWorker", "Bot attempt ${targetAttempt + 1} (${activeTarget.name}) exception: $lastErrorMessage")
                        if ((e is java.io.IOException || e is java.net.SocketTimeoutException) && targetAttempt < targetCount - 1) {
                            Log.i("TelegramBackupWorker", "Socket timeout on bot ${activeTarget.name}. Rotating to next bot in pool...")
                            continue
                        }
                    }
                }

                if (uploadSuccessful) {
                    continue
                }

                val transferred = lastBytesTransferred.get()
                val total = if (finalContentLength > 0) finalContentLength else actualSize
                val bytesInfo = if (total > 0 && transferred > 0) {
                    " (${formatBytes(transferred)} / ${formatBytes(total)} uploaded)"
                } else if (total > 0) {
                    " (0 B / ${formatBytes(total)} uploaded)"
                } else ""

                if (isStopped) {
                    Log.w("TelegramBackupWorker", "Backup worker paused by system. Item stays queued for next run.")
                    dao.updateItem(item.copy(syncState = SyncState.PENDING, errorMessage = null))
                    overallResult = Result.retry()
                    break
                }

                if (runAttemptCount < 3 && (lastHttpCode == 429 || (lastHttpCode != null && lastHttpCode in 500..599))) {
                    dao.updateItem(item.copy(syncState = SyncState.PENDING, errorMessage = "All bots busy ($lastErrorMessage)$bytesInfo, retrying..."))
                    overallResult = Result.retry()
                    break
                }

                val fullErrorMessage = "$lastErrorMessage$bytesInfo"
                Log.e("TelegramBackupWorker", "All $targetCount bots failed for ${item.filename}: $fullErrorMessage")
                dao.updateItem(item.copy(syncState = SyncState.FAILED, errorMessage = fullErrorMessage))
                continue

            } catch (e: Exception) {
                val transferred = lastBytesTransferred.get()
                val total = if (finalContentLength > 0) finalContentLength else item.fileSize
                val bytesInfo = if (total > 0 && transferred > 0) {
                    " at ${formatBytes(transferred)} / ${formatBytes(total)}"
                } else ""

                if (e is kotlinx.coroutines.CancellationException || isStopped) {
                    val termMsg = "Backup worker terminated by system$bytesInfo. Tap retry to resume."
                    Log.w("TelegramBackupWorker", termMsg)
                    dao.updateItem(item.copy(syncState = SyncState.FAILED, errorMessage = termMsg))
                    overallResult = Result.retry()
                    throw e
                }

                Log.e("TelegramBackupWorker", "Worker exception during item processing", e)
                val exMsg = e.localizedMessage ?: e.message ?: "Unknown Exception"
                val fullErrMsg = "$exMsg$bytesInfo"
                if (runAttemptCount < 3 && e is java.io.IOException) {
                    dao.updateItem(item.copy(syncState = SyncState.PENDING, errorMessage = "Network offline or timeout$bytesInfo, retrying..."))
                    overallResult = Result.retry()
                    break
                }
                dao.updateItem(item.copy(syncState = SyncState.FAILED, errorMessage = fullErrMsg))
                continue
            } finally {
                try {
                    stagingFile?.delete()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        
        if (uploadedCount > 0) {
            updateNotification(
                context,
                "Abyss Cloud Backup Complete",
                "Successfully backed up $uploadedCount media file(s) to Telegram cloud.",
                progress = 100,
                ongoing = false
            )
        } else {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
            } catch (_: Exception) {}
        }

        return overallResult
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun findMediaStoreUriForFilename(context: Context, filename: String): Uri? {
        if (filename.isBlank()) return null
        val contentUris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(filename)

        for (contentUri in contentUris) {
            try {
                context.contentResolver.query(contentUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val id = cursor.getLong(idCol)
                        return ContentUris.withAppendedId(contentUri, id)
                    }
                }
            } catch (e: Exception) {
                Log.w("TelegramBackupWorker", "Error searching MediaStore for $filename", e)
            }
        }
        return null
    }
}
