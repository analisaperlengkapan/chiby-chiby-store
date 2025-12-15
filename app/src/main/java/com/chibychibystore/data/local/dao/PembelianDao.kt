package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.local.entity.PembelianWithItems
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PembelianDao {
    @Query("SELECT * FROM pembelian ORDER BY purchaseDate DESC")
    fun getAllPembelian(): Flow<List<Pembelian>>

    @Query("SELECT * FROM pembelian WHERE id = :id")
    suspend fun getPembelianById(id: Long): Pembelian?

    @Query("SELECT * FROM pembelian WHERE supplierId = :supplierId ORDER BY purchaseDate DESC")
    fun getPembelianBySupplier(supplierId: Long): Flow<List<Pembelian>>

    @Query("SELECT * FROM pembelian WHERE purchaseDate BETWEEN :startDate AND :endDate ORDER BY purchaseDate DESC")
    fun getPembelianByDateRange(startDate: Date, endDate: Date): Flow<List<Pembelian>>

    @Query("SELECT * FROM pembelian WHERE purchaseDate BETWEEN :startDate AND :endDate ORDER BY purchaseDate DESC")
    suspend fun getPurchasesInDateRange(startDate: Date, endDate: Date): List<Pembelian>

    @Transaction
    @Query("SELECT * FROM pembelian WHERE id = :id")
    suspend fun getPembelianWithItems(id: Long): PembelianWithItems?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPembelian(pembelian: Pembelian): Long

    @Query("DELETE FROM pembelian WHERE id = :id")
    suspend fun deletePembelianById(id: Long)

    @Query("SELECT COUNT(*) FROM pembelian")
    suspend fun getPembelianCount(): Int

    @Query("SELECT SUM(totalAmount) FROM pembelian WHERE purchaseDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalPurchaseAmount(startDate: Date, endDate: Date): Double?
}