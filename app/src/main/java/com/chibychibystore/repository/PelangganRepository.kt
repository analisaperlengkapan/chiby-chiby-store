package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PelangganDao
import com.chibychibystore.data.local.entity.Pelanggan
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PelangganRepository @Inject constructor(
    private val pelangganDao: PelangganDao
) {
    fun getAllPelanggan(): Flow<List<Pelanggan>> = pelangganDao.getAllPelanggan()

    suspend fun getPelangganById(id: Long): Result<Pelanggan?> {
        return try {
            Result.success(pelangganDao.getPelangganById(id))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPelangganById", e))
        }
    }

    fun searchPelanggan(query: String): Flow<List<Pelanggan>> = pelangganDao.searchPelanggan(query)

    suspend fun createPelanggan(pelanggan: Pelanggan): Result<Long> {
        return try {
            Result.success(pelangganDao.insertPelanggan(pelanggan))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPelanggan", e))
        }
    }

    suspend fun updatePelanggan(pelanggan: Pelanggan): Result<Unit> {
        return try {
            pelangganDao.updatePelanggan(pelanggan)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updatePelanggan", e))
        }
    }

    suspend fun deletePelanggan(pelanggan: Pelanggan): Result<Unit> {
        return try {
            pelangganDao.deletePelanggan(pelanggan)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deletePelanggan", e))
        }
    }
}
