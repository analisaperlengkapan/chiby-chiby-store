package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PembelianDao
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date
import java.time.LocalDate
import java.time.ZoneId
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
    suspend fun getPurchasesInDateRange(startDate: LocalDate, endDate: LocalDate): List<Pembelian> {
        val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
        return pembelianDao.getPembelianByRentangTanggal(start, end).first()
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
    }
}