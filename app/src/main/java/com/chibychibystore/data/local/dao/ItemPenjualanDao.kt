package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.model.ProdukTerpopulerDto
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemPenjualanDao {
    @Query("SELECT * FROM item_penjualan WHERE saleId = :saleId")
    fun getItemsByPenjualanId(saleId: Long): Flow<List<ItemPenjualan>>

    @Query("SELECT * FROM item_penjualan WHERE productId = :productId ORDER BY id DESC")
    fun getItemsByProdukId(productId: Long): Flow<List<ItemPenjualan>>

    @Query("SELECT * FROM item_penjualan")
    fun getAllSaleItems(): Flow<List<ItemPenjualan>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemPenjualan(item: ItemPenjualan): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemPenjualanList(items: List<ItemPenjualan>): List<Long>

    @Query("DELETE FROM item_penjualan WHERE saleId = :saleId")
    suspend fun deleteItemsByPenjualanId(saleId: Long)

    @Query("SELECT COUNT(*) FROM item_penjualan WHERE saleId = :saleId")
    suspend fun getItemCountByPenjualanId(saleId: Long): Int

    @Query("SELECT productId as produkId, SUM(quantity) as jumlahTerjual, SUM(totalPrice) as totalPendapatan FROM item_penjualan GROUP BY productId ORDER BY jumlahTerjual DESC LIMIT :limit")
    suspend fun getProdukTerpopuler(limit: Int): List<ProdukTerpopulerDto>

    @Query("""
        SELECT
            ip.productId as produkId,
            SUM(ip.quantity) as jumlahTerjual,
            SUM(ip.totalPrice) as totalPendapatan
        FROM item_penjualan ip
        JOIN penjualan p ON ip.saleId = p.id
        WHERE p.saleDate BETWEEN :startDate AND :endDate
        AND p.isRefunded = 0
        GROUP BY ip.productId
    """)
    suspend fun getProdukPenjualanStats(startDate: java.util.Date, endDate: java.util.Date): List<ProdukTerpopulerDto>

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
