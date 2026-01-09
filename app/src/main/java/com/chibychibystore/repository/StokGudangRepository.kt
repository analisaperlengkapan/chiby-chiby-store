package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ProdukDao
import com.chibychibystore.data.local.dao.StokGudangDao
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.StokGudang
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StokGudangRepository @Inject constructor(
    private val stokGudangDao: StokGudangDao,
    private val produkDao: ProdukDao
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

            // Sync total stock in Produk table
            updateProductTotalStock(stokGudang.productId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStock(productId: Long, warehouseId: Long): Result<Unit> {
        return try {
            stokGudangDao.deleteStock(productId, warehouseId)

            // Sync total stock in Produk table
            updateProductTotalStock(productId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateProductTotalStock(productId: Long) {
        try {
            val totalStock = stokGudangDao.getTotalStock(productId) ?: 0
            produkDao.setStock(productId, totalStock)
        } catch (e: Exception) {
            // Log error or handle it, but for now we try best effort to sync
            e.printStackTrace()
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
