package com.chibychibystore.repository

import androidx.room.withTransaction
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.dao.SaleDao
import com.chibychibystore.data.local.dao.SaleItemDao
import com.chibychibystore.data.local.entity.SaleItem
import com.chibychibystore.data.local.entity.Sale
import com.chibychibystore.data.local.entity.SaleWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Penjualan
 */
@Singleton
class SaleRepository @Inject constructor(
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val database: ChibyChibyDatabase
) {

    /**
     * Get semua penjualan
     */
    fun getAllSales(): Flow<List<Sale>> = saleDao.getAllSales()

    /**
     * Get recent sales with limit
     */
    fun getRecentSales(limit: Int): Flow<List<Sale>> = saleDao.getRecentSales(limit)

    /**
     * Get penjualan by ID
     */
    suspend fun getSaleById(id: Long): Result<Sale> {
        return try {
            val sale = saleDao.getSaleById(id)
            if (sale != null) {
                Result.success(sale)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Sale with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getSaleById", e))
        }
    }

    /**
     * Get penjualan dengan items by ID
     */
    suspend fun getSaleWithItemsById(id: Long): Result<SaleWithItems> {
        return try {
            val saleWithItems = saleDao.getSaleWithItems(id)
            if (saleWithItems != null) {
                Result.success(saleWithItems)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Sale with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getSaleWithItemsById", e))
        }
    }

    /**
     * Get penjualan in date range
     */
    suspend fun getSalesInDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate): List<Sale> {
        val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
        return saleDao.getSalesByDateRange(start, end).first()
    }

    /**
     * Get penjualan in date range with optional filtering
     */
    suspend fun getSalesFiltered(startDate: java.time.LocalDate, endDate: java.time.LocalDate, cashierId: Long? = null, query: String? = null): List<Sale> {
        val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
        return saleDao.getSalesFiltered(start, end, cashierId, query)
    }

    /**
     * Observe sales in date range with optional query
     */
    fun observeSalesFiltered(startDate: java.time.LocalDate, endDate: java.time.LocalDate, query: String? = null): Flow<List<Sale>> {
        val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
        return saleDao.observeSalesFiltered(start, end, query)
    }

    /**
     * Create penjualan baru dengan items
     */
    suspend fun createSale(sale: Sale, items: List<SaleItem>): Result<SaleWithItems> {
        return try {
            // Validasi data
            if (items.isEmpty()) {
                return Result.failure(ChibyChibyException.ValidationError("items", "Sale must have at least 1 item"))
            }

            // Hitung total amount dari items
            val totalAmount = items.sumOf { it.totalPrice }
            val saleWithTotal = sale.copy(totalAmount = totalAmount)

            // Insert penjualan dan items dalam transaksi
            val saleId = database.withTransaction {
                val id = saleDao.insertSale(saleWithTotal)
                val itemsWithSaleId = items.map { it.copy(saleId = id) }
                saleItemDao.insertSaleItemList(itemsWithSaleId)
                id
            }

            // Return penjualan dengan items
            getSaleWithItemsById(saleId)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createSale", e))
        }
    }

    /**
     * Compatibility wrapper: insert single penjualan and return its id
     */
    suspend fun insertSale(sale: Sale): Long {
        return saleDao.insertSale(sale)
    }

    /**
     * Compatibility wrapper: update penjualan by passing full object
     */
    suspend fun updateSale(sale: Sale): Result<Unit> {
        return try {
            saleDao.updateSale(sale)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateSale", e))
        }
    }

    /**
     * Execute a block within a database transaction
     */
    suspend fun <T> runInTransaction(block: suspend () -> T): Result<T> {
        return try {
            val result = database.withTransaction {
                block()
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Transaction failed", e))
        }
    }

    /**
     * Delete penjualan
     */
    suspend fun deleteSale(id: Long): Result<Unit> {
        return try {
            val existingSale = saleDao.getSaleById(id)
            if (existingSale == null) {
                return Result.failure(ChibyChibyException.DatabaseError("Sale with ID $id not found"))
            }

            saleDao.deleteSaleById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteSale", e))
        }
    }

    /**
     * Get total penjualan by date range (Gross Amount / Cash Receipts)
     */
    suspend fun getTotalSalesAmount(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Result<Double> {
        return try {
            // Optimized query: let the database do the sum
            val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            val total = saleDao.getTotalSalesAmount(start, end) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalSalesAmount", e))
        }
    }

    /**
     * Get Total Revenue (Net Sales excluding Tax)
     */
    suspend fun getTotalRevenue(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Result<Double> {
        return try {
            val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            val total = saleDao.getTotalRevenue(start, end) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalRevenue", e))
        }
    }

    /**
     * Get Total Tax Collected
     */
    suspend fun getTotalTax(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Result<Double> {
        return try {
            val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            val total = saleDao.getTotalTax(start, end) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalTax", e))
        }
    }

    /**
     * Get Total Cash Receipts (including tax, minus discount)
     */
    suspend fun getTotalCashReceipts(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Result<Double> {
        return try {
            val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            val total = saleDao.getTotalCashReceipts(start, end) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalCashReceipts", e))
        }
    }

    /**
     * Get penjualan count by date range
     */
    suspend fun getSaleCountByDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Result<Int> {
        return try {
            val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            val count = saleDao.getSaleCountByDateRange(start, end)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getSaleCountByDateRange", e))
        }
    }

    /**
     * Get non-refunded penjualan count by date range
     */
    suspend fun getSaleCountNonRefunded(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Result<Int> {
        return try {
            val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            val count = saleDao.getSaleCountNonRefunded(start, end)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getSaleCountNonRefunded", e))
        }
    }

    /**
     * Get penjualan by date range (Flow version for observation)
     */
    fun getSalesByDateRange(startDate: String, endDate: String): Flow<List<Sale>> {
        return try {
            val start = java.util.Date.from(java.time.LocalDate.parse(startDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(java.time.LocalDate.parse(endDate).atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            saleDao.getSalesByDateRange(start, end)
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Get penjualan with items by date range (Flow version for observation)
     */
    fun getSaleWithItemsByDateRange(startDate: String, endDate: String): Flow<List<SaleWithItems>> {
        return try {
            val start = java.util.Date.from(java.time.LocalDate.parse(startDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(java.time.LocalDate.parse(endDate).atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            saleDao.getSaleWithItemsByDateRange(start, end)
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Search penjualan by query
     */
    fun searchSales(query: String): Flow<List<Sale>> {
        return saleDao.searchSales(query)
    }

    /**
     * Update penjualan
     */
    suspend fun updateSale(sale: Sale): Result<Unit> {
        return try {
            saleDao.updateSale(sale)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateSale", e))
        }
    }
}
