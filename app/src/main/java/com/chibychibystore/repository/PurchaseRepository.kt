package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PurchaseDao
import com.chibychibystore.data.local.dao.PurchaseItemDao
import com.chibychibystore.data.local.entity.Purchase
import com.chibychibystore.data.local.entity.PurchaseItem
import com.chibychibystore.data.local.entity.PurchaseWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRepository @Inject constructor(
    private val purchaseDao: PurchaseDao,
    private val purchaseItemDao: PurchaseItemDao
) {
    fun getAllPurchases(): Flow<List<Purchase>> = purchaseDao.getAllPurchases()

    suspend fun getPurchaseById(id: Long): Result<Purchase> {
        return try {
            val purchase = purchaseDao.getPurchaseById(id)
            if (purchase != null) {
                Result.success(purchase)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Purchase with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPurchaseById", e))
        }
    }

    fun getPurchasesBySupplier(supplierId: Long): Flow<List<Purchase>> =
        purchaseDao.getPurchasesBySupplier(supplierId)

    fun getPurchasesByDateRange(startDate: Date, endDate: Date): Flow<List<Purchase>> =
        purchaseDao.getPurchasesByDateRange(startDate, endDate)

    fun getPurchaseWithItems(id: Long): Flow<PurchaseWithItems> =
        purchaseDao.getPurchaseWithItems(id)

    suspend fun getAllPurchaseItems(): List<PurchaseItem> = purchaseItemDao.getAllPurchaseItems()

    suspend fun createPurchase(purchase: Purchase, items: List<PurchaseItem>): Result<Long> {
        return try {
            val purchaseId = purchaseDao.insertPurchase(purchase)
            val itemsWithId = items.map { it.copy(purchaseId = purchaseId) }
            purchaseItemDao.insertPurchaseItems(itemsWithId)
            Result.success(purchaseId)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPurchase", e))
        }
    }

    suspend fun createPurchaseItems(items: List<PurchaseItem>): Result<List<Long>> {
        return try {
             val ids = purchaseItemDao.insertPurchaseItems(items)
             Result.success(ids)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPurchaseItems", e))
        }
    }

    suspend fun deletePurchase(id: Long): Result<Unit> {
        return try {
            purchaseDao.getPurchaseById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Purchase not found"))

            // Items should be deleted by CASCADE
            purchaseDao.deletePurchaseById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deletePurchase", e))
        }
    }
}
