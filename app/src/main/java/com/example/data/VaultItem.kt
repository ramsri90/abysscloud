package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncState {
    PENDING,
    UPLOADING,
    SYNCED,
    FAILED
}

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val localPath: String,
    val filename: String,
    val fileSize: Long,
    val mimeType: String,
    val telegramFileId: String? = null,
    val syncState: SyncState = SyncState.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val folder: String = "General",
    val tags: String = "", // AI generated tags
    val userId: String = "default_primary_user",
    val telegramMessageId: Long? = null,
    val telegramChatId: String? = null
)

data class UploadProgressInfo(
    val progress: Int = 0,
    val bytesRead: Long = 0L,
    val bytesTotal: Long = 0L
)


