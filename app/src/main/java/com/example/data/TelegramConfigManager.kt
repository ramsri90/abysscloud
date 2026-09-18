package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TelegramConfigManager(context: Context) {
    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "televault_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        android.util.Log.e("TelegramConfigManager", "Failed to initialize EncryptedSharedPreferences, attempting reset...", e)
        try {
            // Try deleting the corrupted pref file and recreate 
            context.deleteSharedPreferences("televault_secure_prefs")
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "televault_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (ex: Exception) {
            android.util.Log.e("TelegramConfigManager", "Fatal fallback to private SharedPreferences due to Android system Keystore failure", ex)
            context.getSharedPreferences("televault_clear_prefs", Context.MODE_PRIVATE)
        }
    }

    // Multi-User & Credentials Support
    fun getUsers(): List<UserAccount> {
        val jsonStr = sharedPreferences.getString(KEY_USERS_LIST, null)
        if (jsonStr.isNullOrEmpty()) {
            // Initialize with dynamic clean credentials
            val defaultToken = (sharedPreferences.getString(KEY_BOT_TOKEN, "") ?: "").trim()
            val defaultChatId = (sharedPreferences.getString(KEY_CHAT_ID, "") ?: "").trim()

            val primary = UserAccount(
                id = "default_primary_user",
                name = "Personal Vault",
                botToken = defaultToken,
                chatId = defaultChatId,
                avatarColorHex = "#C85A32",
                role = "Personal",
                isVerified = false,
                lastChecked = 0L,
                checkStatusMessage = if (defaultToken.isNotEmpty()) "Tap 'Check it' to verify credentials" else "Configure Bot Token & Chat ID in Settings"
            )
            val list = listOf(primary)
            saveUsers(list)
            setActiveUserId(primary.id)
            return list
        }

        return try {
            val jsonArray = org.json.JSONArray(jsonStr)
            val list = mutableListOf<UserAccount>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(UserAccount.fromJson(obj))
            }
            if (list.isEmpty()) {
                val fallback = UserAccount(
                    id = "default_primary_user",
                    name = "Personal Vault",
                    botToken = getBotToken(),
                    chatId = getChatId(),
                    avatarColorHex = "#C85A32",
                    role = "Personal"
                )
                listOf(fallback)
            } else list
        } catch (e: Exception) {
            android.util.Log.e("TelegramConfigManager", "Failed to parse users list JSON", e)
            listOf(
                UserAccount(
                    id = "default_primary_user",
                    name = "Personal Vault",
                    botToken = getBotToken(),
                    chatId = getChatId(),
                    avatarColorHex = "#C85A32",
                    role = "Personal"
                )
            )
        }
    }

    fun saveUsers(users: List<UserAccount>) {
        val jsonArray = org.json.JSONArray()
        users.forEach { jsonArray.put(it.toJson()) }
        sharedPreferences.edit()
            .putString(KEY_USERS_LIST, jsonArray.toString())
            .apply()
    }

    fun getActiveUserId(): String {
        return sharedPreferences.getString(KEY_ACTIVE_USER_ID, "default_primary_user") ?: "default_primary_user"
    }

    fun setActiveUserId(id: String) {
        sharedPreferences.edit().putString(KEY_ACTIVE_USER_ID, id).apply()
        // Sync active credentials to base keys for background workers
        val user = getUsers().find { it.id == id }
        if (user != null) {
            sharedPreferences.edit()
                .putString(KEY_BOT_TOKEN, user.botToken)
                .putString(KEY_CHAT_ID, user.chatId)
                .apply()
        }
    }

    fun getActiveUser(): UserAccount {
        val users = getUsers()
        val activeId = getActiveUserId()
        return users.find { it.id == activeId } ?: users.firstOrNull() ?: UserAccount(
            id = "default_primary_user",
            name = "Personal Vault",
            botToken = "",
            chatId = ""
        )
    }

    fun addUser(user: UserAccount) {
        val current = getUsers().toMutableList()
        current.add(user)
        saveUsers(current)
    }

    fun updateUser(updated: UserAccount) {
        val current = getUsers().toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
            saveUsers(current)
            if (updated.id == getActiveUserId()) {
                sharedPreferences.edit()
                    .putString(KEY_BOT_TOKEN, updated.botToken)
                    .putString(KEY_CHAT_ID, updated.chatId)
                    .apply()
            }
        }
    }

    fun deleteUser(id: String) {
        val current = getUsers().toMutableList()
        if (current.size <= 1) return // Don't delete last user
        current.removeAll { it.id == id }
        saveUsers(current)
        if (id == getActiveUserId()) {
            setActiveUserId(current.first().id)
        }
    }

    fun updateUserVerification(userId: String, isVerified: Boolean, statusMessage: String) {
        val current = getUsers().toMutableList()
        val index = current.indexOfFirst { it.id == userId }
        if (index != -1) {
            val old = current[index]
            current[index] = old.copy(
                isVerified = isVerified,
                lastChecked = System.currentTimeMillis(),
                checkStatusMessage = statusMessage
            )
            saveUsers(current)
        }
    }

    fun saveConfig(botToken: String, chatId: String) {
        val cleanToken = sanitizeBotToken(botToken)
        val cleanChatId = sanitizeChatId(chatId)
        sharedPreferences.edit()
            .putString(KEY_BOT_TOKEN, cleanToken)
            .putString(KEY_CHAT_ID, cleanChatId)
            .apply()

        // Also update active user
        val active = getActiveUser()
        updateUser(active.copy(botToken = cleanToken, chatId = cleanChatId))
    }

    fun getBotToken(): String {
        val active = getActiveUser()
        if (active.botToken.isNotEmpty()) return sanitizeBotToken(active.botToken)
        val token = (sharedPreferences.getString(KEY_BOT_TOKEN, "") ?: "").trim()
        return sanitizeBotToken(token)
    }

    fun getChatId(): String {
        val active = getActiveUser()
        if (active.chatId.isNotEmpty()) return sanitizeChatId(active.chatId)
        val id = (sharedPreferences.getString(KEY_CHAT_ID, "") ?: "").trim()
        return sanitizeChatId(id)
    }

    fun clearConfig() {
        sharedPreferences.edit().clear().apply()
    }

    fun isAutoSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_SYNC, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun isWifiOnlySyncEnabled(): Boolean {
        return sharedPreferences.getBoolean("KEY_WIFI_ONLY_SYNC", false)
    }

    fun setWifiOnlySyncEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("KEY_WIFI_ONLY_SYNC", enabled).apply()
    }

    fun getAutoBackupFolders(): Set<String> {
        return sharedPreferences.getStringSet(KEY_AUTO_BACKUP_FOLDERS, emptySet()) ?: emptySet()
    }

    fun setAutoBackupFolders(folders: Set<String>) {
        sharedPreferences.edit().putStringSet(KEY_AUTO_BACKUP_FOLDERS, folders).apply()
    }

    fun getBackupMediaType(): String {
        return sharedPreferences.getString("KEY_BACKUP_MEDIA_TYPE", "ALL") ?: "ALL"
    }

    fun setBackupMediaType(mode: String) {
        sharedPreferences.edit().putString("KEY_BACKUP_MEDIA_TYPE", mode).apply()
    }

    fun getRejectedFilenames(): Set<String> {
        return sharedPreferences.getStringSet("KEY_REJECTED_FILENAMES", emptySet()) ?: emptySet()
    }

    fun addRejectedFilename(filename: String) {
        if (filename.isBlank()) return
        val current = getRejectedFilenames().toMutableSet()
        current.add(filename)
        sharedPreferences.edit().putStringSet("KEY_REJECTED_FILENAMES", current).apply()
    }

    fun removeRejectedFilename(filename: String) {
        if (filename.isBlank()) return
        val current = getRejectedFilenames().toMutableSet()
        if (current.remove(filename)) {
            sharedPreferences.edit().putStringSet("KEY_REJECTED_FILENAMES", current).apply()
        }
    }

    fun isConfigured(): Boolean {
        if (getBotToken().isNotEmpty() && getChatId().isNotEmpty()) return true
        val targets = getAvailableBotUploadTargets()
        return targets.any { it.isEnabled && it.token.isNotBlank() && it.chatId.isNotBlank() }
    }

    fun getThemeIndex(): Int {
        return sharedPreferences.getInt("ui_theme_index", 0)
    }

    fun setThemeIndex(index: Int) {
        sharedPreferences.edit().putInt("ui_theme_index", index).apply()
    }

    fun getDarkModeMode(): Int {
        // 0 = System, 1 = Light, 2 = Dark (Default 2 for Ultra Dark AMOLED)
        return sharedPreferences.getInt("ui_dark_mode_mode", 2)
    }

    fun setDarkModeMode(mode: Int) {
        sharedPreferences.edit().putInt("ui_dark_mode_mode", mode).apply()
    }

    // Multi-Bot Pool Support for load balancing and preventing timeouts / rate limits
    private val rotationCounter = java.util.concurrent.atomic.AtomicInteger(0)
    private val uploadRotationCounter = java.util.concurrent.atomic.AtomicInteger(0)

    fun getBotPool(): List<BotProfile> {
        val key = KEY_BOT_POOL_PREFIX + getActiveUserId()
        val jsonStr = sharedPreferences.getString(key, null)
        val list = mutableListOf<BotProfile>()

        if (!jsonStr.isNullOrEmpty()) {
            try {
                val jsonArray = org.json.JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(BotProfile.fromJson(obj))
                }
            } catch (e: Exception) {
                android.util.Log.e("TelegramConfigManager", "Failed to parse bot pool JSON", e)
            }
        }

        // If list is empty, initialize with primary credentials
        if (list.isEmpty()) {
            val primaryToken = sanitizeBotToken(getBotToken())
            val initialBot = BotProfile(
                id = "primary_bot_${getActiveUserId()}",
                name = "Primary Bot",
                token = primaryToken,
                isEnabled = true,
                isVerified = false,
                statusMessage = if (primaryToken.isNotEmpty()) "Tap 'Verify All Bots' to test connectivity" else "No bot token configured yet"
            )
            list.add(initialBot)
        }

        // Automatically sync all configured User Account bots into the pool so all connected bots share uploads
        try {
            val users = getUsers()
            var modified = false
            for (user in users) {
                val cleanToken = sanitizeBotToken(user.botToken)
                if (cleanToken.isNotEmpty()) {
                    val existingIndex = list.indexOfFirst { sanitizeBotToken(it.token) == cleanToken }
                    if (existingIndex == -1) {
                        list.add(
                            BotProfile(
                                id = "user_bot_${user.id}",
                                name = "${user.name} Bot",
                                token = cleanToken,
                                isEnabled = true,
                                isVerified = user.isVerified,
                                statusMessage = if (user.isVerified) "Verified (${user.name})" else "Tap to verify"
                            )
                        )
                        modified = true
                    } else if (user.isVerified && !list[existingIndex].isVerified) {
                        list[existingIndex] = list[existingIndex].copy(
                            isVerified = true,
                            statusMessage = "Verified (${user.name})"
                        )
                        modified = true
                    }
                }
            }
            if (modified) {
                saveBotPool(list)
            }
        } catch (e: Exception) {
            android.util.Log.w("TelegramConfigManager", "Error syncing user profiles into bot pool", e)
        }

        return list
    }

    fun saveBotPool(bots: List<BotProfile>) {
        val key = KEY_BOT_POOL_PREFIX + getActiveUserId()
        val jsonArray = org.json.JSONArray()
        bots.forEach { jsonArray.put(it.toJson()) }
        sharedPreferences.edit()
            .putString(key, jsonArray.toString())
            .apply()

        // Sync first active bot token with primary KEY_BOT_TOKEN for backward compatibility
        val firstActive = bots.firstOrNull { it.isEnabled && it.token.isNotBlank() } ?: bots.firstOrNull { it.token.isNotBlank() }
        if (firstActive != null && firstActive.token.isNotBlank()) {
            sharedPreferences.edit()
                .putString(KEY_BOT_TOKEN, firstActive.token)
                .apply()
        }
    }

    fun addBotToPool(name: String, token: String): BotProfile {
        val cleanToken = sanitizeBotToken(token)
        val cleanName = name.trim().ifEmpty { "Bot ${getBotPool().size + 1}" }
        val newBot = BotProfile(
            name = cleanName,
            token = cleanToken,
            isEnabled = true,
            isVerified = false,
            statusMessage = "Added. Ready for verification."
        )
        val current = getBotPool().toMutableList()
        current.add(newBot)
        saveBotPool(current)
        return newBot
    }

    fun removeBotFromPool(botId: String) {
        val current = getBotPool().toMutableList()
        if (current.size <= 1) return // Keep at least one bot
        current.removeAll { it.id == botId }
        saveBotPool(current)
    }

    fun updateBotInPool(updated: BotProfile) {
        val current = getBotPool().toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
            saveBotPool(current)
        }
    }

    fun updateBotVerification(botId: String, isVerified: Boolean, statusMessage: String, botUsername: String = "") {
        val current = getBotPool().toMutableList()
        val index = current.indexOfFirst { it.id == botId }
        if (index != -1) {
            val old = current[index]
            val updated = old.copy(
                isVerified = isVerified,
                lastChecked = System.currentTimeMillis(),
                statusMessage = statusMessage,
                botUsername = if (botUsername.isNotEmpty()) botUsername else old.botUsername
            )
            current[index] = updated
            saveBotPool(current)

            // Also sync back to any matching User Account
            val cleanToken = sanitizeBotToken(updated.token)
            val users = getUsers().toMutableList()
            var userModified = false
            for (i in users.indices) {
                if (sanitizeBotToken(users[i].botToken) == cleanToken) {
                    users[i] = users[i].copy(
                        isVerified = isVerified,
                        lastChecked = System.currentTimeMillis(),
                        checkStatusMessage = statusMessage
                    )
                    userModified = true
                }
            }
            if (userModified) {
                saveUsers(users)
            }
        }
    }

    fun updateBotVerificationByToken(token: String, isVerified: Boolean, statusMessage: String, botUsername: String = "") {
        val cleanToken = sanitizeBotToken(token)
        if (cleanToken.isEmpty()) return
        val current = getBotPool().toMutableList()
        var poolModified = false
        for (i in current.indices) {
            if (sanitizeBotToken(current[i].token) == cleanToken) {
                val old = current[i]
                current[i] = old.copy(
                    isVerified = isVerified,
                    lastChecked = System.currentTimeMillis(),
                    statusMessage = statusMessage,
                    botUsername = if (botUsername.isNotEmpty()) botUsername else old.botUsername
                )
                poolModified = true
            }
        }
        if (poolModified) {
            saveBotPool(current)
        }

        // Also sync to matching User Account profiles so both bots show verified in User Profiles & Credentials
        try {
            val users = getUsers().toMutableList()
            var userModified = false
            for (i in users.indices) {
                if (sanitizeBotToken(users[i].botToken) == cleanToken) {
                    users[i] = users[i].copy(
                        isVerified = isVerified,
                        lastChecked = System.currentTimeMillis(),
                        checkStatusMessage = statusMessage
                    )
                    userModified = true
                }
            }
            if (userModified) {
                saveUsers(users)
            }
        } catch (e: Exception) {
            android.util.Log.e("TelegramConfigManager", "Failed to sync user verification by token", e)
        }
    }

    /**
     * Aggregates all available and valid bot upload targets across:
     * 1. User Profiles configured under USER PROFILES & CREDENTIALS
     * 2. Active bots in the Multi-Bot Load Balancing Pool
     * 3. Primary Telegram credentials
     *
     * Guarantees that if a user has configured two bots, BOTH are returned and active for file uploading.
     */
    fun getAvailableBotUploadTargets(): List<BotUploadTarget> {
        val targets = mutableListOf<BotUploadTarget>()
        val defaultChatId = getChatId()
        val seenTokens = mutableSetOf<String>()

        // 1. Collect all bots from User Profiles
        try {
            val users = getUsers()
            for (user in users) {
                val cleanToken = sanitizeBotToken(user.botToken)
                if (cleanToken.isNotEmpty() && !seenTokens.contains(cleanToken)) {
                    seenTokens.add(cleanToken)
                    val targetChatId = sanitizeChatId(user.chatId).ifEmpty { defaultChatId }
                    targets.add(
                        BotUploadTarget(
                            id = "user_${user.id}",
                            name = "${user.name} Bot",
                            token = cleanToken,
                            chatId = targetChatId,
                            isVerified = user.isVerified,
                            isEnabled = true,
                            source = "User Profile: ${user.name}"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TelegramConfigManager", "Error collecting bots from users", e)
        }

        // 2. Collect from Bot Pool
        try {
            val pool = getBotPool()
            for (bot in pool) {
                val cleanToken = sanitizeBotToken(bot.token)
                if (cleanToken.isNotEmpty() && bot.isEnabled && !seenTokens.contains(cleanToken)) {
                    seenTokens.add(cleanToken)
                    targets.add(
                        BotUploadTarget(
                            id = bot.id,
                            name = bot.name,
                            token = cleanToken,
                            chatId = defaultChatId,
                            isVerified = bot.isVerified,
                            isEnabled = bot.isEnabled,
                            source = "Bot Pool: ${bot.name}"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TelegramConfigManager", "Error collecting bots from pool", e)
        }

        // 3. Fallback to primary token
        val primaryToken = sanitizeBotToken(getBotToken())
        if (primaryToken.isNotEmpty() && !seenTokens.contains(primaryToken)) {
            seenTokens.add(primaryToken)
            targets.add(
                BotUploadTarget(
                    id = "primary_credentials",
                    name = "Primary Bot",
                    token = primaryToken,
                    chatId = defaultChatId,
                    isVerified = true,
                    isEnabled = true,
                    source = "Telegram Credentials"
                )
            )
        }

        // Ensure targets have fallback chatId
        return targets.map { target ->
            if (target.chatId.isEmpty() && defaultChatId.isNotEmpty()) {
                target.copy(chatId = defaultChatId)
            } else {
                target
            }
        }
    }

    fun getAvailableBotTokens(): List<String> {
        val targets = getAvailableBotUploadTargets()
        val tokens = targets.filter { it.isEnabled && it.token.isNotBlank() }.map { it.token }.distinct()
        if (tokens.isNotEmpty()) return tokens
        val fallback = sanitizeBotToken(getBotToken())
        return if (fallback.isNotEmpty()) listOf(fallback) else emptyList()
    }

    fun getNextRotatedBotToken(): String {
        val tokens = getAvailableBotTokens()
        if (tokens.isEmpty()) return getBotToken()
        val index = Math.abs(rotationCounter.getAndIncrement() % tokens.size)
        return tokens[index]
    }

    /**
     * Returns an atomic round-robin index for distributing consecutive file uploads
     * evenly across all connected bots in the rotation pool.
     */
    fun getNextRotatedTargetIndex(poolSize: Int): Int {
        if (poolSize <= 1) return 0
        return Math.abs(uploadRotationCounter.getAndIncrement() % poolSize)
    }

    companion object {
        private const val KEY_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_CHAT_ID = "telegram_chat_id"
        private const val KEY_AUTO_SYNC = "telegram_auto_sync"
        private const val KEY_AUTO_BACKUP_FOLDERS = "telegram_auto_backup_folders"
        private const val KEY_USERS_LIST = "televault_users_list"
        private const val KEY_ACTIVE_USER_ID = "televault_active_user_id"
        private const val KEY_BOT_POOL_PREFIX = "televault_bot_pool_"

        fun sanitizeBotToken(input: String): String {
            val trimmed = input.trim()
            // Match telegram token pattern (digits followed by colon and alphanumeric characters/hyphen/underscore)
            val tokenRegex = Regex("""(\d+:[a-zA-Z0-9_-]+)""")
            val match = tokenRegex.find(trimmed)
            return match?.value ?: trimmed
        }

        fun sanitizeChatId(input: String): String {
            val trimmed = input.trim()
            // Try matching channels starting with -100
            val channelIdRegex = Regex("""(-100\d+)""")
            val channelMatch = channelIdRegex.find(trimmed)
            if (channelMatch != null) return channelMatch.value

            // Try matching general negative group/supergroup IDs
            val groupIdRegex = Regex("""(-\d+)""")
            val groupMatch = groupIdRegex.find(trimmed)
            if (groupMatch != null) return groupMatch.value

            // Try matching channel usernames starting with @
            val usernameRegex = Regex("""(@[a-zA-Z0-9_]+)""")
            val usernameMatch = usernameRegex.find(trimmed)
            if (usernameMatch != null) return usernameMatch.value

            // Try matching pure digit IDs
            val numberRegex = Regex("""(\d+)""")
            val numberMatch = numberRegex.find(trimmed)
            if (numberMatch != null) return numberMatch.value

            return trimmed
        }
    }
}

data class UserAccount(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val botToken: String,
    val chatId: String,
    val avatarColorHex: String = "#C85A32",
    val role: String = "Personal",
    val isVerified: Boolean = false,
    val lastChecked: Long = 0L,
    val checkStatusMessage: String = "Not checked yet"
) {
    fun toJson(): org.json.JSONObject {
        val json = org.json.JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("botToken", botToken)
        json.put("chatId", chatId)
        json.put("avatarColorHex", avatarColorHex)
        json.put("role", role)
        json.put("isVerified", isVerified)
        json.put("lastChecked", lastChecked)
        json.put("checkStatusMessage", checkStatusMessage)
        return json
    }

    companion object {
        fun fromJson(json: org.json.JSONObject): UserAccount {
            return UserAccount(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.optString("name", "Personal Vault"),
                botToken = json.optString("botToken", ""),
                chatId = json.optString("chatId", ""),
                avatarColorHex = json.optString("avatarColorHex", "#C85A32"),
                role = json.optString("role", "Personal"),
                isVerified = json.optBoolean("isVerified", false),
                lastChecked = json.optLong("lastChecked", 0L),
                checkStatusMessage = json.optString("checkStatusMessage", "Not checked yet")
            )
        }
    }
}

data class BotProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val token: String,
    val isEnabled: Boolean = true,
    val isVerified: Boolean = false,
    val lastChecked: Long = 0L,
    val statusMessage: String = "Not checked yet",
    val botUsername: String = ""
) {
    fun toJson(): org.json.JSONObject {
        val json = org.json.JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("token", token)
        json.put("isEnabled", isEnabled)
        json.put("isVerified", isVerified)
        json.put("lastChecked", lastChecked)
        json.put("statusMessage", statusMessage)
        json.put("botUsername", botUsername)
        return json
    }

    companion object {
        fun fromJson(json: org.json.JSONObject): BotProfile {
            return BotProfile(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.optString("name", "Bot"),
                token = json.optString("token", ""),
                isEnabled = json.optBoolean("isEnabled", true),
                isVerified = json.optBoolean("isVerified", false),
                lastChecked = json.optLong("lastChecked", 0L),
                statusMessage = json.optString("statusMessage", "Not checked yet"),
                botUsername = json.optString("botUsername", "")
            )
        }
    }
}

data class BotUploadTarget(
    val id: String,
    val name: String,
    val token: String,
    val chatId: String,
    val isVerified: Boolean = false,
    val isEnabled: Boolean = true,
    val source: String = "Profile"
)


