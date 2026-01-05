package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Gudang
import kotlinx.coroutines.flow.Flow

@Dao
interface GudangDao {
    @Query("SELECT * FROM gudang ORDER BY name ASC")
    fun getAllGudang(): Flow<List<Gudang>>

    @Query("SELECT * FROM gudang WHERE id = :id")
    suspend fun getGudangById(id: Long): Gudang?

    @Query("SELECT * FROM gudang WHERE name = :name")
    suspend fun getGudangByName(name: String): Gudang?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGudang(gudang: Gudang): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGudangList(gudangList: List<Gudang>): List<Long>

    @Update
    suspend fun updateGudang(gudang: Gudang)

    @Query("DELETE FROM gudang WHERE id = :id")
    suspend fun deleteGudangById(id: Long)

    @Query("SELECT COUNT(*) FROM gudang")
    suspend fun getGudangCount(): Int
}
