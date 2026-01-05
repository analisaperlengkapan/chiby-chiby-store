package com.chibychibystore.data.local.dao

import androidx.room.*
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PengeluaranDao {
    @Query("SELECT * FROM pengeluaran ORDER BY expenseDate DESC")
    fun getAllPengeluarans(): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE id = :id")
    suspend fun getPengeluaranById(id: Long): Pengeluaran?

    @Query("SELECT * FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate ORDER BY expenseDate DESC")
    fun getPengeluaransByDateRange(startDate: Date, endDate: Date): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate ORDER BY expenseDate DESC")
    suspend fun getPengeluaransByDateRangeList(startDate: Date, endDate: Date): List<Pengeluaran>

    @Query("SELECT * FROM pengeluaran WHERE category = :category ORDER BY expenseDate DESC")
    fun getPengeluaransByKategori(category: KategoriPengeluaran): Flow<List<Pengeluaran>>

    @Query("SELECT * FROM pengeluaran WHERE category = :category AND expenseDate BETWEEN :startDate AND :endDate ORDER BY expenseDate DESC")
    suspend fun getPengeluaransByDateRangeAndKategori(startDate: Date, endDate: Date, category: KategoriPengeluaran): List<Pengeluaran>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPengeluaran(pengeluaran: Pengeluaran): Long

    @Update
    suspend fun updatePengeluaran(pengeluaran: Pengeluaran)

    @Query("DELETE FROM pengeluaran WHERE id = :id")
    suspend fun deletePengeluaranById(id: Long)

    @Query("SELECT SUM(amount) FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalPengeluaranAmount(startDate: Date, endDate: Date): Double?

    @Query("UPDATE pengeluaran SET approvedBy = :approverId WHERE id = :id")
    suspend fun approvePengeluaran(id: Long, approverId: Long)

    @Query("SELECT category, SUM(amount) as total FROM pengeluaran WHERE expenseDate BETWEEN :startDate AND :endDate GROUP BY category")
    suspend fun getRingkasanPengeluaranPerKategori(
        startDate: Date, 
        endDate: Date
    ): Map<@MapColumn(columnName = "category") KategoriPengeluaran, @MapColumn(columnName = "total") Double>

    @Query("SELECT * FROM pengeluaran WHERE category IN (:categories) AND expenseDate BETWEEN :startDate AND :endDate ORDER BY expenseDate DESC")
    suspend fun getPengeluaransByCategoriesAndDateRange(
        categories: List<KategoriPengeluaran>,
        startDate: Date,
        endDate: Date
    ): List<Pengeluaran>
}
