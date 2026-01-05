package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.chibychibystore.data.local.entity.Purchase
import com.chibychibystore.data.local.entity.PurchaseWithItems
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM pembelian ORDER BY purchaseDate DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM pembelian WHERE id = :id")
    suspend fun getPurchaseById(id: Long): Purchase?

    @Query("SELECT * FROM pembelian WHERE supplierId = :supplierId ORDER BY purchaseDate DESC")
    fun getPurchasesBySupplier(supplierId: Long): Flow<List<Purchase>>

    @Query("SELECT * FROM pembelian WHERE purchaseDate BETWEEN :startDate AND :endDate ORDER BY purchaseDate DESC")
    fun getPurchasesByDateRange(startDate: Date, endDate: Date): Flow<List<Purchase>>

    @Transaction
    @Query("SELECT * FROM pembelian WHERE id = :id")
    fun getPurchaseWithItems(id: Long): Flow<PurchaseWithItems>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchase(purchase: Purchase): Long

    @Update
    suspend fun updatePurchase(purchase: Purchase)

    @Query("DELETE FROM pembelian WHERE id = :id")
    suspend fun deletePurchaseById(id: Long)

    @Query("SELECT COUNT(*) FROM pembelian")
    suspend fun getPurchaseCount(): Int
}
