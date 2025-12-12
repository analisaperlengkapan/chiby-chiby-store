package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chibychibystore.data.local.entity.ItemPenjualan
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemPenjualanDao {
    @Query("SELECT * FROM item_penjualan WHERE saleId = :saleId")
    fun getItemsBySaleId(saleId: Long): Flow<List<ItemPenjualan>>

    @Query("SELECT * FROM item_penjualan WHERE productId = :productId ORDER BY id DESC")
    fun getItemsByProductId(productId: Long): Flow<List<ItemPenjualan>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemPenjualan(item: ItemPenjualan): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemPenjualanList(items: List<ItemPenjualan>): List<Long>

    @Query("DELETE FROM item_penjualan WHERE saleId = :saleId")
    suspend fun deleteItemsBySaleId(saleId: Long)

    @Query("SELECT COUNT(*) FROM item_penjualan WHERE saleId = :saleId")
    suspend fun getItemCountBySaleId(saleId: Long): Int
}