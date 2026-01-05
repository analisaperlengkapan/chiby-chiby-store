package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ItemPenjualanDao
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.model.Result
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
    fun getItemsByPenjualanId(saleId: Long): Flow<List<ItemPenjualan>> =
        itemPenjualanDao.getItemsByPenjualanId(saleId)

    /**
     * Get items by product ID
     */
    fun getItemsByProdukId(productId: Long): Flow<List<ItemPenjualan>> =
        itemPenjualanDao.getItemsByProdukId(productId)

    /**
     * Get all item penjualan
     */
    fun getAllItemPenjualan(): Flow<List<ItemPenjualan>> =
        itemPenjualanDao.getAllItemPenjualan()

    /**
     * Get top selling products
     */
    suspend fun getTopSellingProduks(limit: Int): Result<List<com.chibychibystore.data.model.ProdukTerpopulerDto>> {
        return try {
             Result.success(itemPenjualanDao.getProdukTerpopuler(limit))
        } catch (e: Exception) {
             Result.failure(e)
        }
    }

    /**
     * Get product sales stats by date range
     */
    suspend fun getProdukPenjualansStats(startDate: java.util.Date, endDate: java.util.Date): Result<List<com.chibychibystore.data.model.ProdukTerpopulerDto>> {
        return try {
             Result.success(itemPenjualanDao.getProdukPenjualanStats(startDate, endDate))
        } catch (e: Exception) {
             Result.failure(e)
        }
    }

    /**
     * Create item penjualan baru
     */
    suspend fun createItemPenjualan(item: ItemPenjualan): Result<ItemPenjualan> {
        return try {
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
    suspend fun deleteItemsByPenjualanId(saleId: Long): Result<Unit> {
        return try {
            itemPenjualanDao.deleteItemsByPenjualanId(saleId)
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
            itemPenjualanDao.insertItemPenjualanList(items)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("insertItemPenjualanBatch", e))
        }
    }

    /**
     * Compatibility wrapper
     */
    suspend fun insertItemPenjualan(item: ItemPenjualan): Long {
        return itemPenjualanDao.insertItemPenjualan(item)
    }

    /**
     * Compatibility wrapper
     */
    suspend fun deleteItemPenjualanByPenjualanId(penjualanId: Long): Result<Unit> {
        return deleteItemsByPenjualanId(penjualanId)
    }
}