package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Warehouse
import com.chibychibystore.data.local.entity.Product
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk Warehouse Service
 */
interface WarehouseService {
    /**
     * Membuat gudang baru
     */
    suspend fun createWarehouse(warehouse: Warehouse): Result<Warehouse>

    /**
     * Update gudang existing
     */
    suspend fun updateWarehouse(warehouse: Warehouse): Result<Warehouse>

    /**
     * Hapus gudang
     */
    suspend fun deleteWarehouse(id: Long): Result<Unit>

    /**
     * Get gudang by ID
     */
    suspend fun getWarehouse(id: Long): Result<Warehouse?>

    /**
     * Get semua gudang
     */
    suspend fun getWarehouses(): Result<List<Warehouse>>

    /**
     * Assign product ke gudang tertentu
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
     * Get stok product per gudang
     */
    suspend fun getWarehouseStock(warehouseId: Long): Result<List<Product>>

    /**
     * Get semua product di semua gudang
     */
    suspend fun getAllWarehouseStock(): Result<Map<Warehouse, List<Product>>>

    /**
     * Observable untuk semua gudang
     */
    fun observeWarehouses(): Flow<List<Warehouse>>

    /**
     * Observable untuk stok gudang tertentu
     */
    fun observeWarehouseStock(warehouseId: Long): Flow<List<Product>>
}
