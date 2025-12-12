package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ProdukDao
import com.chibychibystore.data.local.entity.Produk
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
            // Validasi input
            validateProdukData(produk)

            // Check if barcode already exists (if provided)
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
            // Validasi input
            validateProdukData(produk)

            // Check if produk exists
            val existingProduk = produkDao.getProdukById(produk.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Produk tidak ditemukan"))

            // Check barcode uniqueness (exclude current produk)
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
     * Update stock quantity
     */
    suspend fun updateStock(id: Long, quantity: Int): Result<Unit> {
        return try {
            // Check if produk exists
            val produk = produkDao.getProdukById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Produk tidak ditemukan"))

            // Validate quantity
            if (produk.stockQuantity + quantity < 0) {
                return Result.failure(ChibyChibyException.ValidationError("quantity", "Stok tidak boleh negatif"))
            }

            produkDao.updateStock(id, quantity)
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

    private fun validateProdukData(produk: Produk) {
        if (produk.name.isBlank()) {
            throw ChibyChibyException.ValidationError("name", "Nama produk tidak boleh kosong")
        }
        if (produk.name.length < 2) {
            throw ChibyChibyException.ValidationError("name", "Nama produk minimal 2 karakter")
        }
        if (produk.costPrice < 0) {
            throw ChibyChibyException.ValidationError("costPrice", "Harga beli tidak boleh negatif")
        }
        if (produk.sellingPrice < 0) {
            throw ChibyChibyException.ValidationError("sellingPrice", "Harga jual tidak boleh negatif")
        }
        if (produk.sellingPrice < produk.costPrice) {
            throw ChibyChibyException.ValidationError("sellingPrice", "Harga jual tidak boleh kurang dari harga beli")
        }
        if (produk.stockQuantity < 0) {
            throw ChibyChibyException.ValidationError("stockQuantity", "Stok tidak boleh negatif")
        }
        if (produk.minStock < 0) {
            throw ChibyChibyException.ValidationError("minStock", "Stok minimum tidak boleh negatif")
        }
    }
}