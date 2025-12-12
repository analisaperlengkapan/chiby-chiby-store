package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.GudangDao
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Gudang
 */
@Singleton
class GudangRepository @Inject constructor(
    private val gudangDao: GudangDao
) {

    /**
     * Get semua gudang
     */
    fun getAllGudang(): Flow<List<Gudang>> = gudangDao.getAllGudang()

    /**
     * Get gudang by ID
     */
    suspend fun getGudangById(id: Long): Result<Gudang> {
        return try {
            val gudang = gudangDao.getGudangById(id)
            if (gudang != null) {
                Result.success(gudang)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Gudang dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getGudangById", e))
        }
    }

    /**
     * Get gudang by name
     */
    suspend fun getGudangByName(name: String): Result<Gudang> {
        return try {
            val gudang = gudangDao.getGudangByName(name)
            if (gudang != null) {
                Result.success(gudang)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Gudang dengan nama $name tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getGudangByName", e))
        }
    }

    /**
     * Create gudang baru
     */
    suspend fun createGudang(gudang: Gudang): Result<Long> {
        return try {
            // Validasi input
            validateGudangData(gudang)

            // Check if name already exists
            val existingGudang = gudangDao.getGudangByName(gudang.name)
            if (existingGudang != null) {
                return Result.failure(ChibyChibyException.ValidationError("name", "Nama gudang sudah digunakan"))
            }

            val id = gudangDao.insertGudang(gudang)
            Result.success(id)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createGudang", e))
        }
    }

    /**
     * Update gudang
     */
    suspend fun updateGudang(gudang: Gudang): Result<Unit> {
        return try {
            // Validasi input
            validateGudangData(gudang)

            // Check if gudang exists
            val existingGudang = gudangDao.getGudangById(gudang.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Gudang tidak ditemukan"))

            // Check name uniqueness (exclude current gudang)
            val gudangWithSameName = gudangDao.getGudangByName(gudang.name)
            if (gudangWithSameName != null && gudangWithSameName.id != gudang.id) {
                return Result.failure(ChibyChibyException.ValidationError("name", "Nama gudang sudah digunakan"))
            }

            gudangDao.updateGudang(gudang)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateGudang", e))
        }
    }

    /**
     * Delete gudang
     */
    suspend fun deleteGudang(id: Long): Result<Unit> {
        return try {
            // Check if gudang exists
            val gudang = gudangDao.getGudangById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Gudang tidak ditemukan"))

            // Check if gudang is used by products (business rule)
            // This would require checking ProdukDao, but for now we'll allow deletion
            // In a full implementation, you'd check for foreign key constraints

            gudangDao.deleteGudangById(id)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteGudang", e))
        }
    }

    /**
     * Get jumlah total gudang
     */
    suspend fun getGudangCount(): Result<Int> {
        return try {
            val count = gudangDao.getGudangCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getGudangCount", e))
        }
    }

    private fun validateGudangData(gudang: Gudang) {
        if (gudang.name.isBlank()) {
            throw ChibyChibyException.ValidationError("name", "Nama gudang tidak boleh kosong")
        }
        if (gudang.name.length < 2) {
            throw ChibyChibyException.ValidationError("name", "Nama gudang minimal 2 karakter")
        }
        if (gudang.capacity < 0) {
            throw ChibyChibyException.ValidationError("capacity", "Kapasitas gudang tidak boleh negatif")
        }
    }
}