package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Expense
import com.chibychibystore.data.local.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM pengeluaran ORDER BY expenseDate DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM pengeluaran WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT * FROM pengeluaran WHERE category = :category ORDER BY expenseDate DESC")
    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>>

    @Query("SELECT * FROM pengeluaran WHERE createdBy = :userId ORDER BY expenseDate DESC")
    fun getExpensesByUser(userId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM pengeluaran WHERE approvedBy = :userId ORDER BY expenseDate DESC")
    fun getExpensesApprovedBy(userId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate ORDER BY expenseDate DESC")
    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>>

    // Optimized query for Date + Category
    @Query("SELECT * FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate AND category = :category ORDER BY expenseDate DESC")
    suspend fun getExpensesByDateRangeAndCategory(startDate: Date, endDate: Date, category: ExpenseCategory): List<Expense>

    @Query("SELECT * FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate ORDER BY expenseDate DESC")
    suspend fun getExpensesByDateRangeList(startDate: Date, endDate: Date): List<Expense>

    @Query("SELECT * FROM pengeluaran WHERE approvedBy IS NULL ORDER BY expenseDate DESC")
    fun getUnapprovedExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM pengeluaran WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT COUNT(*) FROM pengeluaran")
    suspend fun getExpenseCount(): Int

    @Query("SELECT SUM(amount) FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpenseAmount(startDate: Date, endDate: Date): Double?
}
