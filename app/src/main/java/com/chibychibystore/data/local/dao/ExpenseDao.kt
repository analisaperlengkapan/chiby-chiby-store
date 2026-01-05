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

    @Query("SELECT * FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate ORDER BY expenseDate DESC")
    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>>

    @Query("SELECT * FROM pengeluaran WHERE category = :category ORDER BY expenseDate DESC")
    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM pengeluaran WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT SUM(amount) FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpense(startDate: Date, endDate: Date): Double?

    @Query("SELECT category, SUM(amount) as total FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate GROUP BY category")
    suspend fun getExpenseSummaryByCategory(startDate: Date, endDate: Date): Map<@androidx.room.MapColumn(columnName = "category") ExpenseCategory, @androidx.room.MapColumn(columnName = "total") Double>

    @Query("UPDATE pengeluaran SET approvedBy = :approverId WHERE id = :id")
    suspend fun approveExpense(id: Long, approverId: Long)
}
