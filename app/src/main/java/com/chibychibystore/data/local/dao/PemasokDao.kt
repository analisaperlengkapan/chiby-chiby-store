package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Pemasok
import kotlinx.coroutines.flow.Flow

@Dao
interface PemasokDao {
    @Query("SELECT * FROM pemasok ORDER BY name ASC")
    fun getAllPemasok(): Flow<List<Pemasok>>

    @Query("SELECT * FROM pemasok WHERE id = :id")
    suspend fun getPemasokById(id: Long): Pemasok?

    @Query("SELECT * FROM pemasok WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPemasok(query: String): Flow<List<Pemasok>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPemasok(pemasok: Pemasok): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPemasokList(pemasokList: List<Pemasok>): List<Long>

    @Update
    suspend fun updatePemasok(pemasok: Pemasok)

    @Query("DELETE FROM pemasok WHERE id = :id")
    suspend fun deletePemasokById(id: Long)

    @Query("SELECT COUNT(*) FROM pemasok")
    suspend fun getPemasokCount(): Int
}