package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.KategoriDao
import com.chibychibystore.data.local.dao.ProdukDao
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Kategori
 */
@Singleton
class KategoriRepository @Inject constructor(
    private val kategoriDao: KategoriDao,
    private val produkDao: ProdukDao
) {

    /**
     * Get semua kategori
     */
    fun getAllKategori(): Flow<List<Kategori>> = kategoriDao.getAllKategori()

    /**
     * Get kategori by ID
     */
    suspend fun getKategoriById(id: Long): Result<Kategori> {
        return try {
            val kategori = kategoriDao.getKategoriById(id)
            if (kategori != null) {
                Result.success(kategori)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Kategori dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getKategoriById", e))
        }
    }

    /**
     * Get kategori by name
     */
    suspend fun getKategoriByName(name: String): Result<Kategori> {
        return try {
            val kategori = kategoriDao.getKategoriByName(name)
            if (kategori != null) {
                Result.success(kategori)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Kategori dengan nama $name tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getKategoriByName", e))
        }
    }

    /**
     * Create kategori baru
     */
    suspend fun createKategori(kategori: Kategori): Result<Long> {
        return try {
            // Validasi input
            validateKategoriData(kategori)

            // Check if name already exists
            val existingKategori = kategoriDao.getKategoriByName(kategori.name)
            if (existingKategori != null) {
                return Result.failure(ChibyChibyException.ValidationError("name", "Nama kategori sudah digunakan"))
            }

            val id = kategoriDao.insertKategori(kategori)
            Result.success(id)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createKategori", e))
        }
    }

    /**
     * Update kategori
     */
    suspend fun updateKategori(kategori: Kategori): Result<Unit> {
        return try {
            // Validasi input
            validateKategoriData(kategori)

            // Check if kategori exists
            val existingKategori = kategoriDao.getKategoriById(kategori.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Kategori tidak ditemukan"))

            // Check name uniqueness (exclude current kategori)
            val kategoriWithSameName = kategoriDao.getKategoriByName(kategori.name)
            if (kategoriWithSameName != null && kategoriWithSameName.id != kategori.id) {
                return Result.failure(ChibyChibyException.ValidationError("name", "Nama kategori sudah digunakan"))
            }

            kategoriDao.updateKategori(kategori)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateKategori", e))
        }
    }

    /**
     * Delete kategori
     */
    suspend fun deleteKategori(id: Long): Result<Unit> {
        return try {
            // Check if kategori exists
            val kategori = kategoriDao.getKategoriById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Kategori tidak ditemukan"))

            // Check if kategori is used by products (business rule)
            val productCount = produkDao.countProdukByKategori(id)
            if (productCount > 0) {
                return Result.failure(
                    ChibyChibyException.ValidationError(
                        "id",
                        "Tidak dapat menghapus kategori yang masih memiliki produk"
                    )
                )
            }

            kategoriDao.deleteKategoriById(id)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteKategori", e))
        }
    }

    /**
     * Get jumlah total kategori
     */
    suspend fun getKategoriCount(): Result<Int> {
        return try {
            val count = kategoriDao.getKategoriCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getKategoriCount", e))
        }
    }

    private fun validateKategoriData(kategori: Kategori) {
        if (kategori.name.isBlank()) {
            throw ChibyChibyException.ValidationError("name", "Nama kategori tidak boleh kosong")
        }
        if (kategori.name.length < 2) {
            throw ChibyChibyException.ValidationError("name", "Nama kategori minimal 2 karakter")
        }
    }
}