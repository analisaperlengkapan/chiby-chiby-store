package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Pelanggan
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PelangganRepository
import com.chibychibystore.service.PelangganService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PelangganServiceImpl @Inject constructor(
    private val pelangganRepository: PelangganRepository
) : PelangganService {

    override fun ambilSemuaPelanggan(): Flow<List<Pelanggan>> {
        return pelangganRepository.getAllPelanggan()
    }

    override fun cariPelanggan(query: String): Flow<List<Pelanggan>> {
        return pelangganRepository.searchPelanggan(query)
    }

    override suspend fun getPelangganById(id: Long): Result<Pelanggan?> {
        return pelangganRepository.getPelangganById(id)
    }

    override suspend fun buatPelanggan(
        nama: String,
        telepon: String?,
        email: String?,
        alamat: String?
    ): Result<Long> {
        val pelanggan = Pelanggan(
            name = nama,
            phone = telepon,
            email = email,
            address = alamat
        )
        return pelangganRepository.createPelanggan(pelanggan)
    }

    override suspend fun perbaruiPelanggan(
        id: Long,
        nama: String,
        telepon: String?,
        email: String?,
        alamat: String?,
        point: Int?
    ): Result<Unit> {
        val existingResult = pelangganRepository.getPelangganById(id)
        if (existingResult is Result.Failure) return Result.failure(existingResult.exception)
        val existing = (existingResult as Result.Success).data
            ?: return Result.failure(Exception("Pelanggan tidak ditemukan"))

        val updated = existing.copy(
            name = nama,
            phone = telepon ?: existing.phone,
            email = email ?: existing.email,
            address = alamat ?: existing.address,
            point = point ?: existing.point,
            updatedAt = java.util.Date()
        )
        return pelangganRepository.updatePelanggan(updated)
    }

    override suspend fun hapusPelanggan(pelanggan: Pelanggan): Result<Unit> {
        return pelangganRepository.deletePelanggan(pelanggan)
    }

    override suspend fun tambahPoint(id: Long, point: Int): Result<Unit> {
        val existingResult = pelangganRepository.getPelangganById(id)
        if (existingResult is Result.Failure) return Result.failure(existingResult.exception)
        val existing = (existingResult as Result.Success).data
            ?: return Result.failure(Exception("Pelanggan tidak ditemukan"))

        val updated = existing.copy(
            point = existing.point + point,
            updatedAt = java.util.Date()
        )
        return pelangganRepository.updatePelanggan(updated)
    }
}
