package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.local.entity.PaymentMethod
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PenjualanDao {
    @Query("SELECT * FROM penjualan ORDER BY saleDate DESC")
    fun getAllPenjualan(): Flow<List<Penjualan>>

    @Query("SELECT * FROM penjualan WHERE id = :id")
    suspend fun getPenjualanById(id: Long): Penjualan?

    @Query("SELECT * FROM penjualan WHERE cashierId = :cashierId ORDER BY saleDate DESC")
    fun getPenjualanByCashier(cashierId: Long): Flow<List<Penjualan>>

    @Query("SELECT * FROM penjualan WHERE paymentMethod = :paymentMethod ORDER BY saleDate DESC")
    fun getPenjualanByPaymentMethod(paymentMethod: PaymentMethod): Flow<List<Penjualan>>

    @Query("SELECT * FROM penjualan WHERE saleDate BETWEEN :startDate AND :endDate ORDER BY saleDate DESC")
    fun getPenjualanByDateRange(startDate: Date, endDate: Date): Flow<List<Penjualan>>

    @Transaction
    @Query("SELECT * FROM penjualan WHERE id = :id")
    suspend fun getPenjualanWithItems(id: Long): PenjualanWithItems?

    @Transaction
    @Query("SELECT * FROM penjualan WHERE saleDate BETWEEN :startDate AND :endDate ORDER BY saleDate DESC")
    fun getPenjualanWithItemsByDateRange(startDate: Date, endDate: Date): Flow<List<PenjualanWithItems>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPenjualan(penjualan: Penjualan): Long

    @androidx.room.Update
    suspend fun updatePenjualan(penjualan: Penjualan)

    @Query("DELETE FROM penjualan WHERE id = :id")
    suspend fun deletePenjualanById(id: Long)

    @Query("SELECT COUNT(*) FROM penjualan")
    suspend fun getPenjualanCount(): Int

    @Query("SELECT SUM(totalAmount) FROM penjualan WHERE saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalSalesAmount(startDate: Date, endDate: Date): Double?
}