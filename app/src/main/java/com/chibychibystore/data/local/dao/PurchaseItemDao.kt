package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.PurchaseItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseItemDao {
    @Query("SELECT * FROM item_pembelian WHERE purchaseId = :purchaseId")
    fun getItemsByPurchaseId(purchaseId: Long): Flow<List<PurchaseItem>>

    @Query("SELECT * FROM item_pembelian")
    suspend fun getAllPurchaseItems(): List<PurchaseItem>

    @Query("SELECT * FROM item_pembelian")
    fun observeAllPurchaseItems(): Flow<List<PurchaseItem>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseItem(item: PurchaseItem): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseItems(items: List<PurchaseItem>): List<Long>

    @Update
    suspend fun updatePurchaseItem(item: PurchaseItem)

    @Query("DELETE FROM item_pembelian WHERE id = :id")
    suspend fun deletePurchaseItemById(id: Long)

    @Query("DELETE FROM item_pembelian WHERE purchaseId = :purchaseId")
    suspend fun deleteItemsByPurchaseId(purchaseId: Long)
}
