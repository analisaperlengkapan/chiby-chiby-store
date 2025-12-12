package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ItemPembelianDao
import com.chibychibystore.data.local.entity.ItemPembelian
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data ItemPembelian
 */
@Singleton
class ItemPembelianRepository @Inject constructor(
    private val itemPembelianDao: ItemPembelianDao
) {

    /**
     * Get semua item pembelian
     */
    fun getAllItemPembelian(): Flow<List<ItemPembelian>> = itemPembelianDao.getAllItemPembelian()

    /**
     * Get item pembelian by ID
     */
    suspend fun getItemPembelianById(id: Long): Result<ItemPembelian> {
        return try {
            val item = itemPembelianDao.getItemPembelianById(id)
            if (item != null) {
                Result.success(item)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Item pembelian dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getItemPembelianById", e))
        }
    }

    /**
     * Get items by pembelian ID
     */
    fun getItemsByPembelianId(pembelianId: Long): Flow<List<ItemPembelian>> =
        itemPembelianDao.getItemsByPembelianId(pembelianId)

    /**
     * Create item pembelian baru
     */
    suspend fun createItemPembelian(item: ItemPembelian): Result<ItemPembelian> {
        return try {
            // Validasi data
            validateItemPembelian(item)

            val id = itemPembelianDao.insertItemPembelian(item)
            val createdItem = item.copy(id = id)
            Result.success(createdItem)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createItemPembelian", e))
        }
    }

    /**
     * Update item pembelian
     */
    suspend fun updateItemPembelian(item: ItemPembelian): Result<ItemPembelian> {
        return try {
            // Validasi data
            validateItemPembelian(item)

            itemPembelianDao.updateItemPembelian(item)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateItemPembelian", e))
        }
    }

    /**
     * Delete item pembelian
     */
    suspend fun deleteItemPembelian(id: Long): Result<Unit> {
        return try {
            itemPembelianDao.deleteItemPembelian(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteItemPembelian", e))
        }
    }

    /**
     * Get items by produk ID
     */
    fun getItemsByProdukId(produkId: Long): Flow<List<ItemPembelian>> =
        itemPembelianDao.getItemsByProdukId(produkId)

    /**
     * Validasi item pembelian
     */
    private fun validateItemPembelian(item: ItemPembelian) {
        require(item.pembelianId > 0) { "ID pembelian harus valid" }
        require(item.produkId > 0) { "ID produk harus valid" }
        require(item.jumlah > 0) { "Jumlah harus lebih dari 0" }
        require(item.hargaBeli >= 0) { "Harga beli tidak boleh negatif" }
    }
}