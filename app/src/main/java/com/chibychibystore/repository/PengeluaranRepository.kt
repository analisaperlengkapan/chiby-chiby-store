package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PengeluaranDao
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Pengeluaran
 */
@Singleton
class PengeluaranRepository @Inject constructor(
    private val pengeluaranDao: PengeluaranDao
) {

    /**
     * Get semua pengeluaran
     */
    fun getAllPengeluarans(): Flow<List<Pengeluaran>> = pengeluaranDao.getAllPengeluarans()

    /**
     * Get pengeluaran by ID
     */
    suspend fun getPengeluaranById(id: Long): Result<Pengeluaran> {
        return try {
            val pengeluaran = pengeluaranDao.getPengeluaranById(id)
            if (pengeluaran != null) {
                Result.success(pengeluaran)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Pengeluaran dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPengeluaranById", e))
        }
    }

    /**
     * Get pengeluaran by kategori
     */
    fun getPengeluaransByKategori(category: KategoriPengeluaran): Flow<List<Pengeluaran>> =
        pengeluaranDao.getPengeluaransByKategori(category)

    /**
     * Get pengeluaran dalam rentang tanggal (Flow)
     */
    fun getPengeluaransByDateRange(startDate: Date, endDate: Date): Flow<List<Pengeluaran>> {
        return pengeluaranDao.getPengeluaransByDateRange(startDate, endDate)
    }

    /**
     * Get pengeluaran dalam rentang tanggal (List)
     */
    suspend fun getPengeluaransByDateRangeList(startDate: Date, endDate: Date): List<Pengeluaran> {
        return pengeluaranDao.getPengeluaransByDateRangeList(startDate, endDate)
    }

    /**
     * Get pengeluaran dalam rentang tanggal dan kategori (List)
     */
    suspend fun getPengeluaransByDateRangeAndKategori(startDate: Date, endDate: Date, category: KategoriPengeluaran): List<Pengeluaran> {
        return pengeluaranDao.getPengeluaransByDateRangeAndKategori(startDate, endDate, category)
    }

    /**
     * Create pengeluaran baru
     */
    suspend fun createPengeluaran(pengeluaran: Pengeluaran): Result<Long> {
        return try {
            if (pengeluaran.amount <= 0) {
                return Result.failure(ChibyChibyException.ValidationError("amount", "Jumlah pengeluaran harus lebih dari 0"))
            }

            val id = pengeluaranDao.insertPengeluaran(pengeluaran)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPengeluaran", e))
        }
    }

    /**
     * Update pengeluaran
     */
    suspend fun updatePengeluaran(pengeluaran: Pengeluaran): Result<Unit> {
        return try {
            if (pengeluaran.amount <= 0) {
                return Result.failure(ChibyChibyException.ValidationError("amount", "Jumlah pengeluaran harus lebih dari 0"))
            }

            pengeluaranDao.updatePengeluaran(pengeluaran)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updatePengeluaran", e))
        }
    }

    /**
     * Approve pengeluaran
     */
    suspend fun approvePengeluaran(id: Long, approvedBy: Long): Result<Unit> {
        return try {
            pengeluaranDao.approvePengeluaran(id, approvedBy)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("approvePengeluaran", e))
        }
    }

    /**
     * Delete pengeluaran
     */
    suspend fun deletePengeluaran(id: Long): Result<Unit> {
        return try {
            pengeluaranDao.deletePengeluaranById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deletePengeluaran", e))
        }
    }

    /**
     * Get total pengeluaran dalam rentang tanggal
     */
    suspend fun getTotalPengeluaranAmount(startDate: Date, endDate: Date): Result<Double> {
        return try {
            val total = pengeluaranDao.getTotalPengeluaranAmount(startDate, endDate) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalPengeluaranAmount", e))
        }
    }
}