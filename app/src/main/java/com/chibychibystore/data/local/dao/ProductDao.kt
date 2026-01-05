package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM produk ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM produk WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM produk WHERE id IN (:ids)")
    suspend fun getProductByIds(ids: List<Long>): List<Product>

    @Query("SELECT * FROM produk WHERE barcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM produk WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getProductByCategory(categoryId: Long): Flow<List<Product>>

    @Query("SELECT * FROM produk WHERE warehouseId = :warehouseId ORDER BY name ASC")
    fun getProductByWarehouse(warehouseId: Long): Flow<List<Product>>

    @Query("SELECT * FROM produk WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM produk WHERE stockQuantity <= minStock AND stockQuantity > 0 ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM produk WHERE stockQuantity = 0 ORDER BY name ASC")
    fun getOutOfStockProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProductList(productList: List<Product>): List<Long>

    @Update
    suspend fun updateProduct(product: Product)

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
    suspend fun deleteProductById(id: Long)

    @Query("SELECT COUNT(*) FROM produk")
    suspend fun getProductCount(): Int

    @Query("SELECT SUM(stockQuantity) FROM produk")
    suspend fun getTotalStock(): Int?

    @Query("SELECT COUNT(*) FROM produk WHERE stockQuantity <= minStock AND stockQuantity > 0")
    suspend fun countLowStock(): Int

    @Query("SELECT COUNT(*) FROM produk WHERE stockQuantity = 0")
    suspend fun countOutOfStock(): Int

    @Query("SELECT SUM(stockQuantity * costPrice) FROM produk")
    suspend fun getTotalInventoryValue(): Double?
}
