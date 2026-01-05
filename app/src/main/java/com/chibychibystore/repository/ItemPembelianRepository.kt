package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ItemPembelianDao
import com.chibychibystore.data.local.entity.ItemPembelian
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data ItemPembelian
 */
@Singleton
class ItemPembelianRepository @Inject constructor(
    private val itemPembelianDao: ItemPembelianDao
) {

    /**
     * Get items by pembelian ID
     */
    fun getItemsByPurchaseId(purchaseId: Long): Flow<List<ItemPembelian>> =
        itemPembelianDao.getItemsByPembelianId(purchaseId)

    /**
     * Get items by product ID
     */
    fun getItemsByProductId(productId: Long): Flow<List<ItemPembelian>> =
        itemPembelianDao.getItemsByProductId(productId)

    /**
     * Get all purchase items
     */
    fun getAllPurchaseItems(): Flow<List<ItemPembelian>> =
        itemPembelianDao.getAllPurchaseItems()

    /**
     * Create item pembelian baru
     */
    suspend fun createItemPembelian(item: ItemPembelian): Result<ItemPembelian> {
        return try {
            // Validasi data
            validateItemPembelian(item)

            val id = itemPembelianDao.insertItemPembelian(item)
            val createdItem = item.copy(id = id)
            Result.success(createdItem)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createItemPembelian", e))
        }
    }


    /**
     * Delete items by purchase ID
     */
    suspend fun deleteItemsByPurchaseId(purchaseId: Long): Result<Unit> {
        return try {
            itemPembelianDao.deleteItemsByPembelianId(purchaseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteItemsByPurchaseId", e))
        }
    }


    /**
     * Validasi item pembelian
     */
    private fun validateItemPembelian(item: ItemPembelian) {
        require(item.purchaseId > 0) { "ID pembelian harus valid" }
        require(item.productId > 0) { "ID produk harus valid" }
        require(item.quantity > 0) { "Jumlah harus lebih dari 0" }
        require(item.unitPrice >= 0) { "Harga beli tidak boleh negatif" }
    }
}