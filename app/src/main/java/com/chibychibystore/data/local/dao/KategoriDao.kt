package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Kategori
import kotlinx.coroutines.flow.Flow

@Dao
interface KategoriDao {
    @Query("SELECT * FROM kategori ORDER BY name ASC")
    fun getAllKategori(): Flow<List<Kategori>>

    @Query("SELECT * FROM kategori WHERE id = :id")
    suspend fun getKategoriById(id: Long): Kategori?

    @Query("SELECT * FROM kategori WHERE name = :name")
    suspend fun getKategoriByName(name: String): Kategori?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertKategori(kategori: Kategori): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertKategoriList(kategoriList: List<Kategori>): List<Long>

    @Update
    suspend fun updateKategori(kategori: Kategori)

    @Query("DELETE FROM kategori WHERE id = :id")
    suspend fun deleteKategoriById(id: Long)

    @Query("SELECT COUNT(*) FROM kategori")
    suspend fun getKategoriCount(): Int
}
