package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Interface untuk Expense Service
 * 
 * Service ini menangani manajemen pengeluaran bisnis:
 * - CRUD operations untuk pengeluaran
 * - Kategorisasi pengeluaran
 * - Laporan pengeluaran
 * - Approval workflow
 */
interface ExpenseService {
    
    /**
     * Membuat pengeluaran baru
     */
    suspend fun createExpense(expense: Pengeluaran): Result<Pengeluaran>
    
    /**
     * Update pengeluaran existing
     */
    suspend fun updateExpense(expense: Pengeluaran): Result<Pengeluaran>
    
    /**
     * Menghapus pengeluaran
     */
    suspend fun deleteExpense(id: Long): Result<Unit>
    
    /**
     * Mendapatkan pengeluaran berdasarkan ID
     */
    suspend fun getExpense(id: Long): Result<Pengeluaran?>
    
    /**
     * Mendapatkan semua pengeluaran dengan filter
     */
    suspend fun getExpenses(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        category: String? = null
    ): Result<List<Pengeluaran>>
    
    /**
     * Mendapatkan total pengeluaran dalam periode
     */
    suspend fun getTotalExpenses(startDate: LocalDate, endDate: LocalDate): Result<Double>
    
    /**
     * Approve pengeluaran (untuk manager/owner)
     */
    suspend fun approveExpense(id: Long, approverId: Long): Result<Unit>
    
    /**
     * Observable untuk pengeluaran
     */
    fun observeExpenses(): Flow<List<Pengeluaran>>
    
    /**
     * Observable untuk pengeluaran berdasarkan kategori
     */
    fun observeExpensesByCategory(category: String): Flow<List<Pengeluaran>>
}
