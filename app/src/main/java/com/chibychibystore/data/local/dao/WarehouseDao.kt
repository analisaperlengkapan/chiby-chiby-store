package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Warehouse
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM gudang ORDER BY name ASC")
    fun getAllWarehouses(): Flow<List<Warehouse>>

    @Query("SELECT * FROM gudang WHERE id = :id")
    suspend fun getWarehouseById(id: Long): Warehouse?

    @Query("SELECT * FROM gudang WHERE name = :name")
    suspend fun getWarehouseByName(name: String): Warehouse?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWarehouse(warehouse: Warehouse): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWarehouseList(warehouseList: List<Warehouse>): List<Long>

    @Update
    suspend fun updateWarehouse(warehouse: Warehouse)

    @Query("DELETE FROM gudang WHERE id = :id")
    suspend fun deleteWarehouseById(id: Long)

    @Query("SELECT COUNT(*) FROM gudang")
    suspend fun getWarehouseCount(): Int
}
