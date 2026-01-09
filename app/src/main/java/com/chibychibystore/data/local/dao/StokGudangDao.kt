package com.chibychibystore.data.local.dao

import androidx.room.*
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.StokGudang
import kotlinx.coroutines.flow.Flow

@Dao
interface StokGudangDao {
    @Query("SELECT * FROM stok_gudang WHERE productId = :productId AND warehouseId = :warehouseId")
    suspend fun getStock(productId: Long, warehouseId: Long): StokGudang?

    @Query("SELECT * FROM stok_gudang WHERE productId = :productId")
    fun getStocksByProduct(productId: Long): Flow<List<StokGudang>>

    @Query("SELECT * FROM stok_gudang WHERE warehouseId = :warehouseId")
    fun getStocksByWarehouse(warehouseId: Long): Flow<List<StokGudang>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStock(stokGudang: StokGudang)

    @Query("UPDATE stok_gudang SET quantity = :quantity WHERE productId = :productId AND warehouseId = :warehouseId")
    suspend fun updateStockQuantity(productId: Long, warehouseId: Long, quantity: Int)

    @Query("DELETE FROM stok_gudang WHERE productId = :productId AND warehouseId = :warehouseId")
    suspend fun deleteStock(productId: Long, warehouseId: Long)

    @Query("SELECT SUM(quantity) FROM stok_gudang WHERE productId = :productId")
    suspend fun getTotalStock(productId: Long): Int?

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
