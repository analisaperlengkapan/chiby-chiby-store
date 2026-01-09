package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.StokGudangDao
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.StokGudang
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StokGudangRepository @Inject constructor(
    private val stokGudangDao: StokGudangDao
) {
    suspend fun getStock(productId: Long, warehouseId: Long): Result<StokGudang?> {
        return try {
            val stock = stokGudangDao.getStock(productId, warehouseId)
            Result.success(stock)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertOrUpdateStock(stokGudang: StokGudang): Result<Unit> {
        return try {
            // Check if exists
            val existing = stokGudangDao.getStock(stokGudang.productId, stokGudang.warehouseId)
            if (existing != null) {
                stokGudangDao.updateStockQuantity(stokGudang.productId, stokGudang.warehouseId, stokGudang.quantity)
            } else {
                stokGudangDao.insertOrUpdateStock(stokGudang)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStock(productId: Long, warehouseId: Long): Result<Unit> {
        return try {
            stokGudangDao.deleteStock(productId, warehouseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getStocksByProduct(productId: Long): Flow<List<StokGudang>> {
        return stokGudangDao.getStocksByProduct(productId)
    }

    fun getProductsByWarehouse(warehouseId: Long): Flow<List<Produk>> {
        return stokGudangDao.getProductsByWarehouse(warehouseId)
    }

    suspend fun getTotalStock(productId: Long): Int {
        return stokGudangDao.getTotalStock(productId) ?: 0
    }
}
