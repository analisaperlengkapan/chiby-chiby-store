package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PemasokDao
import com.chibychibystore.data.local.dao.PembelianDao
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Pemasok
 */
@Singleton
class PemasokRepository @Inject constructor(
    private val pemasokDao: PemasokDao,
    private val pembelianDao: PembelianDao
) {

    /**
     * Get semua pemasok
     */
    fun getAllPemasok(): Flow<List<Pemasok>> = pemasokDao.getAllPemasok()

    /**
     * Get pemasok by ID
     */
    suspend fun getPemasokById(id: Long): Result<Pemasok> {
        return try {
            val pemasok = pemasokDao.getPemasokById(id)
            if (pemasok != null) {
                Result.success(pemasok)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Pemasok dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPemasokById", e))
        }
    }

    /**
     * Search pemasok
     */
    fun searchPemasok(query: String): Flow<List<Pemasok>> = pemasokDao.searchPemasok(query)

    /**
     * Create pemasok baru
     */
    suspend fun createPemasok(pemasok: Pemasok): Result<Long> {
        return try {
            // Validasi input
            validatePemasokData(pemasok)

            val id = pemasokDao.insertPemasok(pemasok)
            Result.success(id)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPemasok", e))
        }
    }

    /**
     * Update pemasok
     */
    suspend fun updatePemasok(pemasok: Pemasok): Result<Unit> {
        return try {
            // Validasi input
            validatePemasokData(pemasok)

            // Check if pemasok exists
            val existingPemasok = pemasokDao.getPemasokById(pemasok.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Pemasok tidak ditemukan"))

            pemasokDao.updatePemasok(pemasok)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updatePemasok", e))
        }
    }

    /**
     * Delete pemasok
     */
    suspend fun deletePemasok(id: Long): Result<Unit> {
        return try {
            // Check if pemasok exists
            val pemasok = pemasokDao.getPemasokById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Pemasok tidak ditemukan"))

            // Check if pemasok is used by purchases
            val purchaseCount = pembelianDao.countPembelianByPemasok(id)
            if (purchaseCount > 0) {
                return Result.failure(ChibyChibyException.DatabaseError("Pemasok tidak dapat dihapus karena memiliki riwayat pembelian"))
            }

            pemasokDao.deletePemasokById(id)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deletePemasok", e))
        }
    }

    /**
     * Get jumlah total pemasok
     */
    suspend fun getPemasokCount(): Result<Int> {
        return try {
            val count = pemasokDao.getPemasokCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPemasokCount", e))
        }
    }

    private fun validatePemasokData(pemasok: Pemasok) {
        if (pemasok.name.isBlank()) {
            throw ChibyChibyException.ValidationError("name", "Nama pemasok tidak boleh kosong")
        }
        if (pemasok.name.length < 2) {
            throw ChibyChibyException.ValidationError("name", "Nama pemasok minimal 2 karakter")
        }
    }
}