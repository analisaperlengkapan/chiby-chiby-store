package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.chibychibystore.data.local.entity.Sale
import com.chibychibystore.data.local.entity.SaleWithItems
import com.chibychibystore.data.local.entity.PaymentMethod
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SaleDao {
    @Query("SELECT * FROM penjualan ORDER BY saleDate DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM penjualan WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM penjualan WHERE cashierId = :cashierId ORDER BY saleDate DESC")
    fun getSaleByCashier(cashierId: Long): Flow<List<Sale>>

    @Query("SELECT * FROM penjualan WHERE paymentMethod = :paymentMethod ORDER BY saleDate DESC")
    fun getSaleByPaymentMethod(paymentMethod: PaymentMethod): Flow<List<Sale>>

    @Query("""
        SELECT * FROM penjualan
        WHERE saleDate BETWEEN :startDate AND :endDate
        AND (:query IS NULL OR (
            CAST(id AS TEXT) LIKE '%' || :query || '%' OR
            paymentMethod LIKE '%' || :query || '%' OR
            CAST(totalAmount AS TEXT) LIKE '%' || :query || '%'
        ))
        ORDER BY saleDate DESC
    """)
    fun observeSalesFiltered(startDate: Date, endDate: Date, query: String?): Flow<List<Sale>>

    @Query("SELECT * FROM penjualan WHERE CAST(id AS TEXT) LIKE '%' || :query || '%' ORDER BY saleDate DESC")
    fun searchSales(query: String): Flow<List<Sale>>

    @Query("""
        SELECT * FROM penjualan
        WHERE saleDate BETWEEN :startDate AND :endDate
        ORDER BY saleDate DESC
    """)
    fun getSalesByDateRange(startDate: Date, endDate: Date): Flow<List<Sale>>

    @Query("""
        SELECT * FROM penjualan
        WHERE saleDate BETWEEN :startDate AND :endDate
        AND (:cashierId IS NULL OR cashierId = :cashierId)
        AND (:query IS NULL OR (
            CAST(id AS TEXT) LIKE '%' || :query || '%' OR
            paymentMethod LIKE '%' || :query || '%' OR
            CAST(totalAmount AS TEXT) LIKE '%' || :query || '%'
        ))
        ORDER BY saleDate DESC
    """)
    suspend fun getSalesFiltered(startDate: Date, endDate: Date, cashierId: Long?, query: String?): List<Sale>

    @Query("SELECT * FROM penjualan ORDER BY saleDate DESC LIMIT :limit")
    fun getRecentSales(limit: Int): Flow<List<Sale>>

    @Transaction
    @Query("SELECT * FROM penjualan WHERE id = :id")
    suspend fun getSaleWithItems(id: Long): SaleWithItems?

    @Transaction
    @Query("SELECT * FROM penjualan WHERE saleDate BETWEEN :startDate AND :endDate ORDER BY saleDate DESC")
    fun getSaleWithItemsByDateRange(startDate: Date, endDate: Date): Flow<List<SaleWithItems>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(sale: Sale): Long

    @Update
    suspend fun updateSale(sale: Sale)

    @Query("DELETE FROM penjualan WHERE id = :id")
    suspend fun deleteSaleById(id: Long)

    @Query("SELECT COUNT(*) FROM penjualan")
    suspend fun getSaleCount(): Int

    @Query("SELECT COUNT(*) FROM penjualan WHERE saleDate BETWEEN :startDate AND :endDate")
    suspend fun getSaleCountByDateRange(startDate: Date, endDate: Date): Int

    @Query("SELECT COUNT(*) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getSaleCountNonRefunded(startDate: Date, endDate: Date): Int

    @Query("SELECT SUM(totalAmount) FROM penjualan WHERE saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalSalesAmount(startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(totalAmount - tax) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalRevenue(startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(tax) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalTax(startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(discount) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalDiscount(startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(totalAmount) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalCashReceipts(startDate: Date, endDate: Date): Double?
}
