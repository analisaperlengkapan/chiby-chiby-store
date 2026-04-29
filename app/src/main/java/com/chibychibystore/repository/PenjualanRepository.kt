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
        val (start, end) = getDateRange(startDate, endDate)
        return penjualanDao.getPenjualanByRentangTanggal(start, end).first()
    }

    /**
     * Get penjualan with items in date range
     */
    suspend fun getSalesWithItemsInDateRange(startDate: LocalDate, endDate: LocalDate): List<PenjualanWithItems> {
        val (start, end) = getDateRange(startDate, endDate)
        return penjualanDao.getPenjualanWithItemsByRentangTanggal(start, end).first()
    }

    private fun getDateRange(startDate: LocalDate, endDate: LocalDate): Pair<Date, Date> {
        val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
        return Pair(start, end)
    }

    /**
     * Create penjualan baru dengan items
     */
    suspend fun createPenjualan(penjualan: Penjualan, items: List<ItemPenjualan>): Result<PenjualanWithItems> {
        return try {
            // Persist the caller-supplied totalAmount as-is. The service layer is
            // responsible for computing the final total (subtotal + tax - discount),
            // and overriding it here with the raw item subtotal would silently drop
            // tax and discount from the persisted sale. That breaks any downstream
            // consumer that sums `totalAmount` to compute actual cash receipts —
            // notably shift cash reconciliation (`getTotalSalesByShift` /
            // `getTotalCashSalesByShift`), which would produce a misleading
            // expected-vs-actual cash discrepancy at shift close.
            val penjualanId = penjualanDao.insertPenjualan(penjualan)

            val itemsWithPenjualanId = items.map { it.copy(saleId = penjualanId) }
            itemPenjualanDao.insertItemPenjualanList(itemsWithPenjualanId)

            getPenjualanWithItemsById(penjualanId)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPenjualan", e))
        }
    }

    /**
     * Create multiple penjualan with items (Batch)
     */
    suspend fun createPenjualanList(sales: List<Penjualan>, items: List<ItemPenjualan>): Result<Int> {
        return try {
            // Filter items to ensure we only insert items belonging to the provided sales
            val saleIds = sales.map { it.id }.toSet()
            val relevantItems = items.filter { it.saleId in saleIds }

            // Insert sales batch
            penjualanDao.insertPenjualanList(sales)

            // Insert items batch
            if (relevantItems.isNotEmpty()) {
                itemPenjualanDao.insertItemPenjualanList(relevantItems)
            }

            Result.success(sales.size)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPenjualanList", e))
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
     * Get total penjualan amount by date range.
     *
     * As of MIGRATION_11_12, `Penjualan.totalAmount` stores the post-tax/discount
     * amount actually paid (subtotal + tax - discount, floored at 0). This method
     * therefore returns gross cash flow INCLUDING tax — it is NOT net revenue.
     * Use [getTotalRevenue] for tax-exclusive revenue figures, or
     * [getTotalCashReceipts] for non-refunded gross cash inflows.
     *
     * Includes refunded sales; for non-refunded only, see [getTotalCashReceipts].
     */
    suspend fun getTotalPenjualanAmount(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val (start, end) = getDateRange(startDate, endDate)
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
            val (start, end) = getDateRange(startDate, endDate)
            val count = penjualanDao.getPenjualanCountByDateRange(start, end)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPenjualanCountByDateRange", e))
        }
    }

    /**
     * Get total cash receipts (non-refunded `SUM(totalAmount)`).
     *
     * As of MIGRATION_11_12, `totalAmount` is post-tax/discount, so this is the
     * gross cash flow from sales (INCLUDING tax) — i.e. what the customer paid.
     * Use [getTotalRevenue] for tax-exclusive revenue.
     *
     * Despite the name, this aggregates ALL payment methods, not just cash.
     */
    suspend fun getTotalCashReceipts(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val (start, end) = getDateRange(startDate, endDate)
            val total = penjualanDao.getTotalCashReceipts(start, end) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalCashReceipts", e))
        }
    }

    /**
     * Get total revenue (non-refunded total amount minus tax)
     */
    suspend fun getTotalRevenue(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val (start, end) = getDateRange(startDate, endDate)
            val total = penjualanDao.getTotalRevenue(start, end) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalRevenue", e))
        }
    }

    /**
     * Get count of non-refunded sales
     */
    suspend fun getPenjualanCountNonRefunded(startDate: LocalDate, endDate: LocalDate): Result<Int> {
        return try {
            val (start, end) = getDateRange(startDate, endDate)
            val count = penjualanDao.getPenjualanCountNonRefunded(start, end)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPenjualanCountNonRefunded", e))
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

    /**
     * Get total sales (non-refunded) linked to a given shift
     */
    suspend fun getTotalSalesByShift(shiftId: Long): Result<Double> {
        return try {
            Result.success(penjualanDao.getTotalSalesByShift(shiftId))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalSalesByShift", e))
        }
    }

    /**
     * Get total cash sales (non-refunded) linked to a given shift
     */
    suspend fun getTotalCashSalesByShift(shiftId: Long): Result<Double> {
        return try {
            Result.success(
                penjualanDao.getTotalSalesByShiftAndPaymentMethod(
                    shiftId,
                    com.chibychibystore.data.local.entity.PaymentMethod.CASH
                )
            )
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalCashSalesByShift", e))
        }
    }
}