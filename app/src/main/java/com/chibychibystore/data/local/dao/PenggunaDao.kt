package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.local.entity.Pengguna
import kotlinx.coroutines.flow.Flow

@Dao
interface PenggunaDao {
    @Query("SELECT * FROM pengguna ORDER BY username ASC")
    fun getAllPengguna(): Flow<List<Pengguna>>

    @Query("SELECT * FROM pengguna WHERE id = :id")
    suspend fun getPenggunaById(id: Long): Pengguna?

    @Query("SELECT * FROM pengguna WHERE username = :username")
    suspend fun getPenggunaByUsername(username: String): Pengguna?

    @Query("SELECT * FROM pengguna WHERE role = :role ORDER BY username ASC")
    fun getPenggunaByRole(role: Role): Flow<List<Pengguna>>

    @Query("SELECT * FROM pengguna WHERE username LIKE '%' || :query || '%' ORDER BY username ASC")
    fun searchPengguna(query: String): Flow<List<Pengguna>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPengguna(pengguna: Pengguna): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPenggunaList(penggunaList: List<Pengguna>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPenggunaListIgnoreConflict(penggunaList: List<Pengguna>): List<Long>

    @Update
    suspend fun updatePengguna(pengguna: Pengguna)

    @Query("DELETE FROM pengguna WHERE id = :id")
    suspend fun deletePenggunaById(id: Long)

    @Query("SELECT COUNT(*) FROM pengguna")
    suspend fun getPenggunaCount(): Int

    @Query("SELECT COUNT(*) FROM pengguna WHERE isActive = 1")
    suspend fun countActiveUsers(): Int

    @Query("SELECT COUNT(*) FROM pengguna WHERE role = :role")
    suspend fun countByRole(role: Role): Int
}
