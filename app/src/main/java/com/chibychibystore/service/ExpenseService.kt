package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Expense
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
    suspend fun createExpense(expense: Expense): Result<Expense>
    
    /**
     * Update pengeluaran existing
     */
    suspend fun updateExpense(expense: Expense): Result<Expense>
    
    /**
     * Menghapus pengeluaran
     */
    suspend fun deleteExpense(id: Long): Result<Unit>
    
    /**
     * Mendapatkan pengeluaran berdasarkan ID
     */
    suspend fun getExpense(id: Long): Result<Expense?>
    
    /**
     * Mendapatkan semua pengeluaran dengan filter
     * @param category Filter berdasarkan nama kategori (opsional)
     */
    suspend fun getExpenses(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        category: String? = null
    ): Result<List<Expense>>
    
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
    fun observeExpenses(): Flow<List<Expense>>
    
    /**
     * Observable untuk pengeluaran berdasarkan kategori
     */
    fun observeExpensesByCategory(category: String): Flow<List<Expense>>
}
