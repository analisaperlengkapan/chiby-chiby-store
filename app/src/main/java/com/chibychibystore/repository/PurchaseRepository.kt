package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PurchaseDao
import com.chibychibystore.data.local.entity.Purchase
import com.chibychibystore.data.local.entity.PurchaseWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRepository @Inject constructor(
    private val purchaseDao: PurchaseDao
) {
    fun getAllPurchases(): Flow<List<Purchase>> = purchaseDao.getAllPurchases()

    suspend fun getPurchaseById(id: Long): Result<Purchase> {
        return try {
            val purchase = purchaseDao.getPurchaseById(id)
            if (purchase != null) {
                Result.success(purchase)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Purchase not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPurchaseById", e))
        }
    }

    suspend fun getPurchaseWithItems(id: Long): Result<PurchaseWithItems> {
        return try {
            val purchaseWithItems = purchaseDao.getPurchaseWithItems(id)
            if (purchaseWithItems != null) {
                Result.success(purchaseWithItems)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Purchase with items not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPurchaseWithItems", e))
        }
    }

    fun getPurchasesBySupplier(supplierId: Long): Flow<List<Purchase>> = purchaseDao.getPurchasesBySupplier(supplierId)

    fun getPurchasesByDateRange(startDate: Date, endDate: Date): Flow<List<Purchase>> = purchaseDao.getPurchasesByDateRange(startDate, endDate)

    suspend fun createPurchase(purchase: Purchase): Result<Long> {
        return try {
            val id = purchaseDao.insertPurchase(purchase)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPurchase", e))
        }
    }

    suspend fun updatePurchase(purchase: Purchase): Result<Unit> {
        return try {
            purchaseDao.updatePurchase(purchase)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updatePurchase", e))
        }
    }

    suspend fun deletePurchase(id: Long): Result<Unit> {
        return try {
            purchaseDao.deletePurchaseById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deletePurchase", e))
        }
    }

    suspend fun getPurchaseCount(): Result<Int> {
        return try {
            val count = purchaseDao.getPurchaseCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPurchaseCount", e))
        }
    }

    suspend fun getTotalPurchaseAmount(startDate: Date, endDate: Date): Result<Double> {
        return try {
            val total = purchaseDao.getTotalPurchaseAmount(startDate, endDate) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalPurchaseAmount", e))
        }
    }
}
