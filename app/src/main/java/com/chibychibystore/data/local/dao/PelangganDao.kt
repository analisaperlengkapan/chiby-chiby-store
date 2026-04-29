package com.chibychibystore.data.local.dao

import androidx.room.*
import com.chibychibystore.data.local.entity.Pelanggan
import kotlinx.coroutines.flow.Flow

@Dao
interface PelangganDao {
    @Query("SELECT * FROM pelanggan ORDER BY name ASC")
    fun getAllPelanggan(): Flow<List<Pelanggan>>

    @Query("SELECT * FROM pelanggan WHERE id = :id")
    suspend fun getPelangganById(id: Long): Pelanggan?

    @Query("SELECT * FROM pelanggan WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchPelanggan(query: String): Flow<List<Pelanggan>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPelanggan(pelanggan: Pelanggan): Long

    @Update
    suspend fun updatePelanggan(pelanggan: Pelanggan)

    @Delete
    suspend fun deletePelanggan(pelanggan: Pelanggan)
}
