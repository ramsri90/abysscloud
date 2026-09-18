package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    suspend fun getAllItemsStatic(): List<VaultItem>

    @Query("SELECT * FROM vault_items WHERE syncState = 'PENDING' OR syncState = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getUnsyncedItems(): List<VaultItem>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getItemById(id: String): VaultItem?

    @Query("SELECT * FROM vault_items WHERE localPath = :path OR filename = :filename LIMIT 1")
    suspend fun getItemByPathOrFilename(path: String, filename: String): VaultItem?

    @Query("DELETE FROM vault_items WHERE id NOT IN (SELECT MIN(id) FROM vault_items GROUP BY filename, fileSize)")
    suspend fun deleteDuplicates()

    @Query("DELETE FROM vault_items WHERE telegramFileId IS NOT NULL AND telegramFileId != '' AND id NOT IN (SELECT MIN(id) FROM vault_items WHERE telegramFileId IS NOT NULL AND telegramFileId != '' GROUP BY telegramFileId)")
    suspend fun deleteTelegramFileIdDuplicates()

    @Query("SELECT COUNT(*) FROM vault_items WHERE syncState = 'SYNCED'")
    fun getSyncedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_items")
    fun getTotalCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItem)

    @Update
    suspend fun updateItem(item: VaultItem)

    @Delete
    suspend fun deleteItem(item: VaultItem)

    @Delete
    suspend fun deleteItems(items: List<VaultItem>)

    @Update
    suspend fun updateItems(items: List<VaultItem>)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM vault_items WHERE syncState = 'PENDING' OR syncState = 'UPLOADING'")
    suspend fun deleteQueueItems()

    @Query("UPDATE vault_items SET syncState = 'PENDING' WHERE syncState = 'FAILED'")
    suspend fun restartFailedItems()

    @Query("UPDATE vault_items SET syncState = 'FAILED' WHERE syncState = 'PENDING' OR syncState = 'UPLOADING'")
    suspend fun markQueueAsFailed()
}
