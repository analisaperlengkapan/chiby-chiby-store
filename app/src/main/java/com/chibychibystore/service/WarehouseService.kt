package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk Warehouse Service
 */
interface WarehouseService {
    suspend fun createGudang(gudang: Gudang): Result<Gudang>
    suspend fun updateGudang(gudang: Gudang): Result<Gudang>
    suspend fun deleteGudang(id: Long): Result<Unit>
    suspend fun getGudang(id: Long): Result<Gudang?>
    suspend fun getGudangs(): Result<List<Gudang>>
    suspend fun tugaskanProdukKeGudang(produkId: Long, gudangId: Long): Result<Unit>
    suspend fun transferStok(
        produkId: Long,
        dariGudangId: Long,
        keGudangId: Long,
        jumlah: Int
    ): Result<Unit>
    suspend fun getStokGudang(gudangId: Long): Result<List<Produk>>
    suspend fun getSemuaStokGudang(): Result<Map<Gudang, List<Produk>>>
    fun observeGudangs(): Flow<List<Gudang>>
    fun observeStokGudang(gudangId: Long): Flow<List<Produk>>
}
