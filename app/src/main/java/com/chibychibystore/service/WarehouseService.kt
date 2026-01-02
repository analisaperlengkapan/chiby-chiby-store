package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk Warehouse Service
 */
interface WarehouseService {
    /**
     * Membuat gudang baru
     */
    suspend fun createWarehouse(warehouse: Gudang): Result<Gudang>

    /**
     * Update gudang existing
     */
    suspend fun updateWarehouse(warehouse: Gudang): Result<Gudang>

    /**
     * Hapus gudang
     */
    suspend fun deleteWarehouse(id: Long): Result<Unit>

    /**
     * Get gudang by ID
     */
    suspend fun getWarehouse(id: Long): Result<Gudang?>

    /**
     * Get semua gudang
     */
    suspend fun getWarehouses(): Result<List<Gudang>>

    /**
     * Assign produk ke gudang tertentu
     */
    suspend fun assignProductToWarehouse(productId: Long, warehouseId: Long): Result<Unit>

    /**
     * Transfer stok antar gudang
     */
    suspend fun transferStock(
        productId: Long,
        fromWarehouseId: Long,
        toWarehouseId: Long,
        quantity: Int
    ): Result<Unit>

    /**
     * Get stok produk per gudang
     */
    suspend fun getWarehouseStock(warehouseId: Long): Result<List<Produk>>

    /**
     * Get semua produk di semua gudang
     */
    suspend fun getAllWarehouseStock(): Result<Map<Gudang, List<Produk>>>

    /**
     * Observable untuk semua gudang
     */
    fun observeWarehouses(): Flow<List<Gudang>>

    /**
     * Observable untuk stok gudang tertentu
     */
    fun observeWarehouseStock(warehouseId: Long): Flow<List<Produk>>
}
