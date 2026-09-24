package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Pelanggan
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PelangganRepository
import com.chibychibystore.service.PelangganService
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PelangganServiceImpl @Inject constructor(
    private val pelangganRepository: PelangganRepository
) : PelangganService {

    override fun ambilSemuaPelanggan(): Flow<List<Pelanggan>> = pelangganRepository.getAllPelanggan()

    override fun cariPelanggan(query: String): Flow<List<Pelanggan>> = pelangganRepository.searchPelanggan(query)

    override suspend fun getPelangganById(id: Long): Result<Pelanggan?> = pelangganRepository.getPelangganById(id)

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
        if (existingResult is Result.Failure) return Result.failure((existingResult as Result.Failure).exception)

        val existing = (existingResult as Result.Success).data ?: return Result.failure(Exception("Pelanggan tidak ditemukan"))

        val updated = existing.copy(
            name = nama,
            phone = telepon,
            email = email,
            address = alamat,
            point = point ?: existing.point,
            updatedAt = Date()
        )
        return pelangganRepository.updatePelanggan(updated)
    }

    override suspend fun hapusPelanggan(pelanggan: Pelanggan): Result<Unit> = pelangganRepository.deletePelanggan(pelanggan)

    override suspend fun tambahPoint(id: Long, point: Int): Result<Unit> {
        val existingResult = pelangganRepository.getPelangganById(id)
        if (existingResult is Result.Failure) return Result.failure((existingResult as Result.Failure).exception)

        val existing = (existingResult as Result.Success).data ?: return Result.failure(Exception("Pelanggan tidak ditemukan"))

        // Floor at 0 to prevent negative point balances when callers pass a
        // negative delta (e.g. for point redemption or adjustment) that would
        // exceed the customer's current balance.
        val updated = existing.copy(
            point = kotlin.math.max(0, existing.point + point),
            updatedAt = Date()
        )
        return pelangganRepository.updatePelanggan(updated)
    }
}
