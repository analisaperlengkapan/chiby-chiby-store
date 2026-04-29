package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.local.entity.PaymentMethod
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PenjualanDao {
    @Query("SELECT * FROM penjualan ORDER BY saleDate DESC")
    fun getAllPenjualan(): Flow<List<Penjualan>>

    @Transaction
    @Query("SELECT * FROM penjualan ORDER BY saleDate DESC")
    fun getAllPenjualanWithItems(): Flow<List<PenjualanWithItems>>

    @Query("SELECT * FROM penjualan WHERE id = :id")
    suspend fun getPenjualanById(id: Long): Penjualan?

    @Query("SELECT * FROM penjualan WHERE cashierId = :cashierId ORDER BY saleDate DESC")
    fun getPenjualanByKasir(cashierId: Long): Flow<List<Penjualan>>

    @Query("SELECT * FROM penjualan WHERE paymentMethod = :paymentMethod ORDER BY saleDate DESC")
    fun getPenjualanByMetodePembayaran(paymentMethod: PaymentMethod): Flow<List<Penjualan>>

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
    fun observePenjualanFiltered(startDate: Date, endDate: Date, query: String?): Flow<List<Penjualan>>

    @Query("SELECT * FROM penjualan WHERE CAST(id AS TEXT) LIKE '%' || :query || '%' ORDER BY saleDate DESC")
    fun searchPenjualan(query: String): Flow<List<Penjualan>>

    @Query("""
        SELECT * FROM penjualan
        WHERE saleDate BETWEEN :startDate AND :endDate
        ORDER BY saleDate DESC
    """)
    fun getPenjualanByRentangTanggal(startDate: Date, endDate: Date): Flow<List<Penjualan>>

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
    suspend fun getPenjualanFiltered(startDate: Date, endDate: Date, cashierId: Long?, query: String?): List<Penjualan>

    @Query("SELECT * FROM penjualan ORDER BY saleDate DESC LIMIT :limit")
    fun getRecentPenjualan(limit: Int): Flow<List<Penjualan>>

    @Transaction
    @Query("SELECT * FROM penjualan WHERE id = :id")
    suspend fun getPenjualanWithItems(id: Long): PenjualanWithItems?

    @Transaction
    @Query("SELECT * FROM penjualan WHERE saleDate BETWEEN :startDate AND :endDate ORDER BY saleDate DESC")
    fun getPenjualanWithItemsByRentangTanggal(startDate: Date, endDate: Date): Flow<List<PenjualanWithItems>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPenjualan(penjualan: Penjualan): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPenjualanList(penjualan: List<Penjualan>): List<Long>

    @Update
    suspend fun updatePenjualan(penjualan: Penjualan)

    @Query("DELETE FROM penjualan WHERE id = :id")
    suspend fun deletePenjualanById(id: Long)

    @Query("SELECT COUNT(*) FROM penjualan")
    suspend fun getPenjualanCount(): Int

    @Query("SELECT COUNT(*) FROM penjualan WHERE saleDate BETWEEN :startDate AND :endDate")
    suspend fun getPenjualanCountByDateRange(startDate: Date, endDate: Date): Int

    @Query("SELECT COUNT(*) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getPenjualanCountNonRefunded(startDate: Date, endDate: Date): Int

    // Aggregate semantics: as of MIGRATION_11_12, `totalAmount` stores the
    // post-tax/discount amount actually paid by the customer (subtotal + tax -
    // discount, floored at 0). Aggregations below reflect this:
    //   - getTotalPenjualanAmount / getTotalCashReceipts: gross cash flow (incl. tax)
    //   - getTotalRevenue: net revenue (subtotal − discount, tax-exclusive)
    // Pre-MIGRATION_11_12 rows stored the raw subtotal; the migration backfills
    // them, so callers don't need to special-case legacy data on a migrated DB.
    @Query("SELECT SUM(totalAmount) FROM penjualan WHERE saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalPenjualanAmount(startDate: Date, endDate: Date): Double?

    // Per-row MAX(0, totalAmount - tax) mirrors the floor applied in MIGRATION_11_12:
    // when a legacy row had `discount > subtotal + tax`, the migration set totalAmount=0
    // while leaving `tax` unchanged. A naive `SUM(totalAmount - tax)` would then contribute
    // a negative `-tax` for those rows and under-report total revenue. The MAX(0, …) clamp
    // matches the conceptual contract of the column (the sale's value can't be negative)
    // and produces a 0 contribution for those edge-case rows, which is the correct net
    // revenue when the customer effectively paid nothing.
    @Query("SELECT SUM(MAX(0, totalAmount - tax)) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalRevenue(startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(tax) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalTax(startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(discount) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalDiscount(startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(totalAmount) FROM penjualan WHERE isRefunded = 0 AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalCashReceipts(startDate: Date, endDate: Date): Double?

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM penjualan WHERE isRefunded = 0 AND shiftId = :shiftId")
    suspend fun getTotalSalesByShift(shiftId: Long): Double

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM penjualan WHERE isRefunded = 0 AND shiftId = :shiftId AND paymentMethod = :paymentMethod")
    suspend fun getTotalSalesByShiftAndPaymentMethod(shiftId: Long, paymentMethod: PaymentMethod): Double
}
