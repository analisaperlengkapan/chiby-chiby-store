package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Product Service untuk manajemen product di Chiby Chiby Store
 */
interface ProductService {
    suspend fun createProduk(produk: Produk): Result<Produk>
    suspend fun updateProduk(produk: Produk): Result<Produk>
    suspend fun deleteProduk(id: String): Result<Unit>
    suspend fun getProduk(id: String): Result<Produk?>
    suspend fun getProduks(
        categoryId: String? = null,
        warehouseId: String? = null,
        searchQuery: String? = null
    ): Result<List<Produk>>
    suspend fun searchProduks(query: String): Result<List<Produk>>
    suspend fun updateStock(productId: String, newStock: Int): Result<Unit>
    suspend fun getLowStockProduks(): Result<List<Produk>>
    fun observeProduks(): Flow<List<Produk>>
    fun observeProduksByKategori(categoryId: String): Flow<List<Produk>>
    fun observeProduksByGudang(warehouseId: String): Flow<List<Produk>>
    fun observeSearchProduks(query: String): Flow<List<Produk>>
    fun observeLowStockProduks(): Flow<List<Produk>>
    suspend fun getProductByBarcode(barcode: String): Result<Produk?>
    suspend fun getProductsByIds(ids: List<Long>): Result<List<Produk>>
}
