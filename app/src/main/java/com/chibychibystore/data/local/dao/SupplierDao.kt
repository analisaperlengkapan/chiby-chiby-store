package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Supplier
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM pemasok ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM pemasok WHERE id = :id")
    suspend fun getSupplierById(id: Long): Supplier?

    @Query("SELECT * FROM pemasok WHERE name LIKE '%' || :query || '%'")
    fun searchSuppliers(query: String): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Query("DELETE FROM pemasok WHERE id = :id")
    suspend fun deleteSupplierById(id: Long)

    @Query("SELECT COUNT(*) FROM pemasok")
    suspend fun getSupplierCount(): Int
}
