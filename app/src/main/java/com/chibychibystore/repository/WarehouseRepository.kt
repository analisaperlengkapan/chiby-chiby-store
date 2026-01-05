package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.WarehouseDao
import com.chibychibystore.data.local.entity.Warehouse
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Gudang
 */
@Singleton
class WarehouseRepository @Inject constructor(
    private val warehouseDao: WarehouseDao
) {

    /**
     * Get semua gudang
     */
    fun getAllWarehouses(): Flow<List<Warehouse>> = warehouseDao.getAllWarehouses()

    /**
     * Get gudang by ID
     */
    suspend fun getWarehouseById(id: Long): Result<Warehouse> {
        return try {
            val warehouse = warehouseDao.getWarehouseById(id)
            if (warehouse != null) {
                Result.success(warehouse)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Warehouse with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getWarehouseById", e))
        }
    }

    /**
     * Get gudang by name
     */
    suspend fun getWarehouseByName(name: String): Result<Warehouse> {
        return try {
            val warehouse = warehouseDao.getWarehouseByName(name)
            if (warehouse != null) {
                Result.success(warehouse)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Warehouse with name $name not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getWarehouseByName", e))
        }
    }

    /**
     * Create gudang baru
     */
    suspend fun createWarehouse(warehouse: Warehouse): Result<Long> {
        return try {
            // Validasi input
            validateWarehouseData(warehouse)

            // Check if name already exists
            val existingWarehouse = warehouseDao.getWarehouseByName(warehouse.name)
            if (existingWarehouse != null) {
                return Result.failure(ChibyChibyException.ValidationError("name", "Warehouse name already taken"))
            }

            val id = warehouseDao.insertWarehouse(warehouse)
            Result.success(id)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createWarehouse", e))
        }
    }

    /**
     * Update gudang
     */
    suspend fun updateWarehouse(warehouse: Warehouse): Result<Unit> {
        return try {
            // Validasi input
            validateWarehouseData(warehouse)

            // Check if warehouse exists
            warehouseDao.getWarehouseById(warehouse.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Warehouse not found"))

            // Check name uniqueness (exclude current warehouse)
            val warehouseWithSameName = warehouseDao.getWarehouseByName(warehouse.name)
            if (warehouseWithSameName != null && warehouseWithSameName.id != warehouse.id) {
                return Result.failure(ChibyChibyException.ValidationError("name", "Warehouse name already taken"))
            }

            warehouseDao.updateWarehouse(warehouse)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateWarehouse", e))
        }
    }

    /**
     * Delete gudang
     */
    suspend fun deleteWarehouse(id: Long): Result<Unit> {
        return try {
            // Check if warehouse exists
            val warehouse = warehouseDao.getWarehouseById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Warehouse not found"))

            warehouseDao.deleteWarehouseById(id)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteWarehouse", e))
        }
    }

    /**
     * Get jumlah total gudang
     */
    suspend fun getWarehouseCount(): Result<Int> {
        return try {
            val count = warehouseDao.getWarehouseCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getWarehouseCount", e))
        }
    }

    private fun validateWarehouseData(warehouse: Warehouse) {
        if (warehouse.name.isBlank()) {
            throw ChibyChibyException.ValidationError("name", "Warehouse name cannot be empty")
        }
        if (warehouse.name.length < 2) {
            throw ChibyChibyException.ValidationError("name", "Warehouse name must be at least 2 characters")
        }
        if (warehouse.capacity < 0) {
            throw ChibyChibyException.ValidationError("capacity", "Warehouse capacity cannot be negative")
        }
    }
}
