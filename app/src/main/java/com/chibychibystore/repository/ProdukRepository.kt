package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ProdukDao
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Produk
 */
@Singleton
class ProdukRepository @Inject constructor(
    private val produkDao: ProdukDao
) {

    /**
     * Get semua produk
     */
    fun getAllProduk(): Flow<List<Produk>> = produkDao.getAllProduk()

    /**
     * Get produk by ID
     */
    suspend fun getProdukById(id: Long): Result<Produk> {
        return try {
            val produk = produkDao.getProdukById(id)
            if (produk != null) {
                Result.success(produk)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Produk dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProdukById", e))
        }
    }

    /**
     * Get produk by multiple IDs
     */
    suspend fun getProdukByIds(ids: List<Long>): Result<List<Produk>> {
        return try {
            val produkList = produkDao.getProdukByIds(ids)
            Result.success(produkList)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProdukByIds", e))
        }
    }

    /**
     * Compatibility wrapper for legacy code / tests that expect a nullable Produk return
     */
    suspend fun getProduk(id: Long): Produk? {
        return try {
            val res = getProdukById(id)
            res.getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get produk by barcode
     */
    suspend fun getProdukByBarcode(barcode: String): Result<Produk> {
        return try {
            val produk = produkDao.getProdukByBarcode(barcode)
            if (produk != null) {
                Result.success(produk)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Produk dengan barcode $barcode tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProdukByBarcode", e))
        }
    }

    /**
     * Get produk by category
     */
    fun getProdukByCategory(categoryId: Long): Flow<List<Produk>> = produkDao.getProdukByCategory(categoryId)

    /**
     * Get produk by warehouse
     */
    fun getProdukByWarehouse(warehouseId: Long): Flow<List<Produk>> = produkDao.getProdukByWarehouse(warehouseId)

    /**
     * Search produk
     */
    fun searchProduk(query: String): Flow<List<Produk>> = produkDao.searchProduk(query)

    /**
     * Get low stock produk
     */
    fun getLowStockProduk(): Flow<List<Produk>> = produkDao.getLowStockProduk()

    /**
     * Get out of stock produk
     */
    fun getOutOfStockProduk(): Flow<List<Produk>> = produkDao.getOutOfStockProduk()

    /**
     * Create produk baru
     */
    suspend fun createProduk(produk: Produk): Result<Long> {
        return try {
            val id = produkDao.insertProduk(produk)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createProduk", e))
        }
    }

    /**
     * Update produk
     */
    suspend fun updateProduk(produk: Produk): Result<Unit> {
        return try {
            produkDao.updateProduk(produk)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateProduk", e))
        }
    }

    /**
     * Adjust stock quantity (Delta)
     * Use this for sales, returns, purchases.
     * @param quantity Positive to add, negative to subtract.
     */
    suspend fun adjustStock(id: Long, quantity: Int): Result<Unit> {
        return try {
            produkDao.adjustStock(id, quantity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("adjustStock", e))
        }
    }

    /**
     * Set absolute stock quantity.
     * Use this for stock taking / inventory correction.
     */
    suspend fun setStock(id: Long, quantity: Int): Result<Unit> {
        return try {
            produkDao.setStock(id, quantity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("setStock", e))
        }
    }

    /**
     * Legacy support - maps to adjustStock (Delta)
     */
    suspend fun updateStock(id: Long, quantity: Int): Result<Unit> {
        return adjustStock(id, quantity)
    }

    /**
     * Delete produk
     */
    suspend fun deleteProduk(id: Long): Result<Unit> {
        return try {
            // Check if produk exists
            val produk = produkDao.getProdukById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Produk tidak ditemukan"))

            produkDao.deleteProdukById(id)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteProduk", e))
        }
    }

    /**
     * Get jumlah total produk
     */
    suspend fun getProdukCount(): Result<Int> {
        return try {
            val count = produkDao.getProdukCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProdukCount", e))
        }
    }

    /**
     * Get total stock across all products
     */
    suspend fun getTotalStock(): Result<Int> {
        return try {
            val total = produkDao.getTotalStock() ?: 0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalStock", e))
        }
    }

}