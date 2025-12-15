package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PengeluaranDao
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
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
    fun getAllPengeluaran(): Flow<List<Pengeluaran>> = pengeluaranDao.getAllPengeluaran()

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
    fun getPengeluaranByCategory(category: ExpenseCategory): Flow<List<Pengeluaran>> =
        pengeluaranDao.getPengeluaranByCategory(category)

    /**
     * Get pengeluaran by user
     */
    fun getPengeluaranByUser(userId: Long): Flow<List<Pengeluaran>> =
        pengeluaranDao.getPengeluaranByUser(userId)

    /**
     * Get pengeluaran yang disetujui oleh user tertentu
     */
    fun getPengeluaranApprovedBy(userId: Long): Flow<List<Pengeluaran>> =
        pengeluaranDao.getPengeluaranApprovedBy(userId)

    /**
     * Get pengeluaran dalam rentang tanggal
     */
    suspend fun getExpensesInDateRange(startDate: LocalDate, endDate: LocalDate): List<Pengeluaran> {
        val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
        return pengeluaranDao.getPengeluaranByDateRange(start, end).first()
    }

    /**
     * Get pengeluaran yang belum disetujui
     */
    fun getUnapprovedPengeluaran(): Flow<List<Pengeluaran>> =
        pengeluaranDao.getUnapprovedPengeluaran()

    /**
     * Insert pengeluaran baru
     */
    suspend fun insertPengeluaran(pengeluaran: Pengeluaran): Result<Long> {
        return try {
            // Validasi data
            if (pengeluaran.amount <= 0) {
                return Result.failure(ChibyChibyException.ValidationError("amount", "Jumlah pengeluaran harus lebih dari 0"))
            }

            // Jika jumlah > threshold, perlu approval
            val needsApproval = pengeluaran.amount > 1000000 // Rp 1 juta threshold
            val pengeluaranToInsert = if (needsApproval && pengeluaran.approvedBy == null) {
                pengeluaran.copy(approvedBy = null) // Explicitly set to null for unapproved
            } else {
                pengeluaran
            }

            val id = pengeluaranDao.insertPengeluaran(pengeluaranToInsert)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("insertPengeluaran", e))
        }
    }

    /**
     * Update pengeluaran
     */
    suspend fun updatePengeluaran(pengeluaran: Pengeluaran): Result<Unit> {
        return try {
            // Validasi data
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
            val pengeluaran = pengeluaranDao.getPengeluaranById(id)
            if (pengeluaran == null) {
                return Result.failure(ChibyChibyException.DatabaseError("Pengeluaran tidak ditemukan"))
            }

            val updatedPengeluaran = pengeluaran.copy(approvedBy = approvedBy)
            pengeluaranDao.updatePengeluaran(updatedPengeluaran)
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
     * Get jumlah pengeluaran
     */
    suspend fun getPengeluaranCount(): Result<Int> {
        return try {
            val count = pengeluaranDao.getPengeluaranCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPengeluaranCount", e))
        }
    }

    /**
     * Get total pengeluaran dalam rentang tanggal
     */
    suspend fun getTotalExpenseAmount(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            val total = pengeluaranDao.getTotalExpenseAmount(start, end) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalExpenseAmount", e))
        }
    }
}