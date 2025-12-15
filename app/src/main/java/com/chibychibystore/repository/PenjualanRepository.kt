package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ItemPenjualanDao
import com.chibychibystore.data.local.dao.PenjualanDao
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import javax.inject.Inject
import javax.inject.Singleton

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
    suspend fun getSalesInDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate): List<Penjualan> {
        val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
        return penjualanDao.getPenjualanByDateRange(start, end).first()
    }

    /**
     * Create penjualan baru dengan items
     */
    suspend fun createPenjualan(penjualan: Penjualan, items: List<ItemPenjualan>): Result<PenjualanWithItems> {
        return try {
            // Validasi data
            if (items.isEmpty()) {
                return Result.failure(ChibyChibyException.ValidationError("items", "Penjualan harus memiliki minimal 1 item"))
            }

            // Hitung total amount dari items
            val totalAmount = items.sumOf { it.totalPrice }
            val penjualanWithTotal = penjualan.copy(totalAmount = totalAmount)

            // Insert penjualan dan items dalam transaksi
            val penjualanId = penjualanDao.insertPenjualan(penjualanWithTotal)

            val itemsWithPenjualanId = items.map { it.copy(saleId = penjualanId) }
            itemPenjualanDao.insertItemPenjualanList(itemsWithPenjualanId)

            // Return penjualan dengan items
            getPenjualanWithItemsById(penjualanId)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPenjualan", e))
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
    suspend fun getTotalPenjualanByDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Result<Double> {
        return try {
            val sales = getSalesInDateRange(startDate, endDate)
            val total = sales.sumOf { it.totalAmount }
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalPenjualanByDateRange", e))
        }
    }

    /**
     * Get penjualan by date range (Flow version for observation)
     */
    fun getPenjualanByDateRange(startDate: String, endDate: String): Flow<List<Penjualan>> {
        return try {
            val start = java.util.Date.from(java.time.LocalDate.parse(startDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(java.time.LocalDate.parse(endDate).atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            penjualanDao.getPenjualanByDateRange(start, end)
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Get penjualan with items by date range (Flow version for observation)
     */
    fun getPenjualanWithItemsByDateRange(startDate: String, endDate: String): Flow<List<PenjualanWithItems>> {
        return try {
            val start = java.util.Date.from(java.time.LocalDate.parse(startDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val end = java.util.Date.from(java.time.LocalDate.parse(endDate).atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
            penjualanDao.getPenjualanWithItemsByDateRange(start, end)
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Search penjualan by query
     */
    fun searchPenjualan(query: String): Flow<List<Penjualan>> {
        return penjualanDao.getAllPenjualan()
    }

    /**
     * Update penjualan
     */
    suspend fun updatePenjualan(id: Long, sale: Penjualan): Result<Unit> {
        return try {
            penjualanDao.updatePenjualan(sale)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updatePenjualan", e))
        }
    }
}