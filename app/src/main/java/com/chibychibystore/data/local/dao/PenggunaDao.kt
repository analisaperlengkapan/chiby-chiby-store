package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import kotlinx.coroutines.flow.Flow

@Dao
interface PenggunaDao {
    @Query("SELECT * FROM pengguna")
    fun getAllPengguna(): Flow<List<Pengguna>>

    @Query("SELECT * FROM pengguna WHERE id = :id")
    suspend fun getPenggunaById(id: Long): Pengguna?

    @Query("SELECT * FROM pengguna WHERE username = :username")
    suspend fun getPenggunaByUsername(username: String): Pengguna?

    @Query("SELECT * FROM pengguna WHERE role = :role")
    fun getPenggunaByRole(role: Role): Flow<List<Pengguna>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPengguna(pengguna: Pengguna): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPenggunaList(penggunaList: List<Pengguna>): List<Long>

    @Update
    suspend fun updatePengguna(pengguna: Pengguna)

    @Query("DELETE FROM pengguna WHERE id = :id")
    suspend fun deletePenggunaById(id: Long)

    @Query("SELECT COUNT(*) FROM pengguna")
    suspend fun getPenggunaCount(): Int
}