package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ItemPenjualanDao
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data ItemPenjualan
 */
@Singleton
class ItemPenjualanRepository @Inject constructor(
    private val itemPenjualanDao: ItemPenjualanDao
) {

    /**
     * Get semua item penjualan
     */
    fun getAllItemPenjualan(): Flow<List<ItemPenjualan>> = itemPenjualanDao.getAllItemPenjualan()

    /**
     * Get item penjualan by ID
     */
    suspend fun getItemPenjualanById(id: Long): Result<ItemPenjualan> {
        return try {
            val item = itemPenjualanDao.getItemPenjualanById(id)
            if (item != null) {
                Result.success(item)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Item penjualan dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getItemPenjualanById", e))
        }
    }

    /**
     * Get items by penjualan ID
     */
    fun getItemsByPenjualanId(penjualanId: Long): Flow<List<ItemPenjualan>> =
        itemPenjualanDao.getItemsByPenjualanId(penjualanId)

    /**
     * Create item penjualan baru
     */
    suspend fun createItemPenjualan(item: ItemPenjualan): Result<ItemPenjualan> {
        return try {
            // Validasi data
            if (item.quantity <= 0) {
                return Result.failure(ChibyChibyException.ValidationError("Quantity harus lebih dari 0"))
            }
            if (item.unitPrice < 0) {
                return Result.failure(ChibyChibyException.ValidationError("Harga unit tidak boleh negatif"))
            }
            if (item.totalPrice < 0) {
                return Result.failure(ChibyChibyException.ValidationError("Total harga tidak boleh negatif"))
            }

            val itemId = itemPenjualanDao.insertItemPenjualan(item)
            val createdItem = item.copy(id = itemId)
            Result.success(createdItem)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createItemPenjualan", e))
        }
    }

    /**
     * Update item penjualan
     */
    suspend fun updateItemPenjualan(id: Long, item: ItemPenjualan): Result<ItemPenjualan> {
        return try {
            val existingItem = itemPenjualanDao.getItemPenjualanById(id)
            if (existingItem == null) {
                return Result.failure(ChibyChibyException.DatabaseError("Item penjualan dengan ID $id tidak ditemukan"))
            }

            val updatedItem = item.copy(id = id)
            itemPenjualanDao.updateItemPenjualan(updatedItem)
            Result.success(updatedItem)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateItemPenjualan", e))
        }
    }

    /**
     * Delete item penjualan
     */
    suspend fun deleteItemPenjualan(id: Long): Result<Unit> {
        return try {
            val existingItem = itemPenjualanDao.getItemPenjualanById(id)
            if (existingItem == null) {
                return Result.failure(ChibyChibyException.DatabaseError("Item penjualan dengan ID $id tidak ditemukan"))
            }

            itemPenjualanDao.deleteItemPenjualanById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteItemPenjualan", e))
        }
    }

    /**
     * Delete items by penjualan ID
     */
    suspend fun deleteItemsByPenjualanId(penjualanId: Long): Result<Unit> {
        return try {
            itemPenjualanDao.deleteItemsByPenjualanId(penjualanId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteItemsByPenjualanId", e))
        }
    }

    /**
     * Insert batch items
     */
    suspend fun insertItemPenjualanBatch(items: List<ItemPenjualan>): Result<Unit> {
        return try {
            itemPenjualanDao.insertItemPenjualanBatch(items)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("insertItemPenjualanBatch", e))
        }
    }
}