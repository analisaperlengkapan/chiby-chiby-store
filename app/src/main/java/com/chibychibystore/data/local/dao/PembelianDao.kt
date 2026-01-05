package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.local.entity.PembelianWithItems
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PembelianDao {
    @Query("SELECT * FROM pembelian ORDER BY purchaseDate DESC")
    fun getAllPurchases(): Flow<List<Pembelian>>

    @Query("SELECT * FROM pembelian WHERE id = :id")
    suspend fun getPembelianById(id: Long): Pembelian?

    @Query("SELECT * FROM pembelian WHERE supplierId = :supplierId ORDER BY purchaseDate DESC")
    fun getPembelianByPemasok(supplierId: Long): Flow<List<Pembelian>>

    @Query("SELECT * FROM pembelian WHERE purchaseDate BETWEEN :startDate AND :endDate ORDER BY purchaseDate DESC")
    fun getPembelianByRentangTanggal(startDate: Date, endDate: Date): Flow<List<Pembelian>>

    @Transaction
    @Query("SELECT * FROM pembelian WHERE id = :id")
    fun getPembelianWithItems(id: Long): Flow<PembelianWithItems>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPembelian(pembelian: Pembelian): Long

    @Update
    suspend fun updatePembelian(pembelian: Pembelian)

    @Query("DELETE FROM pembelian WHERE id = :id")
    suspend fun deletePembelianById(id: Long)

    @Query("SELECT COUNT(*) FROM pembelian")
    suspend fun getPembelianCount(): Int

    @Query("SELECT COUNT(*) FROM pembelian WHERE supplierId = :supplierId")
    suspend fun countPembelianByPemasok(supplierId: Long): Int
}
