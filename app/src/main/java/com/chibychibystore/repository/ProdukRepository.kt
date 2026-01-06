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
     * Get products by list of IDs
     */
    suspend fun getProductsByIds(ids: List<Long>): Result<List<Produk>> {
        return try {
            val products = produkDao.getProdukByIds(ids)
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProductsByIds", e))
        }
    }

    /**
     * Compatibility wrapper
     */
    suspend fun getProduk(id: Long): Produk? {
        return try {
            val res = getProdukById(id)
            if (res is Result.Success) res.data else null
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
    fun getProdukByKategori(categoryId: Long): Flow<List<Produk>> = produkDao.getProdukByKategori(categoryId)

    /**
     * Get produk by warehouse
     */
    fun getProdukByGudang(warehouseId: Long): Flow<List<Produk>> = produkDao.getProdukByGudang(warehouseId)

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
            if (!produk.barcode.isNullOrBlank()) {
                val existingProduk = produkDao.getProdukByBarcode(produk.barcode)
                if (existingProduk != null) {
                    return Result.failure(ChibyChibyException.ValidationError("barcode", "Barcode sudah digunakan"))
                }
            }

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
            val existingProduk = produkDao.getProdukById(produk.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Produk tidak ditemukan"))

            if (!produk.barcode.isNullOrBlank()) {
                val produkWithSameBarcode = produkDao.getProdukByBarcode(produk.barcode)
                if (produkWithSameBarcode != null && produkWithSameBarcode.id != produk.id) {
                    return Result.failure(ChibyChibyException.ValidationError("barcode", "Barcode sudah digunakan"))
                }
            }

            produkDao.updateProduk(produk)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateProduk", e))
        }
    }

    /**
     * Adjust stock quantity
     */
    suspend fun adjustStock(id: Long, delta: Int): Result<Unit> {
        return try {
            val produk = produkDao.getProdukById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Produk tidak ditemukan"))
            
            produkDao.adjustStock(id, delta)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("adjustStock", e))
        }
    }

    /**
     * Compatibility wrapper: Set stock quantity
     */
    suspend fun updateStock(id: Long, quantity: Int): Result<Unit> {
        return try {
            produkDao.setStock(id, quantity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateStock", e))
        }
    }

    /**
     * Delete produk
     */
    suspend fun deleteProduk(id: Long): Result<Unit> {
        return try {
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
    suspend fun getTotalInventoryValue(): Result<Double> {
        return try {
            val total = produkDao.getTotalInventoryValue() ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalInventoryValue", e))
        }
    }

    suspend fun countLowStock(): Result<Int> {
        return try {
            val count = produkDao.countLowStock()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("countLowStock", e))
        }
    }

    suspend fun countOutOfStock(): Result<Int> {
        return try {
            val count = produkDao.countOutOfStock()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("countOutOfStock", e))
        }
    }
}