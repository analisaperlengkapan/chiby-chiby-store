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
     * Get items by sale ID
     */
    fun getItemsBySaleId(saleId: Long): Flow<List<ItemPenjualan>> =
        itemPenjualanDao.getItemsBySaleId(saleId)

    /**
     * Get items by product ID
     */
    fun getItemsByProductId(productId: Long): Flow<List<ItemPenjualan>> =
        itemPenjualanDao.getItemsByProductId(productId)


    /**
     * Create item penjualan baru
     */
    suspend fun createItemPenjualan(item: ItemPenjualan): Result<ItemPenjualan> {
        return try {
            // Validasi data
            if (item.quantity <= 0) {
                return Result.failure(ChibyChibyException.ValidationError("quantity", "Quantity harus lebih dari 0"))
            }
            if (item.unitPrice < 0) {
                return Result.failure(ChibyChibyException.ValidationError("unitPrice", "Harga unit tidak boleh negatif"))
            }
            if (item.totalPrice < 0) {
                return Result.failure(ChibyChibyException.ValidationError("totalPrice", "Total harga tidak boleh negatif"))
            }

            val itemId = itemPenjualanDao.insertItemPenjualan(item)
            val createdItem = item.copy(id = itemId)
            Result.success(createdItem)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createItemPenjualan", e))
        }
    }



    /**
     * Delete items by sale ID
     */
    suspend fun deleteItemsBySaleId(saleId: Long): Result<Unit> {
        return try {
            itemPenjualanDao.deleteItemsBySaleId(saleId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteItemsBySaleId", e))
        }
    }

    /**
     * Insert batch items
     */
    suspend fun insertItemPenjualanBatch(items: List<ItemPenjualan>): Result<Unit> {
        return try {
            itemPenjualanDao.insertItemPenjualanList(items)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("insertItemPenjualanBatch", e))
        }
    }
}