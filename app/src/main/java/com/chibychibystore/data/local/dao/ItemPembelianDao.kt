package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.ItemPembelian
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemPembelianDao {
    @Query("SELECT * FROM item_pembelian WHERE purchaseId = :purchaseId")
    fun getItemsByPembelianId(purchaseId: Long): Flow<List<ItemPembelian>>

    @Query("SELECT * FROM item_pembelian WHERE productId = :productId")
    fun getItemsByProductId(productId: Long): Flow<List<ItemPembelian>>

    @Query("SELECT * FROM item_pembelian")
    fun getAllPurchaseItems(): Flow<List<ItemPembelian>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemPembelian(item: ItemPembelian): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemPembelianList(items: List<ItemPembelian>): List<Long>

    @Update
    suspend fun updateItemPembelian(item: ItemPembelian)

    @Query("DELETE FROM item_pembelian WHERE id = :id")
    suspend fun deleteItemPembelianById(id: Long)

    @Query("DELETE FROM item_pembelian WHERE purchaseId = :purchaseId")
    suspend fun deleteItemsByPembelianId(purchaseId: Long)
}
