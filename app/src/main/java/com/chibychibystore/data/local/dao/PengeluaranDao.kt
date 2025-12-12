package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.local.entity.Pengeluaran
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Date

@Dao
interface PengeluaranDao {
    @Query("SELECT * FROM pengeluaran ORDER BY expenseDate DESC")
    fun getAllPengeluaran(): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE id = :id")
    suspend fun getPengeluaranById(id: Long): Pengeluaran?

    @Query("SELECT * FROM pengeluaran WHERE category = :category ORDER BY expenseDate DESC")
    fun getPengeluaranByCategory(category: ExpenseCategory): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE createdBy = :userId ORDER BY expenseDate DESC")
    fun getPengeluaranByUser(userId: Long): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE approvedBy = :userId ORDER BY expenseDate DESC")
    fun getPengeluaranApprovedBy(userId: Long): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate ORDER BY expenseDate DESC")
    fun getPengeluaranByDateRange(startDate: Date, endDate: Date): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE approvedBy IS NULL ORDER BY expenseDate DESC")
    fun getUnapprovedPengeluaran(): Flow<List<Pengeluaran>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPengeluaran(pengeluaran: Pengeluaran): Long

    @Update
    suspend fun updatePengeluaran(pengeluaran: Pengeluaran)

    @Query("DELETE FROM pengeluaran WHERE id = :id")
    suspend fun deletePengeluaranById(id: Long)

    @Query("SELECT COUNT(*) FROM pengeluaran")
    suspend fun getPengeluaranCount(): Int

    @Query("SELECT SUM(amount) FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpenseAmount(startDate: Date, endDate: Date): Double?
}