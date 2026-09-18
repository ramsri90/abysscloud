package com.example.data

import kotlinx.coroutines.flow.Flow

class VaultRepository(private val vaultDao: VaultDao) {
    val allItems: Flow<List<VaultItem>> = vaultDao.getAllItems()
    val syncedCount: Flow<Int> = vaultDao.getSyncedCountFlow()
    val totalCount: Flow<Int> = vaultDao.getTotalCountFlow()

    suspend fun insert(item: VaultItem) {
        vaultDao.insertItem(item)
    }

    suspend fun update(item: VaultItem) {
        vaultDao.updateItem(item)
    }

    suspend fun updateItems(items: List<VaultItem>) {
        vaultDao.updateItems(items)
    }

    suspend fun delete(item: VaultItem) {
        vaultDao.deleteItem(item)
    }

    suspend fun deleteItems(items: List<VaultItem>) {
        vaultDao.deleteItems(items)
    }

    suspend fun deleteById(id: String) {
        vaultDao.deleteItemById(id)
    }

    suspend fun getUnsyncedItems(): List<VaultItem> {
        return vaultDao.getUnsyncedItems()
    }

    suspend fun getAllItemsStatic(): List<VaultItem> {
        return vaultDao.getAllItemsStatic()
    }

    suspend fun getItemById(id: String): VaultItem? {
        return vaultDao.getItemById(id)
    }

    suspend fun getItemByPathOrFilename(path: String, filename: String): VaultItem? {
        return vaultDao.getItemByPathOrFilename(path, filename)
    }

    suspend fun deleteDuplicates() {
        vaultDao.deleteDuplicates()
        vaultDao.deleteTelegramFileIdDuplicates()
    }

    suspend fun deleteQueueItems() {
        vaultDao.deleteQueueItems()
    }

    suspend fun restartFailedItems() {
        vaultDao.restartFailedItems()
    }

    suspend fun markQueueAsFailed() {
        vaultDao.markQueueAsFailed()
    }
}
