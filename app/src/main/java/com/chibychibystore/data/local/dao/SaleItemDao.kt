package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chibychibystore.data.local.entity.SaleItem
import com.chibychibystore.data.model.TopProductDto
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM item_penjualan")
    fun getAllSaleItems(): kotlinx.coroutines.flow.Flow<List<SaleItem>>

    @Query("SELECT * FROM item_penjualan WHERE saleId = :saleId")
    fun getItemsBySaleId(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM item_penjualan WHERE productId = :productId ORDER BY id DESC")
    fun getItemsByProductId(productId: Long): Flow<List<SaleItem>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleItem(item: SaleItem): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleItemList(items: List<SaleItem>): List<Long>

    @Query("DELETE FROM item_penjualan WHERE saleId = :saleId")
    suspend fun deleteItemsBySaleId(saleId: Long)

    @Query("SELECT COUNT(*) FROM item_penjualan WHERE saleId = :saleId")
    suspend fun getItemCountBySaleId(saleId: Long): Int

    @Query("SELECT productId, SUM(quantity) as quantitySold, SUM(totalPrice) as totalRevenue FROM item_penjualan GROUP BY productId ORDER BY quantitySold DESC LIMIT :limit")
    suspend fun getTopSellingProducts(limit: Int): List<TopProductDto>

    @Query("""
        SELECT
            ip.productId,
            SUM(ip.quantity) as quantitySold,
            SUM(ip.totalPrice) as totalRevenue
        FROM item_penjualan ip
        JOIN penjualan p ON ip.saleId = p.id
        WHERE p.saleDate BETWEEN :startDate AND :endDate
        AND p.isRefunded = 0
        GROUP BY ip.productId
    """)
    suspend fun getProductSalesStats(startDate: java.util.Date, endDate: java.util.Date): List<TopProductDto>

    @Query("""
        SELECT SUM(ip.quantity * p.costPrice)
        FROM item_penjualan ip
        JOIN penjualan s ON ip.saleId = s.id
        JOIN produk p ON ip.productId = p.id
        WHERE s.saleDate BETWEEN :startDate AND :endDate
        AND s.isRefunded = 0
    """)
    suspend fun calculateTotalCogs(startDate: java.util.Date, endDate: java.util.Date): Double?


    
}
