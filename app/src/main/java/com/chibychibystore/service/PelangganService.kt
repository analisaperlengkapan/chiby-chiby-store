package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Pelanggan
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk Pelanggan Service
 */
interface PelangganService {
    fun ambilSemuaPelanggan(): Flow<List<Pelanggan>>
    fun cariPelanggan(query: String): Flow<List<Pelanggan>>
    suspend fun getPelangganById(id: Long): Result<Pelanggan?>
    suspend fun buatPelanggan(
        nama: String,
        telepon: String? = null,
        email: String? = null,
        alamat: String? = null
    ): Result<Long>
    suspend fun perbaruiPelanggan(
        id: Long,
        nama: String,
        telepon: String? = null,
        email: String? = null,
        alamat: String? = null,
        point: Int? = null
    ): Result<Unit>
    suspend fun hapusPelanggan(pelanggan: Pelanggan): Result<Unit>
    suspend fun tambahPoint(id: Long, point: Int): Result<Unit>
}
