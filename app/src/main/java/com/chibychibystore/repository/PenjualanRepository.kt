package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ItemPenjualanDao
import com.chibychibystore.data.local.dao.PenjualanDao
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
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
            val penjualanWithItems = penjualanDao.getPenjualanWithItemsById(id)
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
     * Get penjualan by date range
     */
    fun getPenjualanByDateRange(startDate: String, endDate: String): Flow<List<Penjualan>> =
        penjualanDao.getPenjualanByDateRange(startDate, endDate)

    /**
     * Get penjualan dengan items by date range
     */
    fun getPenjualanWithItemsByDateRange(startDate: String, endDate: String): Flow<List<PenjualanWithItems>> =
        penjualanDao.getPenjualanWithItemsByDateRange(startDate, endDate)

    /**
     * Create penjualan baru dengan items
     */
    suspend fun createPenjualan(penjualan: Penjualan, items: List<ItemPenjualan>): Result<PenjualanWithItems> {
        return try {
            // Validasi data
            if (items.isEmpty()) {
                return Result.failure(ChibyChibyException.ValidationError("Penjualan harus memiliki minimal 1 item"))
            }

            // Hitung total amount dari items
            val totalAmount = items.sumOf { it.totalPrice }
            val penjualanWithTotal = penjualan.copy(totalAmount = totalAmount)

            // Insert penjualan dan items dalam transaksi
            val penjualanId = penjualanDao.insertPenjualan(penjualanWithTotal)

            val itemsWithPenjualanId = items.map { it.copy(penjualanId = penjualanId) }
            itemPenjualanDao.insertItemPenjualanBatch(itemsWithPenjualanId)

            // Return penjualan dengan items
            getPenjualanWithItemsById(penjualanId)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPenjualan", e))
        }
    }

    /**
     * Update penjualan
     */
    suspend fun updatePenjualan(id: Long, penjualan: Penjualan): Result<Penjualan> {
        return try {
            val existingPenjualan = penjualanDao.getPenjualanById(id)
            if (existingPenjualan == null) {
                return Result.failure(ChibyChibyException.DatabaseError("Penjualan dengan ID $id tidak ditemukan"))
            }

            val updatedPenjualan = penjualan.copy(id = id)
            penjualanDao.updatePenjualan(updatedPenjualan)
            Result.success(updatedPenjualan)
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
     * Search penjualan
     */
    fun searchPenjualan(query: String): Flow<List<Penjualan>> =
        penjualanDao.searchPenjualan(query)

    /**
     * Get total penjualan by date range
     */
    suspend fun getTotalPenjualanByDateRange(startDate: String, endDate: String): Result<Double> {
        return try {
            val total = penjualanDao.getTotalPenjualanByDateRange(startDate, endDate) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalPenjualanByDateRange", e))
        }
    }
}