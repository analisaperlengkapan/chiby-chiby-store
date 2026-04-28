package com.chibychibystore.service

import com.chibychibystore.data.local.entity.ItemPembelian
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Interface untuk Purchase Service (Manajemen Pembelian)
 */
interface PurchaseService {
    suspend fun createPembelian(pembelian: Pembelian, items: List<ItemPembelian>): Result<Pembelian>
    suspend fun getPembelianById(id: Long): Result<Pembelian?>
    suspend fun getPembelianInDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<Pembelian>>
    suspend fun deletePembelian(id: Long): Result<Unit>
    fun observeAllPurchases(): Flow<List<Pembelian>>
    suspend fun getItemsByPurchaseId(purchaseId: Long): Result<List<ItemPembelian>>
}
