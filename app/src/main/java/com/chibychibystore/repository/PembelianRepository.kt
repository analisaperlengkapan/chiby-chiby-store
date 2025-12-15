package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PembelianDao
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Pembelian
 */
@Singleton
class PembelianRepository @Inject constructor(
    private val pembelianDao: PembelianDao
) {

    /**
     * Get semua pembelian
     */
    fun getAllPembelian(): Flow<List<Pembelian>> = pembelianDao.getAllPembelian()

    /**
     * Get purchases in date range
     */
    suspend fun getPurchasesInDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate): List<Pembelian> {
        val start = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        val end = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())
        return pembelianDao.getPurchasesInDateRange(start, end)
    }

    /**
     * Get pembelian by ID
     */
    suspend fun getPembelianById(id: Long): Result<Pembelian> {
        return try {
            val pembelian = pembelianDao.getPembelianById(id)
            if (pembelian != null) {
                Result.success(pembelian)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Pembelian dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPembelianById", e))
        }
    }

    /**
     * Create pembelian baru
     */
    suspend fun createPembelian(pembelian: Pembelian): Result<Pembelian> {
        return try {
            // Validasi data
            validatePembelian(pembelian)

            val id = pembelianDao.insertPembelian(pembelian)
            val createdPembelian = pembelian.copy(id = id)
            Result.success(createdPembelian)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPembelian", e))
        }
    }

    /**
     * Delete pembelian
     */
    suspend fun deletePembelian(id: Long): Result<Unit> {
        return try {
            pembelianDao.deletePembelianById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deletePembelian", e))
        }
    }

    /**
     * Validasi pembelian
     */
    private fun validatePembelian(pembelian: Pembelian) {
        require(pembelian.supplierId > 0) { "ID pemasok harus valid" }
        require(pembelian.totalAmount >= 0) { "Total amount tidak boleh negatif" }
        require(pembelian.purchaseDate != null) { "Tanggal pembelian harus diisi" }
    }
}