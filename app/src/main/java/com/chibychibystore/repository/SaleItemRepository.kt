package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.SaleItemDao
import com.chibychibystore.data.local.entity.SaleItem
import com.chibychibystore.data.model.Result
import com.chibychibystore.data.model.TopProductDto
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data ItemPenjualan
 */
@Singleton
class SaleItemRepository @Inject constructor(
    private val saleItemDao: SaleItemDao
) {

    fun getAllSaleItems(): Flow<List<SaleItem>> = saleItemDao.getAllSaleItems()

    /**
     * Get items by sale ID
     */
    fun getItemsBySaleId(saleId: Long): Flow<List<SaleItem>> =
        saleItemDao.getItemsBySaleId(saleId)

    /**
     * Get items by product ID
     */
    fun getItemsByProductId(productId: Long): Flow<List<SaleItem>> =
        saleItemDao.getItemsByProductId(productId)


    /**
     * Create item penjualan baru
     */
    suspend fun createSaleItem(item: SaleItem): Result<SaleItem> {
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

            val itemId = saleItemDao.insertSaleItem(item)
            val createdItem = item.copy(id = itemId)
            Result.success(createdItem)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createSaleItem", e))
        }
    }



    /**
     * Delete items by sale ID
     */
    suspend fun deleteItemsBySaleId(saleId: Long): Result<Unit> {
        return try {
            saleItemDao.deleteItemsBySaleId(saleId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteItemsBySaleId", e))
        }
    }

    /**
     * Insert batch items
     */
    suspend fun insertSaleItemBatch(items: List<SaleItem>): Result<Unit> {
        return try {
            saleItemDao.insertSaleItemList(items)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("insertSaleItemBatch", e))
        }
    }

    /**
     * Compatibility wrapper for legacy code/tests that expect single insert returning id
     */
    suspend fun insertSaleItem(item: SaleItem): Long {
        return saleItemDao.insertSaleItem(item)
    }

    /**
     * Compatibility wrapper for DAO delete by penjualanId
     */
    suspend fun deleteSaleItemBySaleId(saleId: Long): Result<Unit> {
        return try {
            saleItemDao.deleteItemsBySaleId(saleId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteSaleItemBySaleId", e))
        }
    }

    /**
     * Get top selling products
     */
    suspend fun getTopSellingProducts(limit: Int): Result<List<TopProductDto>> {
        return try {
            val result = saleItemDao.getTopSellingProducts(limit)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTopSellingProducts", e))
        }
    }

    /**
     * Get aggregated product sales stats within date range
     */
    suspend fun getProductSalesStats(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Result<List<TopProductDto>> {
        return try {
            val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            val result = saleItemDao.getProductSalesStats(start, end)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProductSalesStats", e))
        }
    }
}
