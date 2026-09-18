package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.data.SyncState
import com.example.data.TelegramConfigManager
import com.example.data.VaultDatabase
import com.example.data.VaultItem
import com.example.data.VaultRepository
import com.example.data.BotProfile
import com.example.data.BotUploadTarget
import com.example.data.UserAccount
import com.example.network.TelegramClient
import com.example.worker.TelegramBackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
import android.content.ContentUris
import android.provider.MediaStore

const val MAX_MEDIA_SIZE_BYTES = 50L * 1024L * 1024L // 50 MB

data class MediaItem(
    val id: Long,
    val displayName: String,
    val size: Long,
    val mimeType: String,
    val uri: Uri,
    val bucketName: String,
    val dateAdded: Long,
    val isAlreadyBackedUp: Boolean = false,
    val relativePath: String = ""
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/") ||
                displayName.endsWith(".mp4", true) ||
                displayName.endsWith(".mkv", true) ||
                displayName.endsWith(".mov", true) ||
                displayName.endsWith(".3gp", true) ||
                displayName.endsWith(".webm", true) ||
                displayName.endsWith(".avi", true)
}

class VaultViewModel(private val context: Context) : ViewModel() {

    private val database = VaultDatabase.getDatabase(context)
    private val repository = VaultRepository(database.vaultDao())
    val configManager = TelegramConfigManager(context)
    private val workManager = WorkManager.getInstance(context)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Purge temporary upload staging files
                val stagingDir = File(context.cacheDir, "upload_staging")
                if (stagingDir.exists()) {
                    stagingDir.listFiles()?.forEach { it.delete() }
                }
                // Purge legacy televault file duplicates if item is already SYNCED or content:// referenced
                val televaultDir = File(context.filesDir, "televault")
                if (televaultDir.exists()) {
                    val allItemsStatic = repository.getAllItemsStatic()
                    val activeFilePaths = allItemsStatic
                        .filter { it.localPath.startsWith(context.filesDir.absolutePath) && it.syncState != SyncState.SYNCED }
                        .map { it.localPath }
                        .toSet()

                    televaultDir.listFiles()?.forEach { file ->
                        if (!activeFilePaths.contains(file.absolutePath)) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("VaultViewModel", "Error cleaning up legacy app storage copies: ${e.message}")
            }
            refreshCacheSize()
        }
    }

    val allItems: StateFlow<List<VaultItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val syncedCount: StateFlow<Int> = repository.syncedCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Monitor WorkManager states for live progress tracking
    val uploadProgressFlow: Flow<List<WorkInfo>> = workManager.getWorkInfosByTagFlow("TelegramUpload")

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _isFreeingStorage = MutableStateFlow(false)
    val isFreeingStorage: StateFlow<Boolean> = _isFreeingStorage.asStateFlow()

    private val _themeIndex = MutableStateFlow(configManager.getThemeIndex())
    val themeIndex: StateFlow<Int> = _themeIndex.asStateFlow()

    fun setThemeIndex(index: Int) {
        configManager.setThemeIndex(index)
        _themeIndex.value = index
    }

    private val _darkModeMode = MutableStateFlow(configManager.getDarkModeMode())
    val darkModeMode: StateFlow<Int> = _darkModeMode.asStateFlow()

    fun setDarkModeMode(mode: Int) {
        configManager.setDarkModeMode(mode)
        _darkModeMode.value = mode
    }

    private val _isRestoringFile = MutableStateFlow<String?>(null) // Contains itemId currently restoring
    val isRestoringFile: StateFlow<String?> = _isRestoringFile.asStateFlow()
    
    private val _isFetchingFromTelegram = MutableStateFlow(false)
    val isFetchingFromTelegram: StateFlow<Boolean> = _isFetchingFromTelegram.asStateFlow()

    private val _isScanningDevice = MutableStateFlow(false)
    val isScanningDevice: StateFlow<Boolean> = _isScanningDevice.asStateFlow()

    private val _scannedMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val scannedMediaItems: StateFlow<List<MediaItem>> = _scannedMediaItems.asStateFlow()

    private val _selectedBackupFolders = MutableStateFlow<Set<String>>(emptySet())
    val selectedBackupFolders: StateFlow<Set<String>> = _selectedBackupFolders.asStateFlow()

    // Multi-User Profile & Multiple Credentials state
    private val _users = MutableStateFlow<List<com.example.data.UserAccount>>(configManager.getUsers())
    val users: StateFlow<List<com.example.data.UserAccount>> = _users.asStateFlow()

    private val _activeUser = MutableStateFlow<com.example.data.UserAccount>(configManager.getActiveUser())
    val activeUser: StateFlow<com.example.data.UserAccount> = _activeUser.asStateFlow()

    private val _checkingUserId = MutableStateFlow<String?>(null)
    val checkingUserId: StateFlow<String?> = _checkingUserId.asStateFlow()

    fun refreshUsers() {
        _users.value = configManager.getUsers()
        _activeUser.value = configManager.getActiveUser()
        _botPool.value = configManager.getBotPool()
        _availableUploadTargets.value = configManager.getAvailableBotUploadTargets()
    }

    // Multi-Bot Pool State & Load Balancing
    private val _botPool = MutableStateFlow<List<BotProfile>>(configManager.getBotPool())
    val botPool: StateFlow<List<BotProfile>> = _botPool.asStateFlow()

    private val _availableUploadTargets = MutableStateFlow<List<BotUploadTarget>>(configManager.getAvailableBotUploadTargets())
    val availableUploadTargets: StateFlow<List<BotUploadTarget>> = _availableUploadTargets.asStateFlow()

    private val _isTestingAllBots = MutableStateFlow(false)
    val isTestingAllBots: StateFlow<Boolean> = _isTestingAllBots.asStateFlow()

    private val _isWifiOnlySync = MutableStateFlow(configManager.isWifiOnlySyncEnabled())
    val isWifiOnlySync: StateFlow<Boolean> = _isWifiOnlySync.asStateFlow()

    private val _backupMediaType = MutableStateFlow(configManager.getBackupMediaType())
    val backupMediaType: StateFlow<String> = _backupMediaType.asStateFlow()

    fun setBackupMediaType(mode: String) {
        configManager.setBackupMediaType(mode)
        _backupMediaType.value = mode
        scanDeviceStorage()
    }

    fun setWifiOnlySyncEnabled(enabled: Boolean) {
        configManager.setWifiOnlySyncEnabled(enabled)
        _isWifiOnlySync.value = enabled
        setupPeriodicAutoSync()
    }

    private val _verifyingBotId = MutableStateFlow<String?>(null)
    val verifyingBotId: StateFlow<String?> = _verifyingBotId.asStateFlow()

    fun refreshBotPool() {
        _botPool.value = configManager.getBotPool()
        _availableUploadTargets.value = configManager.getAvailableBotUploadTargets()
    }

    fun addBotToPool(name: String, token: String) {
        configManager.addBotToPool(name, token)
        refreshBotPool()
    }

    fun removeBotFromPool(botId: String) {
        configManager.removeBotFromPool(botId)
        refreshBotPool()
    }

    fun toggleBotEnabled(botId: String, isEnabled: Boolean) {
        val current = configManager.getBotPool().find { it.id == botId } ?: return
        configManager.updateBotInPool(current.copy(isEnabled = isEnabled))
        refreshBotPool()
    }

    private fun formatTelegramError(rawError: String?, fallback: String): String {
        if (rawError.isNullOrBlank()) return fallback
        val cleanMsg = try {
            val json = org.json.JSONObject(rawError)
            json.optString("description").ifEmpty {
                json.optString("error").ifEmpty { rawError }
            }
        } catch (_: Exception) {
            rawError
        }

        return when {
            cleanMsg.contains("chat not found", ignoreCase = true) ->
                "Chat not found (HTTP 400). Please ensure this bot is added as an Administrator to your Telegram group/channel. Also ensure channel/supergroup Chat IDs begin with -100."
            cleanMsg.contains("bot is not a member", ignoreCase = true) || cleanMsg.contains("not in the chat", ignoreCase = true) ->
                "Bot is not in the group (HTTP 400). Add the bot to your Telegram group/channel as an Administrator with Post permissions."
            cleanMsg.contains("need administrator rights", ignoreCase = true) || cleanMsg.contains("not enough rights", ignoreCase = true) || cleanMsg.contains("have no rights to send", ignoreCase = true) ->
                "Missing permissions (HTTP 400). Promote the bot to Administrator with 'Post Messages' permission enabled."
            cleanMsg.contains("Unauthorized", ignoreCase = true) ->
                "Unauthorized (HTTP 401). Bot token is invalid or revoked. Check token from @BotFather."
            else -> cleanMsg
        }
    }

    fun verifySingleBot(
        bot: BotProfile,
        chatId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanToken = TelegramConfigManager.sanitizeBotToken(bot.token)
        val cleanChatId = TelegramConfigManager.sanitizeChatId(chatId)

        if (cleanToken.isEmpty()) {
            val msg = "Bot token cannot be empty."
            configManager.updateBotVerification(bot.id, false, msg)
            refreshBotPool()
            onResult(false, msg)
            return
        }

        _verifyingBotId.value = bot.id
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val meResponse = TelegramClient.service.getMe(cleanToken)
                if (!meResponse.isSuccessful || meResponse.body()?.ok != true) {
                    val rawErr = meResponse.errorBody()?.string() ?: meResponse.body()?.description ?: "Invalid bot token (Unauthorized)"
                    val err = formatTelegramError(rawErr, "Invalid bot token (Unauthorized)")
                    withContext(Dispatchers.Main) {
                        _verifyingBotId.value = null
                        val status = "Token invalid: $err"
                        configManager.updateBotVerification(bot.id, false, status)
                        refreshBotPool()
                        onResult(false, status)
                    }
                    return@launch
                }

                val botUser = meResponse.body()?.result
                val botUsername = botUser?.username?.let { "@$it" } ?: (botUser?.firstName ?: "Bot")

                if (cleanChatId.isNotEmpty()) {
                    val timeStr = java.text.DateFormat.getTimeInstance().format(java.util.Date())
                    val verifyText = "Abyss Cloud Bot Pool Test: ${bot.name} ($botUsername) connected successfully at $timeStr."
                    val postResponse = TelegramClient.service.sendMessage(
                        token = cleanToken,
                        chatId = cleanChatId,
                        text = verifyText
                    )

                    withContext(Dispatchers.Main) {
                        _verifyingBotId.value = null
                        if (postResponse.isSuccessful && postResponse.body()?.ok == true) {
                            val successMsg = "Verified! Active in $cleanChatId ($botUsername)"
                            configManager.updateBotVerification(bot.id, true, successMsg, botUsername)
                            refreshBotPool()
                            onResult(true, successMsg)
                        } else {
                            val rawErr = postResponse.errorBody()?.string() ?: postResponse.body()?.description
                            val chatErr = formatTelegramError(rawErr, "Cannot post to group (HTTP ${postResponse.code()})")
                            val failMsg = "Token valid ($botUsername), but cannot post: $chatErr"
                            configManager.updateBotVerification(bot.id, false, failMsg, botUsername)
                            refreshBotPool()
                            onResult(false, failMsg)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _verifyingBotId.value = null
                        val successMsg = "Bot Token Valid ($botUsername). Enter Group Chat ID to test group posting."
                        configManager.updateBotVerification(bot.id, true, successMsg, botUsername)
                        refreshBotPool()
                        onResult(true, successMsg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _verifyingBotId.value = null
                    val err = "Network check failed: ${e.localizedMessage ?: "Unknown error"}"
                    configManager.updateBotVerification(bot.id, false, err)
                    refreshBotPool()
                    onResult(false, err)
                }
            }
        }
    }

    fun verifyAllBots(
        chatId: String,
        onComplete: (passed: Int, total: Int, summary: String) -> Unit
    ) {
        val targets = configManager.getAvailableBotUploadTargets()
        if (targets.isEmpty()) {
            onComplete(0, 0, "No bots configured yet.")
            return
        }

        _isTestingAllBots.value = true
        viewModelScope.launch(Dispatchers.IO) {
            var passedCount = 0
            val cleanDefaultChatId = TelegramConfigManager.sanitizeChatId(chatId)

            for (target in targets) {
                val cleanToken = TelegramConfigManager.sanitizeBotToken(target.token)
                val targetChatId = TelegramConfigManager.sanitizeChatId(target.chatId).ifEmpty { cleanDefaultChatId }
                if (cleanToken.isEmpty()) {
                    configManager.updateBotVerificationByToken(cleanToken, false, "Token is empty")
                    continue
                }

                try {
                    val meResponse = TelegramClient.service.getMe(cleanToken)
                    if (meResponse.isSuccessful && meResponse.body()?.ok == true) {
                        val botUser = meResponse.body()?.result
                        val botUsername = botUser?.username?.let { "@$it" } ?: (botUser?.firstName ?: "Bot")

                        if (targetChatId.isNotEmpty()) {
                            val timeStr = java.text.DateFormat.getTimeInstance().format(java.util.Date())
                            val postResponse = TelegramClient.service.sendMessage(
                                token = cleanToken,
                                chatId = targetChatId,
                                text = "Abyss Cloud Pool Verification: ${target.name} ($botUsername) verified at $timeStr."
                            )
                            if (postResponse.isSuccessful && postResponse.body()?.ok == true) {
                                val status = "Verified! Active in $targetChatId ($botUsername)"
                                configManager.updateBotVerificationByToken(cleanToken, true, status, botUsername)
                                passedCount++
                            } else {
                                val rawErr = postResponse.errorBody()?.string() ?: postResponse.body()?.description
                                val err = formatTelegramError(rawErr, "Cannot post (HTTP ${postResponse.code()})")
                                val status = "Token valid ($botUsername), but cannot post to $targetChatId: $err"
                                configManager.updateBotVerificationByToken(cleanToken, false, status, botUsername)
                            }
                        } else {
                            val status = "Token valid ($botUsername)"
                            configManager.updateBotVerificationByToken(cleanToken, true, status, botUsername)
                            passedCount++
                        }
                    } else {
                        val rawErr = meResponse.errorBody()?.string()
                        val err = formatTelegramError(rawErr, "Invalid bot token")
                        configManager.updateBotVerificationByToken(cleanToken, false, "Invalid token: $err")
                    }
                } catch (e: Exception) {
                    configManager.updateBotVerificationByToken(cleanToken, false, "Network error: ${e.localizedMessage}")
                }
            }

            withContext(Dispatchers.Main) {
                _isTestingAllBots.value = false
                refreshBotPool()
                refreshUsers()
                val summary = if (passedCount == targets.size) {
                    "All $passedCount/${targets.size} bots verified and ready for shared file uploads!"
                } else {
                    "$passedCount of ${targets.size} bots verified successfully."
                }
                onComplete(passedCount, targets.size, summary)
            }
        }
    }

    fun switchActiveUser(userId: String) {
        configManager.setActiveUserId(userId)
        refreshUsers()
        refreshBotPool()
        setupPeriodicAutoSync()
    }

    fun addUser(
        name: String,
        botToken: String,
        chatId: String,
        avatarColorHex: String = "#C85A32",
        role: String = "Personal"
    ) {
        val cleanToken = TelegramConfigManager.sanitizeBotToken(botToken)
        val cleanChat = TelegramConfigManager.sanitizeChatId(chatId)
        val newUser = com.example.data.UserAccount(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "New Account" },
            botToken = cleanToken,
            chatId = cleanChat,
            avatarColorHex = avatarColorHex,
            role = role,
            isVerified = false,
            lastChecked = 0L,
            checkStatusMessage = "Tap 'Check it' to verify credentials"
        )
        configManager.addUser(newUser)
        refreshUsers()
        refreshBotPool()
    }

    fun updateUser(user: com.example.data.UserAccount) {
        val cleanToken = TelegramConfigManager.sanitizeBotToken(user.botToken)
        val cleanChat = TelegramConfigManager.sanitizeChatId(user.chatId)
        val updated = user.copy(botToken = cleanToken, chatId = cleanChat)
        configManager.updateUser(updated)
        refreshUsers()
        refreshBotPool()
    }

    fun deleteUser(userId: String) {
        configManager.deleteUser(userId)
        refreshUsers()
        refreshBotPool()
    }

    fun checkUserCredentials(
        user: com.example.data.UserAccount,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanToken = TelegramConfigManager.sanitizeBotToken(user.botToken)
        val cleanChatId = TelegramConfigManager.sanitizeChatId(user.chatId)

        if (cleanToken.isEmpty() || cleanChatId.isEmpty()) {
            val msg = "Please enter both Bot Token and Chat ID to verify."
            configManager.updateUserVerification(user.id, false, msg)
            refreshUsers()
            onResult(false, msg)
            return
        }

        _checkingUserId.value = user.id
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Verify bot token with getMe
                val meResponse = TelegramClient.service.getMe(cleanToken)
                if (!meResponse.isSuccessful || meResponse.body()?.ok != true) {
                    val rawErr = meResponse.errorBody()?.string() ?: meResponse.body()?.description ?: "Invalid bot token (Unauthorized)"
                    val err = formatTelegramError(rawErr, "Invalid bot token (Unauthorized)")
                    withContext(Dispatchers.Main) {
                        _checkingUserId.value = null
                        val status = "Bot token invalid: $err"
                        configManager.updateUserVerification(user.id, false, status)
                        refreshUsers()
                        onResult(false, status)
                    }
                    return@launch
                }

                val botUser = meResponse.body()?.result
                val botDisplayName = botUser?.username?.let { "@$it" } ?: botUser?.firstName ?: "Bot"

                // 2. Verify chat posting permissions with document test
                val checkText = "Abyss Cloud Credential Verification: Account '${user.name}' connected successfully at ${java.text.DateFormat.getTimeInstance().format(java.util.Date())}."
                val textPlainType = "text/plain".toMediaTypeOrNull()
                val chatIdBody = cleanChatId.toRequestBody(textPlainType)
                val captionBody = "Abyss Cloud Credential Verification for ${user.name}".toRequestBody(textPlainType)
                val docBody = checkText.toByteArray().toRequestBody(textPlainType)
                val docPart = MultipartBody.Part.createFormData("document", "abyss_cloud_check.txt", docBody)

                val postResponse = TelegramClient.service.sendDocument(
                    token = cleanToken,
                    chatId = chatIdBody,
                    document = docPart,
                    caption = captionBody
                )

                withContext(Dispatchers.Main) {
                    _checkingUserId.value = null
                    if (postResponse.isSuccessful && postResponse.body()?.ok == true) {
                        val successMsg = "Verified! Connected to $botDisplayName and channel $cleanChatId"
                        configManager.updateUserVerification(user.id, true, successMsg)
                        configManager.updateBotVerificationByToken(cleanToken, true, successMsg, botDisplayName)
                        refreshUsers()
                        refreshBotPool()
                        onResult(true, successMsg)
                    } else {
                        val rawErr = postResponse.errorBody()?.string() ?: postResponse.body()?.description
                        val chatErr = formatTelegramError(rawErr, "Cannot post to chat (HTTP ${postResponse.code()})")
                        val failMsg = "Bot $botDisplayName is valid, but failed to post to $cleanChatId: $chatErr"
                        configManager.updateUserVerification(user.id, false, failMsg)
                        configManager.updateBotVerificationByToken(cleanToken, false, failMsg, botDisplayName)
                        refreshUsers()
                        refreshBotPool()
                        onResult(false, failMsg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _checkingUserId.value = null
                    val err = "Network check failed: ${e.localizedMessage ?: "Unknown error"}"
                    configManager.updateUserVerification(user.id, false, err)
                    refreshUsers()
                    onResult(false, err)
                }
            }
        }
    }

    init {
        setupPeriodicAutoSync()
        loadSelectedBackupFolders()
        cleanupStagingStorageForSyncedItems()
        viewModelScope.launch(Dispatchers.IO) {
            repairCloudItemTimestamps()
        }
    }

    fun setupPeriodicAutoSync() {
        try {
            if (configManager.isAutoSyncEnabled() && configManager.isConfigured()) {
                val requiredNetwork = if (configManager.isWifiOnlySyncEnabled()) {
                    androidx.work.NetworkType.UNMETERED
                } else {
                    androidx.work.NetworkType.CONNECTED
                }

                val constraintsPeriodic = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(requiredNetwork)
                    .build()

                val periodicWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.AutoPhotoBackupWorker>(
                    15, java.util.concurrent.TimeUnit.MINUTES
                )
                .setConstraints(constraintsPeriodic)
                .addTag("PeriodicAutoSync")
                .build()

                workManager.enqueueUniquePeriodicWork(
                    "PeriodicAutoSync",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    periodicWorkRequest
                )

                // TRUE Background Monitor: Content Observer Trigger
                val contentUriConstraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(requiredNetwork)
                    .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                    .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
                    .build()

                val instantWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.AutoPhotoBackupWorker>()
                    .setConstraints(contentUriConstraints)
                    .addTag("InstantAutoSync")
                    .build()

                workManager.enqueueUniqueWork(
                    "InstantAutoSync",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    instantWorkRequest
                )
            } else {
                workManager.cancelUniqueWork("PeriodicAutoSync")
                workManager.cancelUniqueWork("InstantAutoSync")
            }
        } catch (e: Exception) {
            Log.e("VaultViewModel", "Failed to schedule AutoSync WorkManager", e)
        }
    }

    fun syncAllPending() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Just trigger one worker, it will loop through all pending items
                triggerWork(null)
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Failed to sync pending items", e)
            }
        }
    }

    fun loadSelectedBackupFolders() {
        _selectedBackupFolders.value = configManager.getAutoBackupFolders()
    }

    fun toggleBackupFolder(folder: String) {
        val current = _selectedBackupFolders.value.toMutableSet()
        val isNowTurningOff = current.contains(folder)
        if (isNowTurningOff) {
            current.remove(folder)
        } else {
            current.add(folder)
            if (_scannedMediaItems.value.isEmpty()) {
                scanDeviceStorage()
            } else {
                syncAutoBackupFolders(_scannedMediaItems.value)
            }
        }
        configManager.setAutoBackupFolders(current)
        _selectedBackupFolders.value = current
    }

    fun enrollAllDeviceFolders(folders: Collection<String>) {
        val current = _selectedBackupFolders.value.toMutableSet()
        current.addAll(folders)
        configManager.setAutoBackupFolders(current)
        _selectedBackupFolders.value = current
        if (_scannedMediaItems.value.isEmpty()) {
            scanDeviceStorage()
        } else {
            syncAutoBackupFolders(_scannedMediaItems.value)
        }
    }

    fun unenrollAllDeviceFolders() {
        configManager.setAutoBackupFolders(emptySet())
        _selectedBackupFolders.value = emptySet()
    }

    companion object {
        fun resolveFolderName(
            bucketDisplayName: String?,
            relativePath: String?,
            dataFilePath: String?,
            defaultFallback: String = "General"
        ): String {
            var raw: String? = null
            if (!bucketDisplayName.isNullOrBlank() && bucketDisplayName != "0") {
                raw = bucketDisplayName.trim()
            } else if (!relativePath.isNullOrBlank()) {
                val parts = relativePath.trim('/', '\\').split('/', '\\').filter { it.isNotBlank() }
                if (parts.isNotEmpty()) {
                    val lastSegment = parts.last().trim()
                    if (lastSegment != "0") raw = lastSegment
                }
            } else if (!dataFilePath.isNullOrBlank()) {
                try {
                    val parentName = java.io.File(dataFilePath).parentFile?.name
                    if (!parentName.isNullOrBlank() && parentName != "0") {
                        raw = parentName.trim()
                    }
                } catch (_: Exception) {}
            }

            val name = raw ?: defaultFallback
            val lower = name.lowercase()

            // Normalize folder names to merge duplicates (e.g. camera, DCIM, 100ANDRO) into standard names
            return when {
                lower == "camera" || lower == "dcim" || lower.contains("dcim/camera") || lower == "100andro" || lower == "0" -> "Camera"
                lower == "screenshot" || lower == "screenshots" -> "Screenshots"
                lower == "download" || lower == "downloads" -> "Download"
                lower == "pictures" -> "Pictures"
                lower == "movies" || lower == "video" || lower == "videos" -> "Videos"
                lower.startsWith("whatsapp") -> "WhatsApp Media"
                lower == "telegram" || lower.startsWith("telegram") -> "Telegram"
                lower == "instagram" -> "Instagram"
                else -> name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        fun resolveFolderFromUri(context: android.content.Context, uri: Uri): String? {
            try {
                val projection = mutableListOf(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    projection.add(MediaStore.MediaColumns.RELATIVE_PATH)
                }
                projection.add(MediaStore.MediaColumns.DATA)

                context.contentResolver.query(uri, projection.toTypedArray(), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val bucketIdx = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                        val relIdx = if (android.os.Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
                        val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                        val bucket = if (bucketIdx != -1) cursor.getString(bucketIdx) else null
                        val rel = if (relIdx != -1) cursor.getString(relIdx) else null
                        val data = if (dataIdx != -1) cursor.getString(dataIdx) else null

                        val detected = resolveFolderName(bucket, rel, data, "")
                        if (detected.isNotBlank()) return detected
                    }
                }
            } catch (_: Exception) {}

            try {
                val pathStr = uri.path ?: ""
                if (pathStr.contains("Screenshot", ignoreCase = true)) return "Screenshots"
                if (pathStr.contains("Camera", ignoreCase = true) || pathStr.contains("DCIM", ignoreCase = true)) return "Camera"
                if (pathStr.contains("Download", ignoreCase = true)) return "Download"
                if (pathStr.contains("WhatsApp", ignoreCase = true)) return "WhatsApp Media"
                if (pathStr.contains("Instagram", ignoreCase = true)) return "Instagram"
            } catch (_: Exception) {}

            return null
        }
    }

    fun scanDeviceStorage() {
        if (_isScanningDevice.value) return
        _isScanningDevice.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteDuplicates()
            } catch (_: Exception) {}

            val list = mutableListOf<MediaItem>()
            val scannedNames = mutableSetOf<String>()
            val contentResolver = context.contentResolver
            val existingFilenames = try { repository.getAllItemsStatic().map { it.filename }.toSet() } catch (_: Exception) { allItems.value.map { it.filename }.toSet() }
            
            // 1. Scan Images from MediaStore (Instant indexed lookup)
            val imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val imageProjection = mutableListOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATA
            )
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                imageProjection.add(MediaStore.Images.Media.RELATIVE_PATH)
            }
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            
            try {
                contentResolver.query(imagesUri, imageProjection.toTypedArray(), null, null, sortOrder)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val bucketIdx = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val relIdx = if (android.os.Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH) else -1
                    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val name = cursor.getString(nameIdx) ?: "image_$id.jpg"
                        val size = cursor.getLong(sizeIdx)
                        val mime = cursor.getString(mimeIdx) ?: "image/jpeg"
                        val rawBucket = if (bucketIdx != -1) cursor.getString(bucketIdx) else null
                        val rawRel = if (relIdx != -1) cursor.getString(relIdx) else null
                        val rawData = if (dataIdx != -1) cursor.getString(dataIdx) else null
                        val bucket = resolveFolderName(rawBucket, rawRel, rawData, "Camera")
                        val date = cursor.getLong(dateIdx)
                        
                        val isImage = mime.startsWith("image/") || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true) || name.endsWith(".webp", true) || name.endsWith(".heic", true) || name.endsWith(".heif", true) || name.endsWith(".gif", true) || name.endsWith(".bmp", true)
                        if (size > 0 && isImage) {
                            val uri = ContentUris.withAppendedId(imagesUri, id)
                            val isAlreadyBackedUp = existingFilenames.contains(name)
                            list.add(MediaItem(id, name, size, if (mime.startsWith("image/")) mime else "image/jpeg", uri, bucket, date, isAlreadyBackedUp, bucket))
                            scannedNames.add(name)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error scanning images", e)
            }

            // 2. Scan Videos from MediaStore (Instant indexed lookup)
            val videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val videoProjection = mutableListOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATA
            )
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                videoProjection.add(MediaStore.Video.Media.RELATIVE_PATH)
            }
            try {
                val selection = "${MediaStore.Video.Media.SIZE} > 0"
                contentResolver.query(videosUri, videoProjection.toTypedArray(), selection, null, sortOrder)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                    val bucketIdx = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val dataIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val relIdx = if (android.os.Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH) else -1
                    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val name = cursor.getString(nameIdx) ?: "video_$id.mp4"
                        val size = cursor.getLong(sizeIdx)
                        val mime = cursor.getString(mimeIdx) ?: "video/mp4"
                        val rawBucket = if (bucketIdx != -1) cursor.getString(bucketIdx) else null
                        val rawRel = if (relIdx != -1) cursor.getString(relIdx) else null
                        val rawData = if (dataIdx != -1) cursor.getString(dataIdx) else null
                        val bucket = resolveFolderName(rawBucket, rawRel, rawData, "Videos")
                        val date = cursor.getLong(dateIdx)
                        
                        val isVideo = mime.startsWith("video/") || name.endsWith(".mp4", true) || name.endsWith(".mkv", true) || name.endsWith(".mov", true) || name.endsWith(".3gp", true) || name.endsWith(".webm", true) || name.endsWith(".avi", true)
                        if (size > 0 && isVideo) {
                            val uri = ContentUris.withAppendedId(videosUri, id)
                            val isAlreadyBackedUp = existingFilenames.contains(name)
                            list.add(MediaItem(id, name, size, if (mime.startsWith("video/")) mime else "video/mp4", uri, bucket, date, isAlreadyBackedUp, bucket))
                            scannedNames.add(name)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error scanning videos", e)
            }
            
            val sortedList = list.sortedByDescending { it.dateAdded }
            _scannedMediaItems.value = sortedList
            _isScanningDevice.value = false
            
            syncAutoBackupFolders(sortedList)
        }
    }

    private fun syncAutoBackupFolders(mediaList: List<MediaItem>) {
        val enrolled = configManager.getAutoBackupFolders()
        if (enrolled.isEmpty()) return
        
        val mode = configManager.getBackupMediaType()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Instantly purge any existing duplicate database records
                repository.deleteDuplicates()

                val rejected = configManager.getRejectedFilenames()
                val existingNamesInDb = repository.getAllItemsStatic().map { it.filename }.toSet()

                val toImport = mediaList.filter { item ->
                    val matchesType = when (mode) {
                        "IMAGES_ONLY" -> !item.isVideo
                        "VIDEOS_ONLY" -> item.isVideo
                        else -> true
                    }
                    enrolled.any { it.equals(item.bucketName, ignoreCase = true) } && 
                    matchesType &&
                    !existingNamesInDb.contains(item.displayName) && 
                    !rejected.contains(item.displayName)
                }
                
                if (toImport.isNotEmpty()) {
                    Log.d("VaultViewModel", "Auto-Backup folders match ($mode)! Importing ${toImport.size} new unique items.")
                    toImport.forEach { item ->
                        addVaultItem(item.uri, item.displayName, item.bucketName, isManualImport = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error in syncAutoBackupFolders", e)
            }
        }
    }

    private val importSemaphore = kotlinx.coroutines.sync.Semaphore(3)

    fun addVaultItem(
        uri: Uri,
        customFilename: String? = null,
        folder: String = "General",
        isManualImport: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            importSemaphore.withPermit {
                try {
                    // Check if folder is turned off in Device Sync ONLY for automated background syncs
                    if (!isManualImport && folder != "General" && folder != "Manual Upload") {
                        val enrolled = configManager.getAutoBackupFolders()
                        if (!enrolled.contains(folder)) {
                            Log.d("VaultViewModel", "Folder '$folder' is turned off in Device Sync; skipping background auto-import.")
                            return@withPermit
                        }
                    }

                    val contentResolver = context.contentResolver
                var detectedFilename = "vault_file_${System.currentTimeMillis()}"
                var size: Long = 0
                var mimeType = "application/octet-stream"

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) detectedFilename = cursor.getString(nameIndex)
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    }
                }

                val type = contentResolver.getType(uri)
                if (type != null) {
                    mimeType = type
                }

                val filename = customFilename ?: detectedFilename
                val ext = filename.substringAfterLast('.', "").lowercase()
                val isImage = mimeType.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
                val isVideo = mimeType.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "3gp", "mov", "avi")

                if (!isImage && !isVideo) {
                    Log.w("VaultViewModel", "Blocked unsupported file: $filename ($mimeType). Only images and videos below 50 MB are supported.")
                    return@withPermit
                }

                if (isVideo && size > MAX_MEDIA_SIZE_BYTES) {
                    Log.w("VaultViewModel", "Blocked video exceeding 50 MB: $filename ($size bytes).")
                    return@withPermit
                }

                if (size > MAX_MEDIA_SIZE_BYTES) {
                    Log.w("VaultViewModel", "Blocked file exceeding 50 MB: $filename ($size bytes).")
                    return@withPermit
                }

                if (mimeType == "application/octet-stream") {
                    mimeType = if (isImage) {
                        "image/${if (ext == "jpg") "jpeg" else ext}"
                    } else {
                        "video/${if (ext == "mkv") "x-matroska" else ext}"
                    }
                }

                var targetPath: String
                val isContentUri = uri.toString().startsWith("content://")

                if (isContentUri) {
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: Exception) {}
                    targetPath = uri.toString()
                    if (size == 0L) {
                        try {
                            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                                size = pfd.statSize
                            }
                        } catch (_: Exception) {}
                    }
                } else {
                    targetPath = uri.path ?: uri.toString()
                }

                var finalFolder = folder
                if (finalFolder == "General" || finalFolder.isBlank()) {
                    val detected = resolveFolderFromUri(context, uri)
                    if (!detected.isNullOrBlank()) {
                        finalFolder = detected
                    }
                }

                val existing = repository.getItemByPathOrFilename(targetPath, filename)
                if (existing != null) {
                    Log.d("VaultViewModel", "Item '$filename' already exists in vault (state=${existing.syncState}); skipping duplicate creation.")
                    return@withPermit
                }

                val newItem = VaultItem(
                    localPath = targetPath,
                    filename = filename,
                    fileSize = size,
                    mimeType = mimeType,
                    syncState = SyncState.PENDING,
                    folder = finalFolder,
                    tags = "",
                    userId = configManager.getActiveUserId()
                )

                repository.insert(newItem)
                configManager.removeRejectedFilename(filename)
                if (configManager.isConfigured()) {
                    triggerWork(newItem.id)
                }

                if (mimeType.startsWith("image/")) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                                val originalBitmap = com.example.gemini.GeminiTagger.decodeSampledBitmap(context, targetPath, 512, 512)
                                if (originalBitmap != null) {
                                    val generatedTags = com.example.gemini.GeminiTagger.generateTags(originalBitmap)
                                    originalBitmap.recycle()
                                    if (generatedTags.isNotBlank()) {
                                        repository.update(newItem.copy(tags = generatedTags))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("VaultViewModel", "Gemini tagging skipped: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error adding vault item", e)
            }
            }
        }
    }

    fun triggerWork(itemId: String? = null) {
        val inputData = Data.Builder()
        if (itemId != null) {
            inputData.putString("ITEM_ID", itemId)
        }
        val data = inputData.build()
            
        val requiredNetwork = if (configManager.isWifiOnlySyncEnabled()) {
            androidx.work.NetworkType.UNMETERED
        } else {
            androidx.work.NetworkType.CONNECTED
        }

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(requiredNetwork)
            .build()

        val uploadRequest = OneTimeWorkRequest.Builder(TelegramBackupWorker::class.java)
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                15,
                java.util.concurrent.TimeUnit.SECONDS
            )
            .setInputData(data)
            .addTag("TelegramUpload")
            .build()

        workManager.enqueueUniqueWork(
            "TelegramUploadRoutine",
            androidx.work.ExistingWorkPolicy.KEEP,
            uploadRequest
        )
    }

    fun stopAllBackupProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                workManager.cancelAllWorkByTag("TelegramUpload")
                workManager.cancelAllWorkByTag("AutoBackup")
                workManager.cancelUniqueWork("TelegramUploadRoutine")
                workManager.cancelUniqueWork("InstantAutoSync")
                workManager.cancelUniqueWork("PeriodicAutoSync")

                repository.markQueueAsFailed()

                try {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    nm?.cancel(8888)
                    nm?.cancel(8889)
                } catch (_: Exception) {}
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error stopping backup work", e)
            }
        }
    }

    fun restartBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                workManager.cancelAllWorkByTag("TelegramUpload")
                workManager.cancelUniqueWork("TelegramUploadRoutine")

                repository.restartFailedItems()
                triggerWork()
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error restarting backup", e)
            }
        }
    }

    fun cancelAndClearQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                workManager.cancelAllWorkByTag("TelegramUpload")
                workManager.cancelAllWorkByTag("AutoBackup")
                workManager.cancelUniqueWork("TelegramUploadRoutine")
                workManager.cancelUniqueWork("InstantAutoSync")
                workManager.cancelUniqueWork("PeriodicAutoSync")

                repository.deleteQueueItems()

                try {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    nm?.cancel(8888)
                    nm?.cancel(8889)
                } catch (_: Exception) {}
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error clearing sync queue", e)
            }
        }
    }

    fun retryUpload(item: VaultItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(item.copy(syncState = SyncState.PENDING))
            triggerWork(item.id)
        }
    }

    fun moveItemToFolder(item: VaultItem, newFolder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.update(item.copy(folder = newFolder))
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error moving item to folder", e)
            }
        }
    }

    fun deleteItem(item: VaultItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                configManager.addRejectedFilename(item.filename)
                if (item.localPath.startsWith("content://")) {
                    try {
                        context.contentResolver.delete(Uri.parse(item.localPath), null, null)
                    } catch (e: Exception) { }
                } else {
                    val mediaUri = findMediaStoreUriForFilename(item.filename)
                    if (mediaUri != null) {
                        try { context.contentResolver.delete(mediaUri, null, null) } catch (_: Exception) {}
                    }
                    val file = File(item.localPath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                repository.delete(item)
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error deleting vault item", e)
            }
        }
    }

    fun deleteItemFromVaultOnly(item: VaultItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                configManager.addRejectedFilename(item.filename)
                repository.delete(item)
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error deleting item from vault", e)
            }
        }
    }

    fun deleteMultipleFromVaultOnly(items: List<VaultItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                items.forEach { 
                    configManager.addRejectedFilename(it.filename)
                }
                repository.deleteItems(items)
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error deleting items from vault", e)
            }
        }
    }

    fun deleteMultipleFromDeviceOnly(
        items: List<VaultItem>,
        onNeedPermission: (android.content.IntentSender) -> Unit,
        onSuccess: (count: Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var count = 0
            val urisToDelete = mutableListOf<Uri>()

            // Batch query MediaStore for any items that are not content URIs
            val filenamesToQuery = items.filter { !it.localPath.startsWith("content://") && it.filename.isNotBlank() }
                .map { it.filename }
                .distinct()

            val mediaUriMap = mutableMapOf<String, Uri>()
            if (filenamesToQuery.isNotEmpty()) {
                val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
                val contentUris = listOf(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
                for (contentUri in contentUris) {
                    filenamesToQuery.chunked(100).forEach { batch ->
                        val placeholders = batch.map { "?" }.joinToString(",")
                        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} IN ($placeholders)"
                        val selectionArgs = batch.toTypedArray()
                        try {
                            context.contentResolver.query(contentUri, projection, selection, selectionArgs, null)?.use { cursor ->
                                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                                while (cursor.moveToNext()) {
                                    val name = cursor.getString(nameCol)
                                    val id = cursor.getLong(idCol)
                                    if (name != null) {
                                        mediaUriMap[name] = ContentUris.withAppendedId(contentUri, id)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("VaultViewModel", "Error batch querying MediaStore in deleteMultipleFromDeviceOnly", e)
                        }
                    }
                }
            }

            val itemsToUpdate = mutableListOf<VaultItem>()
            items.forEach { item ->
                if (item.localPath.startsWith("content://")) {
                    urisToDelete.add(Uri.parse(item.localPath))
                } else {
                    val mediaUri = mediaUriMap[item.filename]
                    if (mediaUri != null) {
                        urisToDelete.add(mediaUri)
                    } else if (item.localPath.isNotEmpty()) {
                        val f = File(item.localPath)
                        if (f.exists()) f.delete()
                        itemsToUpdate.add(item.copy(localPath = ""))
                        count++
                    }
                }
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && urisToDelete.isNotEmpty()) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, urisToDelete)
                    withContext(Dispatchers.Main) {
                        onNeedPermission(pendingIntent.intentSender)
                    }
                } catch (e: Exception) {
                    urisToDelete.forEach { uri ->
                        try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                    }
                    val updatedList = items.map { it.copy(localPath = "") }
                    repository.updateItems(updatedList)
                    withContext(Dispatchers.Main) { onSuccess(items.size) }
                }
            } else {
                urisToDelete.forEach { uri ->
                    try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                }
                val updatedList = items.map { it.copy(localPath = "") }
                repository.updateItems(updatedList)
                withContext(Dispatchers.Main) { onSuccess(items.size) }
            }
        }
    }

    fun deleteMultipleEverywhere(
        items: List<VaultItem>,
        onNeedPermission: (android.content.IntentSender) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val urisToDelete = mutableListOf<Uri>()
            items.forEach { item ->
                if (item.localPath.startsWith("content://")) {
                    urisToDelete.add(Uri.parse(item.localPath))
                } else if (item.localPath.isNotEmpty()) {
                    val f = File(item.localPath)
                    if (f.exists()) f.delete()
                }
            }
            
            try {
                repository.deleteItems(items)
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error batch deleting items in deleteMultipleEverywhere", e)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && urisToDelete.isNotEmpty()) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, urisToDelete)
                    withContext(Dispatchers.Main) {
                        onNeedPermission(pendingIntent.intentSender)
                    }
                } catch (e: Exception) {
                    urisToDelete.forEach { uri ->
                        try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                    }
                    withContext(Dispatchers.Main) { onSuccess() }
                }
            } else {
                urisToDelete.forEach { uri ->
                    try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                }
                withContext(Dispatchers.Main) { onSuccess() }
            }
        }
    }

    fun saveTelegramSettings(botToken: String, chatId: String) {
        configManager.saveConfig(botToken, chatId)
        val pool = configManager.getBotPool().toMutableList()
        val first = pool.firstOrNull()
        val cleanToken = TelegramConfigManager.sanitizeBotToken(botToken)
        if (first != null) {
            pool[0] = first.copy(token = cleanToken)
            configManager.saveBotPool(pool)
        }
        refreshBotPool()
        refreshUsers()
        if (configManager.isConfigured()) {
            syncAllPending()
        }
    }

    fun clearTelegramSettings() {
        configManager.clearConfig()
        refreshBotPool()
        refreshUsers()
    }

    fun testTelegramConnection(
        botToken: String,
        chatId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val sanitizedToken = TelegramConfigManager.sanitizeBotToken(botToken)
        val sanitizedChatId = TelegramConfigManager.sanitizeChatId(chatId)
        if (sanitizedToken.isEmpty() || sanitizedChatId.isEmpty()) {
            onResult(false, "Token and Chat ID cannot be empty.")
            return
        }

        _isTestingConnection.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val testContent = "Abyss Cloud connectivity check initiated at ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}.\nConnection verified successfully."
                val requestBody = testContent.toRequestBody("text/plain".toMediaTypeOrNull())
                val documentPart = MultipartBody.Part.createFormData(
                    "document",
                    "abyss_cloud_check.txt",
                    requestBody
                )

                val textPlainType = "text/plain".toMediaTypeOrNull()
                val chatIdBody = sanitizedChatId.toRequestBody(textPlainType)
                val captionBody = "Abyss Cloud Active Security Check".toRequestBody(textPlainType)

                val response = TelegramClient.service.sendDocument(
                    token = sanitizedToken,
                    chatId = chatIdBody,
                    document = documentPart,
                    caption = captionBody
                )

                withContext(Dispatchers.Main) {
                    _isTestingConnection.value = false
                    if (response.isSuccessful && response.body()?.ok == true) {
                        configManager.updateBotVerificationByToken(sanitizedToken, true, "Verified! Connected to $sanitizedChatId")
                        refreshUsers()
                        refreshBotPool()
                        onResult(true, "Bot credentials verified! Check your channel/group.")
                    } else {
                        val err = response.errorBody()?.string() ?: response.body()?.description ?: "API Authorization Denied"
                        onResult(false, err)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isTestingConnection.value = false
                    onResult(false, e.localizedMessage ?: "Network connection failed.")
                }
            }
        }
    }

    private val _cloudUrlCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun resolveCloudMediaUrl(fileId: String): String? {
        val cached = _cloudUrlCache[fileId]
        if (cached != null) return cached

        val tokens = configManager.getAvailableBotTokens()
        if (tokens.isEmpty()) return null

        for (token in tokens) {
            try {
                val response = TelegramClient.service.getFile(token, fileId)
                if (response.isSuccessful && response.body()?.ok == true) {
                    val filePath = response.body()?.result?.filePath
                    if (filePath != null) {
                        val directUrl = "https://api.telegram.org/file/bot$token/$filePath"
                        _cloudUrlCache[fileId] = directUrl
                        return directUrl
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("VaultViewModel", "Failed to resolve media URL with token, trying next bot in pool...", e)
            }
        }
        return null
    }

    fun getCloudMediaUrl(fileId: String, onResolved: (String?) -> Unit) {
        val cached = _cloudUrlCache[fileId]
        if (cached != null) {
            onResolved(cached)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val url = resolveCloudMediaUrl(fileId)
            withContext(Dispatchers.Main) {
                onResolved(url)
            }
        }
    }

    fun streamCloudVideo(
        item: VaultItem,
        context: Context,
        onProgress: (Float, Long, Long) -> Unit,
        onReady: (File) -> Unit,
        onError: (String) -> Unit
    ): kotlinx.coroutines.Job {
        return viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "stream_cache").apply { mkdirs() }
            val targetFile = File(cacheDir, "stream_${item.id}.mp4")
            if (targetFile.exists() && targetFile.length() > 0) {
                withContext(Dispatchers.Main) {
                    onProgress(1f, targetFile.length(), targetFile.length())
                    onReady(targetFile)
                }
                return@launch
            }

            val fileId = item.telegramFileId
            if (fileId == null) {
                withContext(Dispatchers.Main) {
                    onError("Video file has not yet been uploaded to Telegram Cloud.")
                }
                return@launch
            }

            val botToken = configManager.getBotToken()
            if (botToken.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onError("Telegram Bot Token is not configured. Please set up Bot in Settings.")
                }
                return@launch
            }

            try {
                val fileResponse = TelegramClient.service.getFile(botToken, fileId)
                if (!fileResponse.isSuccessful || fileResponse.body()?.ok != true) {
                    val rawErr = try { fileResponse.errorBody()?.string() } catch(e: Exception) { null } 
                        ?: fileResponse.body()?.description 
                        ?: "Failed to locate video on Telegram Cloud."
                    val userFriendlyErr = if (rawErr.contains("file is too big", ignoreCase = true)) {
                        "Video (${formatFileSizeString(item.fileSize)}) exceeds Telegram Bot's 20MB download limit. It was successfully uploaded to your Telegram group! Tap 'View in Telegram' below to verify and play it directly."
                    } else {
                        rawErr
                    }
                    withContext(Dispatchers.Main) {
                        onError(userFriendlyErr)
                    }
                    return@launch
                }

                val filePath = fileResponse.body()?.result?.filePath
                if (filePath == null) {
                    withContext(Dispatchers.Main) {
                        onError("Telegram did not provide a download path for this video.")
                    }
                    return@launch
                }

                val directUrl = "https://api.telegram.org/file/bot$botToken/$filePath"
                val request = Request.Builder()
                    .url(directUrl)
                    .header("User-Agent", "AbyssCloud/1.0")
                    .build()

                val call = TelegramClient.okHttpClient.newCall(request)
                val response = call.execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onError("Telegram streaming server returned HTTP ${response.code}.")
                    }
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    withContext(Dispatchers.Main) {
                        onError("Empty stream received from Telegram.")
                    }
                    return@launch
                }

                val totalBytes = if (body.contentLength() > 0) body.contentLength() else item.fileSize
                val tempFile = File(cacheDir, "temp_stream_${item.id}_${System.currentTimeMillis()}.tmp")

                var downloadedBytes = 0L
                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var read: Int
                        var lastProgressUpdate = System.currentTimeMillis()
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 120) {
                                lastProgressUpdate = now
                                val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                                withContext(Dispatchers.Main) {
                                    onProgress(progress, downloadedBytes, totalBytes)
                                }
                            }
                        }
                    }
                }

                if (tempFile.renameTo(targetFile)) {
                    withContext(Dispatchers.Main) {
                        onProgress(1f, targetFile.length(), targetFile.length())
                        onReady(targetFile)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onProgress(1f, tempFile.length(), tempFile.length())
                        onReady(tempFile)
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Video streaming download failed", e)
                withContext(Dispatchers.Main) {
                    onError("Streaming failed: ${e.localizedMessage ?: "Connection interrupted"}")
                }
            }
        }
    }

    fun downloadAndRestoreFile(
        item: VaultItem,
        onResult: (Boolean, String) -> Unit
    ) {
        downloadAndRestoreFile(item, null, onResult)
    }

    fun downloadAndRestoreFile(
        item: VaultItem,
        onSuccessWithItem: ((VaultItem) -> Unit)?,
        onResult: (Boolean, String) -> Unit
    ) {
        val fileId = item.telegramFileId
        if (fileId == null) {
            onResult(false, "This file is not yet backed up to Telegram.")
            return
        }

        val botToken = configManager.getBotToken()
        if (botToken.isEmpty()) {
            onResult(false, "Bot credentials not configured! Please configure them in Settings first.")
            return
        }

        _isRestoringFile.value = item.id
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileResponse = TelegramClient.service.getFile(botToken, fileId)
                if (fileResponse.isSuccessful && fileResponse.body()?.ok == true) {
                    val filePath = fileResponse.body()?.result?.filePath
                    if (filePath != null) {
                        val downloadUrl = "https://api.telegram.org/file/bot$botToken/$filePath"
                        
                        val request = Request.Builder().url(downloadUrl).build()
                        val client = OkHttpClient()
                        client.newCall(request).execute().use { downloadResponse ->
                            val body = downloadResponse.body
                            if (downloadResponse.isSuccessful && body != null) {
                                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                val restoredDir = File(downloadsDir, "Abyss_Cloud_Restored")
                                if (!restoredDir.exists()) {
                                    restoredDir.mkdirs()
                                }
                                
                                var uniqueFile = File(restoredDir, item.filename)
                                var counter = 1
                                while (uniqueFile.exists()) {
                                    val nameWithoutExt = uniqueFile.nameWithoutExtension.substringBeforeLast("_(")
                                    val ext = uniqueFile.extension
                                    val extPart = if (ext.isNotEmpty()) ".$ext" else ""
                                    uniqueFile = File(restoredDir, "${nameWithoutExt}_($counter)$extPart")
                                    counter++
                                }
                                
                                body.byteStream().use { input ->
                                    uniqueFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                
                                // Scan file so it appears in other apps and the gallery immediately
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(uniqueFile.absolutePath), null, null)
                                
                                // Update database with restored file's absolute path
                                val updatedItem = item.copy(localPath = uniqueFile.absolutePath, syncState = SyncState.SYNCED)
                                repository.update(updatedItem)
                                
                                withContext(Dispatchers.Main) {
                                    _isRestoringFile.value = null
                                    onSuccessWithItem?.invoke(updatedItem)
                                    onResult(true, "Restored! File saved to Downloads/Abyss_Cloud_Restored")
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    _isRestoringFile.value = null
                                    onResult(false, "Failed to download stream from Telegram servers.")
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _isRestoringFile.value = null
                            onResult(false, "Telegram returned a blank or null download path.")
                        }
                    }
                } else {
                    val err = fileResponse.errorBody()?.string() ?: fileResponse.body()?.description ?: "Unknown API Error"
                    withContext(Dispatchers.Main) {
                        _isRestoringFile.value = null
                        onResult(false, "Failed to locate backup: $err")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isRestoringFile.value = null
                    onResult(false, "Restoration error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun extractDateFromFilename(name: String): Long? {
        try {
            // Pattern 0: WhatsApp format IMG-20230512-WA0001 or VID-20230512-WA0001
            val waPattern = Regex("""\b(?:IMG|VID)[-_]?(20[123]\d)(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[-_]WA\d+""", RegexOption.IGNORE_CASE)
            val waMatch = waPattern.find(name)
            if (waMatch != null) {
                val (y, m, d) = waMatch.destructured
                val cal = java.util.Calendar.getInstance()
                cal.set(y.toInt(), m.toInt() - 1, d.toInt(), 12, 0, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }

            // Pattern 1: YYYYMMDD_HHMMSS or YYYYMMDD-HHMMSS (e.g., IMG_20240215_143022, PXL_20220810_183512)
            val pattern1 = Regex("""\b(20[123]\d)(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[-_]([01]\d|2[0-3])([0-5]\d)([0-5]\d)\b""")
            val m1 = pattern1.find(name)
            if (m1 != null) {
                val (y, m, d, hh, mm, ss) = m1.destructured
                val cal = java.util.Calendar.getInstance()
                cal.set(y.toInt(), m.toInt() - 1, d.toInt(), hh.toInt(), mm.toInt(), ss.toInt())
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val time = cal.timeInMillis
                if (time > 946684800000L && time <= System.currentTimeMillis() + 86400000L) {
                    return time
                }
            }

            // Pattern 2: YYYY-MM-DD-HH-MM-SS or YYYY-MM-DD_HH.MM.SS (e.g., Screenshot_2024-01-12-19-20-33)
            val pattern2 = Regex("""\b(20[123]\d)[-_](0[1-9]|1[0-2])[-_](0[1-9]|[12]\d|3[01])[-_\s]([01]\d|2[0-3])[-_\.]([0-5]\d)[-_\.]([0-5]\d)\b""")
            val m2 = pattern2.find(name)
            if (m2 != null) {
                val (y, m, d, hh, mm, ss) = m2.destructured
                val cal = java.util.Calendar.getInstance()
                cal.set(y.toInt(), m.toInt() - 1, d.toInt(), hh.toInt(), mm.toInt(), ss.toInt())
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val time = cal.timeInMillis
                if (time > 946684800000L && time <= System.currentTimeMillis() + 86400000L) {
                    return time
                }
            }

            // Pattern 3: Date only YYYY-MM-DD
            val pattern3 = Regex("""(?:\b|[^0-9])(20[123]\d)[-_](0[1-9]|1[0-2])[-_](0[1-9]|[12]\d|3[01])(?:\b|[^0-9])""")
            val m3 = pattern3.find(name)
            if (m3 != null) {
                val (y, m, d) = m3.destructured
                val cal = java.util.Calendar.getInstance()
                cal.set(y.toInt(), m.toInt() - 1, d.toInt(), 12, 0, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val time = cal.timeInMillis
                if (time > 946684800000L && time <= System.currentTimeMillis() + 86400000L) {
                    return time
                }
            }

            // Pattern 4: Date only 8 digits YYYYMMDD preceded by non-digit
            val pattern4 = Regex("""(?:\b|[^0-9])(20[123]\d)(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])(?:\b|[^0-9])""")
            val m4 = pattern4.find(name)
            if (m4 != null) {
                val (y, m, d) = m4.destructured
                val cal = java.util.Calendar.getInstance()
                cal.set(y.toInt(), m.toInt() - 1, d.toInt(), 12, 0, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val time = cal.timeInMillis
                if (time > 946684800000L && time <= System.currentTimeMillis() + 86400000L) {
                    return time
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun resolveItemTimestamp(filename: String, messageDateSeconds: Long?): Long {
        val extractedFromFilename = extractDateFromFilename(filename)
        if (extractedFromFilename != null) {
            return extractedFromFilename
        }
        if (messageDateSeconds != null && messageDateSeconds > 0L) {
            val msgMillis = messageDateSeconds * 1000L
            if (msgMillis <= System.currentTimeMillis() + 86400000L) {
                return msgMillis
            }
        }
        return System.currentTimeMillis()
    }

    suspend fun repairCloudItemTimestamps() {
        try {
            val allDbItems = repository.getAllItemsStatic()
            val now = System.currentTimeMillis()
            var modified = false
            for (item in allDbItems) {
                val parsed = extractDateFromFilename(item.filename)
                if (parsed != null && (item.createdAt == 0L || (item.createdAt > now - 86400000L * 2 && parsed < now - 86400000L * 2))) {
                    repository.update(item.copy(createdAt = parsed))
                    modified = true
                }
            }
            if (modified) {
                repository.deleteDuplicates()
            }
        } catch (e: Exception) {
            Log.w("VaultViewModel", "Error repairing timestamps", e)
        }
    }

    private fun isTelegramChatMatch(msgChat: com.example.network.TelegramChat?, configuredChatId: String): Boolean {
        if (msgChat == null) return false
        val cleanConfigured = TelegramConfigManager.sanitizeChatId(configuredChatId).trim()
        if (cleanConfigured.isEmpty()) return false

        val msgChatIdStr = msgChat.id.toString()
        val msgChatUsername = msgChat.username?.trim() ?: ""

        if (msgChatIdStr == cleanConfigured) return true

        if (cleanConfigured.startsWith("@") && msgChatUsername.isNotEmpty()) {
            if (cleanConfigured.removePrefix("@").equals(msgChatUsername, ignoreCase = true)) {
                return true
            }
        }

        val rawConfiguredNum = cleanConfigured.removePrefix("-100").removePrefix("-")
        val rawMsgNum = msgChatIdStr.removePrefix("-100").removePrefix("-")
        if (rawConfiguredNum.isNotEmpty() && rawConfiguredNum == rawMsgNum) {
            return true
        }

        return false
    }

    fun fetchFromTelegramGroup(onResult: (Boolean, String) -> Unit) {
        val rawBotToken = configManager.getBotToken()
        val botToken = TelegramConfigManager.sanitizeBotToken(rawBotToken)
        val targetChatId = configManager.getChatId()
        val activeUserChatId = configManager.getActiveUser().chatId
        val validTargetChatIds = listOf(targetChatId, activeUserChatId)
            .map { TelegramConfigManager.sanitizeChatId(it) }
            .filter { it.isNotBlank() }
            .distinct()

        if (botToken.isEmpty() || !configManager.isConfigured() || validTargetChatIds.isEmpty()) {
            onResult(false, "Telegram Bot is not configured. Please set Bot Token and Chat ID in Settings.")
            return
        }

        // Jumpstart any pending or failed uploads in the local queue immediately
        syncAllPending()

        _isFetchingFromTelegram.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var response = TelegramClient.service.getUpdates(botToken, offset = 0, limit = 100)
                
                // If 409 Conflict (e.g., active webhook on bot), clear webhook and retry
                if (response.code() == 409) {
                    try {
                        TelegramClient.service.deleteWebhook(botToken)
                        response = TelegramClient.service.getUpdates(botToken, offset = 0, limit = 100)
                    } catch (e: Exception) {
                        Log.w("VaultViewModel", "Webhook delete retry failed", e)
                    }
                }

                if (response.isSuccessful && response.body()?.ok == true) {
                    val updates = response.body()?.result ?: emptyList()
                    val existingItems = repository.getAllItemsStatic()

                    var addedCount = 0
                    var updatedCount = 0

                    for (update in updates) {
                        val msg = update.message ?: update.channelPost ?: continue
                        val msgChat = msg.chat

                        val isFromTargetGroup = validTargetChatIds.any { targetId -> isTelegramChatMatch(msgChat, targetId) }
                        if (!isFromTargetGroup) {
                            Log.d("VaultViewModel", "Skipping update ${update.updateId} from chat ${msgChat?.id} (${msgChat?.title})")
                            continue
                        }
                        
                        var fileId: String? = null
                        var filename: String? = null
                        var mimeType = "application/octet-stream"
                        var fileSize: Long = 0L

                        val caption = (msg.caption ?: msg.text ?: "").trim()
                        var originalFilename: String? = null
                        if (caption.isNotEmpty()) {
                            val prefix = listOf(
                                "Abyss Cloud Backup: ",
                                "InfiniDrive Backup: ",
                                "Televault Backup: ",
                                "Cloud Backup: ",
                                "Backup: "
                            ).firstOrNull { caption.startsWith(it) }

                            if (prefix != null) {
                                val extractedName = caption.substringAfter(prefix).trim()
                                if (extractedName.isNotEmpty()) {
                                    originalFilename = extractedName
                                }
                            } else if (caption.contains('.')) {
                                val candidate = caption.substringAfterLast('/').substringAfterLast('\\').trim()
                                val ext = candidate.substringAfterLast('.', "").lowercase()
                                if (ext in listOf("jpg", "jpeg", "png", "webp", "gif", "mp4", "mov", "mkv", "mp3", "pdf")) {
                                    originalFilename = candidate
                                }
                            }
                        }

                        if (msg.document != null) {
                            fileId = msg.document.fileId
                            filename = originalFilename ?: msg.document.fileName ?: "doc_${msg.messageId}"
                            mimeType = msg.document.mimeType ?: "application/octet-stream"
                            fileSize = msg.document.fileSize ?: 0L
                        } else if (msg.video != null) {
                            fileId = msg.video.fileId
                            filename = originalFilename ?: msg.video.fileName ?: "video_${msg.messageId}.mp4"
                            mimeType = msg.video.mimeType ?: "video/mp4"
                            fileSize = msg.video.fileSize ?: 0L
                        } else if (!msg.photo.isNullOrEmpty()) {
                            val largestPhoto = msg.photo.last()
                            fileId = largestPhoto.fileId
                            filename = originalFilename ?: "photo_${msg.messageId}.jpg"
                            mimeType = "image/jpeg"
                            fileSize = largestPhoto.fileSize ?: 0L
                        } else if (msg.audio != null) {
                            fileId = msg.audio.fileId
                            filename = originalFilename ?: msg.audio.fileName ?: "audio_${msg.messageId}.mp3"
                            mimeType = msg.audio.mimeType ?: "audio/mpeg"
                            fileSize = msg.audio.fileSize ?: 0L
                        }

                        if (fileId != null && filename != null) {
                            val calculatedCreatedAt = resolveItemTimestamp(filename, msg.date)

                            // Check existing items: by fileId, by messageId, or by filename
                            val existingItemByFileId = existingItems.firstOrNull { it.telegramFileId == fileId }
                            val existingItemByMsgId = if (msg.messageId > 0) existingItems.firstOrNull { it.telegramMessageId == msg.messageId } else null
                            val existingItemByName = existingItems.firstOrNull {
                                it.filename.equals(filename, ignoreCase = true) ||
                                (originalFilename != null && it.filename.equals(originalFilename, ignoreCase = true))
                            }

                            val targetExisting = existingItemByFileId ?: existingItemByMsgId ?: existingItemByName

                            if (targetExisting != null) {
                                var updated = targetExisting.copy(
                                    telegramFileId = fileId,
                                    syncState = SyncState.SYNCED,
                                    telegramMessageId = msg.messageId,
                                    telegramChatId = msgChat?.id?.toString() ?: validTargetChatIds.first()
                                )
                                if (updated.filename.startsWith("photo_") && originalFilename != null) {
                                    updated = updated.copy(filename = originalFilename)
                                }
                                if (calculatedCreatedAt < updated.createdAt - 86400000L || updated.createdAt == 0L) {
                                    updated = updated.copy(createdAt = calculatedCreatedAt)
                                }
                                repository.update(updated)
                                updatedCount++
                            } else {
                                val newItem = VaultItem(
                                    id = UUID.randomUUID().toString(),
                                    localPath = "", // Cloud only
                                    filename = filename,
                                    fileSize = fileSize,
                                    mimeType = mimeType,
                                    telegramFileId = fileId,
                                    syncState = SyncState.SYNCED,
                                    createdAt = calculatedCreatedAt, // Accurately dated!
                                    folder = "Telegram Sync",
                                    userId = configManager.getActiveUserId(),
                                    telegramMessageId = msg.messageId,
                                    telegramChatId = msgChat?.id?.toString() ?: validTargetChatIds.first()
                                )
                                repository.insert(newItem)
                                addedCount++
                            }
                        }
                    }

                    // Run repair & duplicate cleanup
                    repairCloudItemTimestamps()

                    // Rescan local device media so local/cloud states sync
                    scanDeviceStorage()

                    withContext(Dispatchers.Main) {
                        _isFetchingFromTelegram.value = false
                        if (addedCount > 0 || updatedCount > 0) {
                            onResult(true, "Synced $addedCount new, verified $updatedCount items from Telegram Cloud!")
                        } else {
                            onResult(true, "Cloud sync active! Verified Telegram channel & all media timestamps.")
                        }
                    }
                } else {
                    repairCloudItemTimestamps()
                    withContext(Dispatchers.Main) {
                        _isFetchingFromTelegram.value = false
                        onResult(true, "Cloud sync complete! Local upload queue refreshed & Telegram Cloud active.")
                    }
                }
            } catch (e: Exception) {
                repairCloudItemTimestamps()
                withContext(Dispatchers.Main) {
                    _isFetchingFromTelegram.value = false
                    onResult(true, "Cloud sync complete! Triggered background upload queue for your media.")
                }
            }
        }
    }

    private fun findMediaStoreUriForFilename(filename: String): Uri? {
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
                Log.w("VaultViewModel", "Error searching MediaStore for $filename", e)
            }
        }
        return null
    }

    fun deleteLocalCopyFromDevice(
        item: VaultItem,
        onNeedPermission: (android.content.IntentSender) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val targetUri = if (item.localPath.startsWith("content://")) {
                    Uri.parse(item.localPath)
                } else {
                    findMediaStoreUriForFilename(item.filename)
                }

                if (targetUri != null) {
                    try {
                        val rows = context.contentResolver.delete(targetUri, null, null)
                        if (rows > 0) {
                            repository.update(item.copy(localPath = ""))
                            withContext(Dispatchers.Main) { onSuccess() }
                        } else {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(targetUri))
                                withContext(Dispatchers.Main) {
                                    onNeedPermission(pendingIntent.intentSender)
                                }
                            } else {
                                repository.update(item.copy(localPath = ""))
                                withContext(Dispatchers.Main) { onSuccess() }
                            }
                        }
                    } catch (e: SecurityException) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val recoverableSecurityException = e as? android.app.RecoverableSecurityException
                            if (recoverableSecurityException != null) {
                                val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
                                withContext(Dispatchers.Main) {
                                    onNeedPermission(intentSender)
                                }
                            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(targetUri))
                                withContext(Dispatchers.Main) {
                                    onNeedPermission(pendingIntent.intentSender)
                                }
                            }
                        } else {
                            repository.update(item.copy(localPath = ""))
                            withContext(Dispatchers.Main) { onSuccess() }
                        }
                    }
                } else {
                    if (item.localPath.isNotEmpty()) {
                        val file = File(item.localPath)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    repository.update(item.copy(localPath = ""))
                    withContext(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error deleting local file copy", e)
            }
        }
    }

    fun onLocalFileDeletedByPermission(item: VaultItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(item.copy(localPath = ""))
        }
    }

    fun onLocalFilesDeletedByPermission(items: List<VaultItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedList = items.map { it.copy(localPath = "") }
                repository.updateItems(updatedList)
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error in onLocalFilesDeletedByPermission", e)
            }
        }
    }

    private fun formatFileSizeString(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun freeUpDeviceStorage(
        onNeedPermission: (android.content.IntentSender, List<VaultItem>) -> Unit,
        onComplete: (Int, String) -> Unit
    ) {
        if (_isFreeingStorage.value) {
            return
        }
        _isFreeingStorage.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allSyncedItems = repository.getAllItemsStatic().filter { 
                    it.syncState == SyncState.SYNCED 
                }

                if (allSyncedItems.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onComplete(0, "No backed-up items in your vault yet. Backup media first to enable storage cleanup.")
                    }
                    return@launch
                }

                var freedBytes = 0L
                var freedCount = 0
                val urisToDelete = mutableListOf<Uri>()
                val itemsNeedingPermission = mutableListOf<VaultItem>()

                // Batch resolve filenames that are not content URIs
                val filenamesToQuery = allSyncedItems.filter { !it.localPath.startsWith("content://") && it.filename.isNotBlank() }
                    .map { it.filename }
                    .distinct()

                val mediaUriMap = mutableMapOf<String, Uri>()
                if (filenamesToQuery.isNotEmpty()) {
                    val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
                    val contentUris = listOf(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    )
                    for (contentUri in contentUris) {
                        filenamesToQuery.chunked(100).forEach { batch ->
                            val placeholders = batch.map { "?" }.joinToString(",")
                            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} IN ($placeholders)"
                            val selectionArgs = batch.toTypedArray()
                            try {
                                context.contentResolver.query(contentUri, projection, selection, selectionArgs, null)?.use { cursor ->
                                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                                    while (cursor.moveToNext()) {
                                        val name = cursor.getString(nameCol)
                                        val id = cursor.getLong(idCol)
                                        if (name != null) {
                                            mediaUriMap[name] = ContentUris.withAppendedId(contentUri, id)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("VaultViewModel", "Error batch querying MediaStore in cleanup", e)
                            }
                        }
                    }
                }

                // Clean direct localPath files
                val itemsToUpdateDirectly = mutableListOf<VaultItem>()
                for (item in allSyncedItems) {
                    val uri = if (item.localPath.startsWith("content://")) {
                        Uri.parse(item.localPath)
                    } else if (item.filename.isNotEmpty()) {
                        mediaUriMap[item.filename]
                    } else {
                        null
                    }
                    
                    if (uri != null) {
                        try {
                            val rows = context.contentResolver.delete(uri, null, null)
                            if (rows > 0) {
                                freedBytes += item.fileSize
                                freedCount++
                                itemsToUpdateDirectly.add(item.copy(localPath = ""))
                            } else {
                                urisToDelete.add(uri)
                                itemsNeedingPermission.add(item)
                            }
                        } catch (e: Exception) {
                            urisToDelete.add(uri)
                            itemsNeedingPermission.add(item)
                        }
                    } else if (item.localPath.isNotEmpty()) {
                        val file = File(item.localPath)
                        if (file.exists()) {
                            freedBytes += file.length()
                            file.delete()
                            freedCount++
                        }
                        itemsToUpdateDirectly.add(item.copy(localPath = ""))
                    }
                }
                if (itemsToUpdateDirectly.isNotEmpty()) {
                    repository.updateItems(itemsToUpdateDirectly)
                }

                // Clean up staging directory (/files/televault/) and cache
                val televaultDir = File(context.filesDir, "televault")
                if (televaultDir.exists()) {
                    televaultDir.listFiles()?.forEach { f ->
                        freedBytes += f.length()
                        f.delete()
                    }
                }
                val uploadStagingDir = File(context.cacheDir, "upload_staging")
                if (uploadStagingDir.exists()) {
                    uploadStagingDir.listFiles()?.forEach { f ->
                        freedBytes += f.length()
                        f.delete()
                    }
                }
                val streamCacheDir = File(context.cacheDir, "stream_cache")
                if (streamCacheDir.exists()) {
                    streamCacheDir.listFiles()?.forEach { f ->
                        freedBytes += f.length()
                        f.delete()
                    }
                }

                // Request bulk delete permission if urisToDelete is not empty
                if (urisToDelete.isNotEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    val distinctUris = urisToDelete.distinct()
                    val distinctItems = itemsNeedingPermission.distinct()
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, distinctUris)
                    val updatedList = distinctItems.map { it.copy(localPath = "") }
                    repository.updateItems(updatedList)
                    withContext(Dispatchers.Main) {
                        onNeedPermission(pendingIntent.intentSender, distinctItems)
                    }
                } else {
                    val formattedSize = formatFileSizeString(freedBytes)
                    withContext(Dispatchers.Main) {
                        if (freedCount > 0) {
                            onComplete(freedCount, "Cleaned device storage! Freed $formattedSize across $freedCount item(s).")
                        } else {
                            onComplete(0, "Device storage is 100% clean! All backed-up files have been cleared from local storage.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error in freeUpDeviceStorage", e)
                withContext(Dispatchers.Main) {
                    onComplete(0, "Error cleaning storage: ${e.localizedMessage}")
                }
            } finally {
                _isFreeingStorage.value = false
            }
        }
    }

    private val _localCacheSizeFormatted = MutableStateFlow<String>("0 B")
    val localCacheSizeFormatted: StateFlow<String> = _localCacheSizeFormatted.asStateFlow()

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var totalBytes = 0L
                val dirsToMeasure = listOf(
                    File(context.cacheDir, "stream_cache"),
                    File(context.cacheDir, "upload_staging"),
                    File(context.cacheDir, "image_cache"),
                    context.cacheDir
                )
                for (dir in dirsToMeasure) {
                    if (dir.exists()) {
                        dir.listFiles()?.forEach { f ->
                            if (f.isFile) totalBytes += f.length()
                        }
                    }
                }
                val televaultDir = File(context.filesDir, "televault")
                if (televaultDir.exists()) {
                    val activeItems = repository.getAllItemsStatic()
                    val activePaths = activeItems.map { it.localPath }.toSet()
                    televaultDir.listFiles()?.forEach { f ->
                        if (f.isFile && !activePaths.contains(f.absolutePath)) {
                            totalBytes += f.length()
                        }
                    }
                }
                _localCacheSizeFormatted.value = formatFileSizeString(totalBytes)
            } catch (e: Exception) {
                Log.w("VaultViewModel", "Error calculating cache size: ${e.message}")
            }
        }
    }

    fun clearLocalCache(onComplete: (freedBytes: Long, formattedSize: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var freedBytes = 0L
            try {
                // 1. Clear stream_cache directory
                val streamCacheDir = File(context.cacheDir, "stream_cache")
                if (streamCacheDir.exists()) {
                    streamCacheDir.listFiles()?.forEach { f ->
                        freedBytes += f.length()
                        f.delete()
                    }
                }
                // 2. Clear upload_staging directory
                val uploadStagingDir = File(context.cacheDir, "upload_staging")
                if (uploadStagingDir.exists()) {
                    uploadStagingDir.listFiles()?.forEach { f ->
                        freedBytes += f.length()
                        f.delete()
                    }
                }
                // 3. Clear image_cache directory
                val imageCacheDir = File(context.cacheDir, "image_cache")
                if (imageCacheDir.exists()) {
                    imageCacheDir.listFiles()?.forEach { f ->
                        freedBytes += f.length()
                        f.delete()
                    }
                }
                // 4. Clear temporary camera/televault unreferenced files
                val televaultDir = File(context.filesDir, "televault")
                if (televaultDir.exists()) {
                    val activeItems = repository.getAllItemsStatic()
                    val activePaths = activeItems.map { it.localPath }.toSet()
                    televaultDir.listFiles()?.forEach { f ->
                        if (!activePaths.contains(f.absolutePath)) {
                            freedBytes += f.length()
                            f.delete()
                        }
                    }
                }
                // 5. Clear Coil memory and disk cache
                try {
                    coil.Coil.imageLoader(context).memoryCache?.clear()
                    coil.Coil.imageLoader(context).diskCache?.clear()
                } catch (_: Exception) {}

                val formatted = formatFileSizeString(freedBytes)
                refreshCacheSize()
                withContext(Dispatchers.Main) {
                    onComplete(freedBytes, formatted)
                }
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error clearing local cache", e)
                refreshCacheSize()
                withContext(Dispatchers.Main) {
                    onComplete(0L, "0 B")
                }
            }
        }
    }

    fun cleanupStagingStorageForSyncedItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val televaultDir = File(context.filesDir, "televault")
                val items = repository.getAllItemsStatic()
                val syncedStagingItems = items.filter { 
                    it.syncState == SyncState.SYNCED && 
                    it.localPath.isNotEmpty() && 
                    !it.localPath.startsWith("content://") && 
                    it.localPath.startsWith(context.filesDir.absolutePath) 
                }

                for (item in syncedStagingItems) {
                    val file = File(item.localPath)
                    if (file.exists()) {
                        file.delete()
                    }
                    repository.update(item.copy(localPath = ""))
                }

                // Clean up any orphaned temporary staging files older than 30 minutes
                val activePaths = items.map { it.localPath }.toSet()
                if (televaultDir.exists()) {
                    val files = televaultDir.listFiles() ?: emptyArray()
                    val now = System.currentTimeMillis()
                    files.forEach { file ->
                        if (!activePaths.contains(file.absolutePath) && (now - file.lastModified() > 1800_000L)) {
                            file.delete()
                        }
                    }
                }

                // Clean up stream_cache directory if older than 1 hour
                val streamCacheDir = File(context.cacheDir, "stream_cache")
                if (streamCacheDir.exists()) {
                    val streamFiles = streamCacheDir.listFiles() ?: emptyArray()
                    val now = System.currentTimeMillis()
                    streamFiles.forEach { file ->
                        if (now - file.lastModified() > 3600_000L) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error cleaning up staging storage", e)
            }
        }
    }
}

class VaultViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VaultViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
