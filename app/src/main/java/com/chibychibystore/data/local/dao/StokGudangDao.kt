package com.chibychibystore.data.local.dao

import androidx.room.*
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.StokGudang
import com.chibychibystore.data.model.StockAdjustment
import kotlinx.coroutines.flow.Flow

@Dao
interface StokGudangDao {
    @Query("SELECT * FROM stok_gudang WHERE productId = :productId AND warehouseId = :warehouseId")
    suspend fun getStock(productId: Long, warehouseId: Long): StokGudang?

    @Query("SELECT * FROM stok_gudang WHERE productId IN (:productIds) AND warehouseId = :warehouseId")
    suspend fun getStocks(productIds: List<Long>, warehouseId: Long): List<StokGudang>

    @Query("SELECT * FROM stok_gudang WHERE productId = :productId")
    fun getStocksByProduct(productId: Long): Flow<List<StokGudang>>

    @Query("SELECT * FROM stok_gudang WHERE warehouseId = :warehouseId")
    fun getStocksByWarehouse(warehouseId: Long): Flow<List<StokGudang>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStock(stokGudang: StokGudang)

    @Query("UPDATE stok_gudang SET quantity = :quantity WHERE productId = :productId AND warehouseId = :warehouseId")
    suspend fun updateStockQuantity(productId: Long, warehouseId: Long, quantity: Int)

    @Query("UPDATE stok_gudang SET quantity = quantity + :delta WHERE productId = :productId AND warehouseId = :warehouseId")
    suspend fun adjustStock(productId: Long, warehouseId: Long, delta: Int)

    @Transaction
    suspend fun adjustStockBatch(adjustments: List<StockAdjustment>) {
        for (adj in adjustments) {
            adjustStock(adj.productId, adj.warehouseId, adj.delta)
        }
    }

    @Query("DELETE FROM stok_gudang WHERE productId = :productId AND warehouseId = :warehouseId")
    suspend fun deleteStock(productId: Long, warehouseId: Long)

    @Query("SELECT SUM(quantity) FROM stok_gudang WHERE productId = :productId")
    suspend fun getTotalStock(productId: Long): Int?

    // Snapshot of every per-warehouse stock row, used by the backup pipeline so
    // that warehouse-level inventory survives a backup/restore cycle. Without
    // this, restoring a backup leaves `stok_gudang` empty and `Produk.stockQuantity`
    // (which IS restored) becomes inconsistent with the per-warehouse breakdown
    // that purchases / sales / audits all rely on.
    @Query("SELECT * FROM stok_gudang")
    suspend fun getAllStocks(): List<StokGudang>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StokGudang>)

    // Return Produk but with stockQuantity mapped from StokGudang
    @Query("""
        SELECT p.id, p.name, p.barcode, p.categoryId, p.costPrice, p.sellingPrice,
               s.quantity as stockQuantity,
               p.warehouseId, p.minStock, p.createdAt, p.updatedAt
        FROM produk p
        INNER JOIN stok_gudang s ON p.id = s.productId
        WHERE s.warehouseId = :warehouseId
    """)
    fun getProductsByWarehouse(warehouseId: Long): Flow<List<Produk>>
}
