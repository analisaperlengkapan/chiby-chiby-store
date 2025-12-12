package com.chibychibystore.service

import com.chibychibystore.data.Result
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.repository.PengeluaranRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service untuk manajemen pengeluaran retail
 */
@Singleton
class ExpenseService @Inject constructor(
    private val pengeluaranRepository: PengeluaranRepository,
    private val authService: AuthService
) {

    /**
     * Get semua pengeluaran
     */
    fun getAllPengeluaran(): Flow<List<Pengeluaran>> =
        pengeluaranRepository.getAllPengeluaran()

    /**
     * Get pengeluaran by ID
     */
    suspend fun getPengeluaranById(id: Long): Result<Pengeluaran> =
        pengeluaranRepository.getPengeluaranById(id)

    /**
     * Get pengeluaran by kategori
     */
    fun getPengeluaranByCategory(category: ExpenseCategory): Flow<List<Pengeluaran>> =
        pengeluaranRepository.getPengeluaranByCategory(category)

    /**
     * Get pengeluaran dalam rentang tanggal
     */
    suspend fun getExpensesInDateRange(startDate: LocalDate, endDate: LocalDate): List<Pengeluaran> =
        pengeluaranRepository.getExpensesInDateRange(startDate, endDate)

    /**
     * Get pengeluaran yang belum disetujui
     */
    fun getUnapprovedPengeluaran(): Flow<List<Pengeluaran>> =
        pengeluaranRepository.getUnapprovedPengeluaran()

    /**
     * Create pengeluaran baru
     */
    suspend fun createExpense(
        expenseDate: java.util.Date,
        category: ExpenseCategory,
        amount: Double,
        description: String? = null
    ): Result<Long> {
        return try {
            // Validasi permission
            val currentUser = authService.getCurrentUser()
                ?: return Result.Error("User tidak terautentikasi")

            // Validasi data
            if (amount <= 0) {
                return Result.Error("Jumlah pengeluaran harus lebih dari 0")
            }

            // Cek apakah perlu approval untuk jumlah besar
            val needsApproval = amount > 1000000 // Rp 1 juta threshold
            val approvedBy = if (needsApproval) null else currentUser.id

            val pengeluaran = Pengeluaran(
                expenseDate = expenseDate,
                category = category,
                amount = amount,
                description = description,
                approvedBy = approvedBy,
                createdBy = currentUser.id
            )

            pengeluaranRepository.insertPengeluaran(pengeluaran)
        } catch (e: Exception) {
            Result.Error("Gagal membuat pengeluaran: ${e.message}")
        }
    }

    /**
     * Update pengeluaran
     */
    suspend fun updateExpense(
        id: Long,
        expenseDate: java.util.Date,
        category: ExpenseCategory,
        amount: Double,
        description: String? = null
    ): Result<Unit> {
        return try {
            // Validasi permission
            val currentUser = authService.getCurrentUser()
                ?: return Result.Error("User tidak terautentikasi")

            // Cek apakah user memiliki permission untuk edit
            if (!authService.hasPermission("EDIT_EXPENSE")) {
                return Result.Error("Tidak memiliki izin untuk mengedit pengeluaran")
            }

            // Get existing expense
            val existingExpense = pengeluaranRepository.getPengeluaranById(id)
                .getOrNull() ?: return Result.Error("Pengeluaran tidak ditemukan")

            // Validasi data
            if (amount <= 0) {
                return Result.Error("Jumlah pengeluaran harus lebih dari 0")
            }

            // Cek apakah perlu approval untuk perubahan jumlah besar
            val needsApproval = amount > 1000000
            val approvedBy = if (needsApproval && existingExpense.approvedBy == null) null else existingExpense.approvedBy

            val updatedPengeluaran = existingExpense.copy(
                expenseDate = expenseDate,
                category = category,
                amount = amount,
                description = description,
                approvedBy = approvedBy
            )

            pengeluaranRepository.updatePengeluaran(updatedPengeluaran)
        } catch (e: Exception) {
            Result.Error("Gagal mengupdate pengeluaran: ${e.message}")
        }
    }

    /**
     * Approve pengeluaran
     */
    suspend fun approveExpense(id: Long): Result<Unit> {
        return try {
            // Validasi permission
            val currentUser = authService.getCurrentUser()
                ?: return Result.Error("User tidak terautentikasi")

            // Cek apakah user memiliki permission untuk approve
            if (!authService.hasPermission("APPROVE_EXPENSE")) {
                return Result.Error("Tidak memiliki izin untuk menyetujui pengeluaran")
            }

            pengeluaranRepository.approvePengeluaran(id, currentUser.id)
        } catch (e: Exception) {
            Result.Error("Gagal menyetujui pengeluaran: ${e.message}")
        }
    }

    /**
     * Delete pengeluaran
     */
    suspend fun deleteExpense(id: Long): Result<Unit> {
        return try {
            // Validasi permission
            val currentUser = authService.getCurrentUser()
                ?: return Result.Error("User tidak terautentikasi")

            // Cek apakah user memiliki permission untuk delete
            if (!authService.hasPermission("DELETE_EXPENSE")) {
                return Result.Error("Tidak memiliki izin untuk menghapus pengeluaran")
            }

            pengeluaranRepository.deletePengeluaran(id)
        } catch (e: Exception) {
            Result.Error("Gagal menghapus pengeluaran: ${e.message}")
        }
    }

    /**
     * Get total pengeluaran dalam rentang tanggal
     */
    suspend fun getTotalExpenses(startDate: LocalDate, endDate: LocalDate): Result<Double> =
        pengeluaranRepository.getTotalExpenseAmount(startDate, endDate)

    /**
     * Get kategori pengeluaran yang tersedia
     */
    fun getExpenseCategories(): List<ExpenseCategory> = ExpenseCategory.values().toList()

    /**
     * Get kategori pengeluaran berdasarkan display name
     */
    fun getExpenseCategoryByDisplayName(displayName: String): ExpenseCategory? =
        ExpenseCategory.fromDisplayName(displayName)
}