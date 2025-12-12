package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chibychibystore.data.local.entity.ItemPembelian
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemPembelianDao {
    @Query("SELECT * FROM item_pembelian WHERE purchaseId = :purchaseId")
    fun getItemsByPurchaseId(purchaseId: Long): Flow<List<ItemPembelian>>

    @Query("SELECT * FROM item_pembelian WHERE productId = :productId ORDER BY id DESC")
    fun getItemsByProductId(productId: Long): Flow<List<ItemPembelian>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemPembelian(item: ItemPembelian): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemPembelianList(items: List<ItemPembelian>): List<Long>

    @Query("DELETE FROM item_pembelian WHERE purchaseId = :purchaseId")
    suspend fun deleteItemsByPurchaseId(purchaseId: Long)

    @Query("SELECT COUNT(*) FROM item_pembelian WHERE purchaseId = :purchaseId")
    suspend fun getItemCountByPurchaseId(purchaseId: Long): Int
}