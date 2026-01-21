package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PurchaseItemDao
import com.chibychibystore.data.local.entity.PurchaseItem
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseItemRepository @Inject constructor(
    private val purchaseItemDao: PurchaseItemDao
) {
    fun getAllPurchaseItems(): Flow<List<PurchaseItem>> {
        // We need to add this query to the DAO and make it return Flow
        return purchaseItemDao.observeAllPurchaseItems()
    }

    fun getItemsByPurchaseId(purchaseId: Long): Flow<List<PurchaseItem>> =
        purchaseItemDao.getItemsByPurchaseId(purchaseId)

    suspend fun insertPurchaseItems(items: List<PurchaseItem>): Result<List<Long>> {
        return try {
            val ids = purchaseItemDao.insertPurchaseItems(items)
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("insertPurchaseItems", e))
        }
    }
}
