package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Produk
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdukDao {
    @Query("SELECT * FROM produk ORDER BY name ASC")
    fun getAllProduk(): Flow<List<Produk>>

    @Query("SELECT * FROM produk WHERE id = :id")
    suspend fun getProdukById(id: Long): Produk?

    @Query("SELECT * FROM produk WHERE id IN (:ids)")
    suspend fun getProdukByIds(ids: List<Long>): List<Produk>

    @Query("SELECT * FROM produk WHERE barcode = :barcode")
    suspend fun getProdukByBarcode(barcode: String): Produk?

    @Query("SELECT * FROM produk WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getProdukByCategory(categoryId: Long): Flow<List<Produk>>

    @Query("SELECT * FROM produk WHERE warehouseId = :warehouseId ORDER BY name ASC")
    fun getProdukByWarehouse(warehouseId: Long): Flow<List<Produk>>

    @Query("SELECT * FROM produk WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProduk(query: String): Flow<List<Produk>>

    @Query("SELECT * FROM produk WHERE stockQuantity <= minStock AND stockQuantity > 0 ORDER BY stockQuantity ASC")
    fun getLowStockProduk(): Flow<List<Produk>>

    @Query("SELECT * FROM produk WHERE stockQuantity = 0 ORDER BY name ASC")
    fun getOutOfStockProduk(): Flow<List<Produk>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduk(produk: Produk): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProdukList(produkList: List<Produk>): List<Long>

    @Update
    suspend fun updateProduk(produk: Produk)

    /**
     * Adjust stock by adding/subtracting quantity (Delta)
     */
    @Query("UPDATE produk SET stockQuantity = stockQuantity + :quantity WHERE id = :id")
    suspend fun adjustStock(id: Long, quantity: Int)

    /**
     * Set stock to absolute value
     */
    @Query("UPDATE produk SET stockQuantity = :quantity WHERE id = :id")
    suspend fun setStock(id: Long, quantity: Int)

    @Query("DELETE FROM produk WHERE id = :id")
    suspend fun deleteProdukById(id: Long)

    @Query("SELECT COUNT(*) FROM produk")
    suspend fun getProdukCount(): Int

    @Query("SELECT SUM(stockQuantity) FROM produk")
    suspend fun getTotalStock(): Int?

    @Query("SELECT COUNT(*) FROM produk WHERE stockQuantity <= minStock AND stockQuantity > 0")
    suspend fun countLowStock(): Int

    @Query("SELECT COUNT(*) FROM produk WHERE stockQuantity = 0")
    suspend fun countOutOfStock(): Int

    @Query("SELECT SUM(stockQuantity * costPrice) FROM produk")
    suspend fun getTotalInventoryValue(): Double?
}