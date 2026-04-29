package com.chibychibystore.service

import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Sale Service untuk manajemen transaksi penjualan di Chiby Chiby Store
 */
interface SaleService {
    suspend fun createPenjualan(sale: Penjualan, items: List<ItemPenjualan>): Result<PenjualanWithItems>
    suspend fun getPenjualanById(id: Long): Result<PenjualanWithItems?>
    suspend fun getPenjualanByRentangTanggal(
        startDate: String? = null,
        endDate: String? = null,
        cashierId: Long? = null,
        query: String? = null
    ): Result<List<Penjualan>>
    suspend fun getRecentPenjualan(limit: Int): Result<List<Penjualan>>
    suspend fun searchPenjualan(query: String): Result<List<Penjualan>>
    suspend fun updatePenjualan(id: Long, sale: Penjualan): Result<Penjualan>
    suspend fun deletePenjualan(id: Long): Result<Unit>
    suspend fun refundPenjualan(id: Long): Result<Unit>
    suspend fun cancelPenjualan(id: Long): Result<Unit>
    suspend fun getTotalPenjualanByRentangTanggal(startDate: String, endDate: String): Result<Double>
    suspend fun getPenjualanCountByRentangTanggal(startDate: String, endDate: String): Result<Int>
    fun observePenjualan(): Flow<List<Penjualan>>
    fun observePenjualanWithItems(): Flow<List<PenjualanWithItems>>
    fun observePenjualanFiltered(startDate: String, endDate: String, query: String? = null): Flow<List<Penjualan>>
    suspend fun cetakStruk(
        saleId: Long,
        storeName: String = "Chiby Chiby Store",
        storeAddress: String = "Jl. Example No. 123, Jakarta",
        cashierName: String = "Kasir"
    ): Result<Unit>
    fun observePenjualanWithItemsByRentangTanggal(startDate: String, endDate: String): Flow<List<PenjualanWithItems>>
    suspend fun getOpenShift(kasirId: Long): Result<com.chibychibystore.data.local.entity.Shift?>
}
