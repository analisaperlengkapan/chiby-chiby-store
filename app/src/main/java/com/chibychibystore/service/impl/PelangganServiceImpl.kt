package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Pelanggan
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PelangganRepository
import com.chibychibystore.service.PelangganService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

@Singleton
class PelangganServiceImpl @Inject constructor(
    private val repo: PelangganRepository
) : PelangganService {

    override fun ambilSemuaPelanggan(): Flow<List<Pelanggan>> = repo.getAllPelanggan()

    override fun cariPelanggan(query: String): Flow<List<Pelanggan>> = repo.searchPelanggan(query)

    override suspend fun getPelangganById(id: Long): Result<Pelanggan?> = repo.getPelangganById(id)

    override suspend fun buatPelanggan(
        nama: String,
        telepon: String?,
        email: String?,
        alamat: String?
    ): Result<Long> = repo.createPelanggan(
        Pelanggan(name = nama, phone = telepon, email = email, address = alamat)
    )

    override suspend fun perbaruiPelanggan(
        id: Long,
        nama: String,
        telepon: String?,
        email: String?,
        alamat: String?,
        point: Int?
    ): Result<Unit> {
        val existingResult = repo.getPelangganById(id)
        if (existingResult is Result.Failure) return Result.failure(existingResult.exception)
        val existing = (existingResult as Result.Success).data ?: return Result.failure(Exception("Not found"))

        return repo.updatePelanggan(existing.copy(
            name = nama,
            phone = telepon ?: existing.phone,
            email = email ?: existing.email,
            address = alamat ?: existing.address,
            point = point ?: existing.point,
            updatedAt = Date()
        ))
    }

    override suspend fun hapusPelanggan(pelanggan: Pelanggan): Result<Unit> = repo.deletePelanggan(pelanggan)

    override suspend fun tambahPoint(id: Long, point: Int): Result<Unit> {
        val existingResult = repo.getPelangganById(id)
        if (existingResult is Result.Failure) return Result.failure(existingResult.exception)
        val existing = (existingResult as Result.Success).data ?: return Result.failure(Exception("Not found"))

        return repo.updatePelanggan(existing.copy(
            point = kotlin.math.max(0, existing.point + point),
            updatedAt = Date()
        ))
    }
}
