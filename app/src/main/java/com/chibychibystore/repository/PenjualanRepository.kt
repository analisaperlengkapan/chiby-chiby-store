package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ItemPenjualanDao
import com.chibychibystore.data.local.dao.PenjualanDao
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository untuk operasi data Penjualan
 */
@Singleton
class PenjualanRepository @Inject constructor(
    private val penjualanDao: PenjualanDao,
    private val itemPenjualanDao: ItemPenjualanDao
) {

    /**
     * Get semua penjualan
     */
    fun getAllPenjualan(): Flow<List<Penjualan>> = penjualanDao.getAllPenjualan()

    /**
     * Get semua penjualan dengan items
     */
    fun getAllPenjualanWithItems(): Flow<List<PenjualanWithItems>> = penjualanDao.getAllPenjualanWithItems()

    /**
     * Get recent sales with limit
     */
    fun getRecentPenjualan(limit: Int): Flow<List<Penjualan>> = penjualanDao.getRecentPenjualan(limit)

    /**
     * Get penjualan by ID
     */
    suspend fun getPenjualanById(id: Long): Result<Penjualan> {
        return try {
            val penjualan = penjualanDao.getPenjualanById(id)
            if (penjualan != null) {
                Result.success(penjualan)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Penjualan dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPenjualanById", e))
        }
    }

    /**
     * Get penjualan dengan items by ID
     */
    suspend fun getPenjualanWithItemsById(id: Long): Result<PenjualanWithItems> {
        return try {
            val penjualanWithItems = penjualanDao.getPenjualanWithItems(id)
            if (penjualanWithItems != null) {
                Result.success(penjualanWithItems)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Penjualan dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPenjualanWithItemsById", e))
        }
    }

    /**
     * Get penjualan in date range
     */
    suspend fun getSalesInDateRange(startDate: LocalDate, endDate: LocalDate): List<Penjualan> {
        val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
        return penjualanDao.getPenjualanByRentangTanggal(start, end).first()
    }

    /**
     * Create penjualan baru dengan items
     */
    suspend fun createPenjualan(penjualan: Penjualan, items: List<ItemPenjualan>): Result<PenjualanWithItems> {
        return try {
            val totalAmount = items.sumOf { it.totalPrice }
            val penjualanWithTotal = penjualan.copy(totalAmount = totalAmount)

            val penjualanId = penjualanDao.insertPenjualan(penjualanWithTotal)

            val itemsWithPenjualanId = items.map { it.copy(saleId = penjualanId) }
            itemPenjualanDao.insertItemPenjualanList(itemsWithPenjualanId)

            getPenjualanWithItemsById(penjualanId)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPenjualan", e))
        }
    }

    /**
     * Compatibility wrapper: insert single penjualan and return its id
     */
    suspend fun insertPenjualan(penjualan: Penjualan): Long {
        return penjualanDao.insertPenjualan(penjualan)
    }

    /**
     * Compatibility wrapper: update penjualan by passing full object
     */
    suspend fun updatePenjualan(penjualan: Penjualan): Result<Unit> {
        return try {
            penjualanDao.updatePenjualan(penjualan)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updatePenjualan", e))
        }
    }

    /**
     * Delete penjualan
     */
    suspend fun deletePenjualan(id: Long): Result<Unit> {
        return try {
            val existingPenjualan = penjualanDao.getPenjualanById(id)
            if (existingPenjualan == null) {
                return Result.failure(ChibyChibyException.DatabaseError("Penjualan dengan ID $id tidak ditemukan"))
            }

            penjualanDao.deletePenjualanById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deletePenjualan", e))
        }
    }

    /**
     * Get total penjualan by date range
     */
    suspend fun getTotalPenjualanAmount(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            val total = penjualanDao.getTotalPenjualanAmount(start, end) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalPenjualanByDateRange", e))
        }
    }

    /**
     * Get penjualan count by date range
     */
    suspend fun getPenjualanCountByDateRange(startDate: LocalDate, endDate: LocalDate): Result<Int> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            val count = penjualanDao.getPenjualanCountByDateRange(start, end)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPenjualanCountByDateRange", e))
        }
    }

    suspend fun getTotalCashReceipts(startDate: Date, endDate: Date): Result<Double> {
        return try {
             // Reusing the wrapper that takes LocalDate? No, converting Date to LocalDate is annoying.
             // But existing method takes LocalDate.
             // Let's implement directly or forward.
             // Actually, ReportingServiceImpl passes Date to this method (because I didn't verify .toDate removal for this call? No, ReportingService getGrossSales uses .toDate()).
             // So inputs are Date.
             // But getTotalPenjualanAmount takes LocalDate.
             // Let's overload it or convert.
             val localStart = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
             val localEnd = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
             getTotalPenjualanAmount(localStart, localEnd)
        } catch (e: Exception) {
             Result.failure(e)
        }
    }

    suspend fun getTotalRevenue(startDate: Date, endDate: Date): Result<Double> = getTotalCashReceipts(startDate, endDate)

    suspend fun getPenjualanCountNonRefunded(startDate: Date, endDate: Date): Result<Int> {
         return try {
             val localStart = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
             val localEnd = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
             getPenjualanCountByDateRange(localStart, localEnd) // Assumption: all valid sales count
         } catch (e: Exception) {
             Result.failure(e)
         }
    }

    /**
     * Get penjualan by date range (Flow version for observation)
     */
    fun getPenjualanByRentangTanggal(startDate: String, endDate: String): Flow<List<Penjualan>> {
        return try {
            val start = Date.from(LocalDate.parse(startDate).atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(LocalDate.parse(endDate).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            penjualanDao.getPenjualanByRentangTanggal(start, end)
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Get penjualan with items by date range (Flow version for observation)
     */
    fun getPenjualanWithItemsByRentangTanggal(startDate: String, endDate: String): Flow<List<PenjualanWithItems>> {
        return try {
            val start = Date.from(LocalDate.parse(startDate).atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(LocalDate.parse(endDate).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            penjualanDao.getPenjualanWithItemsByRentangTanggal(start, end)
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Search penjualan by query
     */
    fun searchPenjualan(query: String): Flow<List<Penjualan>> {
        return penjualanDao.searchPenjualan(query)
    }

    /**
     * Update penjualan
     */
    suspend fun updatePenjualan(id: Long, sale: Penjualan): Result<Unit> {
        return updatePenjualan(sale)
    }
}