package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.model.Result
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Pengguna
 */
@Singleton
class PenggunaRepository @Inject constructor(
    private val penggunaDao: PenggunaDao
) {

    /**
     * Get semua pengguna
     */
    fun getAllPengguna(): Flow<List<Pengguna>> = penggunaDao.getAllPengguna()

    /**
     * Get pengguna by ID
     */
    suspend fun getPenggunaById(id: Long): Result<Pengguna> {
        return try {
            val user = penggunaDao.getPenggunaById(id)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("User dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPenggunaById", e))
        }
    }

    /**
     * Get pengguna by username
     */
    suspend fun getPenggunaByUsername(username: String): Result<Pengguna> {
        return try {
            val user = penggunaDao.getPenggunaByUsername(username)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("User dengan username $username tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPenggunaByUsername", e))
        }
    }

    /**
     * Get pengguna by role
     */
    fun getPenggunaByRole(role: Role): Flow<List<Pengguna>> = penggunaDao.getPenggunaByRole(role)

    /**
     * Search pengguna
     */
    fun searchPengguna(query: String): Flow<List<Pengguna>> = penggunaDao.searchPengguna(query)

    /**
     * Create pengguna baru
     */
    suspend fun createPengguna(pengguna: Pengguna): Result<Long> {
        return try {
            // Validasi input
            validatePenggunaData(pengguna)

            // Check if username already exists
            val existingUser = penggunaDao.getPenggunaByUsername(pengguna.username)
            if (existingUser != null) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username sudah digunakan"))
            }

            val id = penggunaDao.insertPengguna(pengguna)
            Result.success(id)

        } catch (e: ChibyChibyException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createPengguna", e))
        }
    }

    /**
     * Update pengguna
     */
    suspend fun updatePengguna(pengguna: Pengguna): Result<Unit> {
        return try {
            // Validasi input
            validatePenggunaData(pengguna)

            // Check if user exists
            penggunaDao.getPenggunaById(pengguna.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("User tidak ditemukan"))

            // Check username uniqueness (exclude current user)
            val userWithSameUsername = penggunaDao.getPenggunaByUsername(pengguna.username)
            if (userWithSameUsername != null && userWithSameUsername.id != pengguna.id) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username sudah digunakan"))
            }

            penggunaDao.updatePengguna(pengguna)
            Result.success(Unit)

        } catch (e: ChibyChibyException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updatePengguna", e))
        }
    }

    /**
     * Delete pengguna
     */
    suspend fun deletePengguna(id: Long): Result<Unit> {
        return try {
            // Check if user exists
            val user = penggunaDao.getPenggunaById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("User tidak ditemukan"))

            // Business rule: tidak boleh menghapus owner terakhir
            if (user.role == Role.OWNER) {
                val users = penggunaDao.getAllPengguna().first()
                val ownerCount = users.count { it.role == Role.OWNER }
                if (ownerCount <= 1) {
                    return Result.failure(ChibyChibyException.BusinessLogicError("Tidak dapat menghapus owner terakhir"))
                }
            }

            penggunaDao.deletePenggunaById(id)
            Result.success(Unit)

        } catch (e: ChibyChibyException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deletePengguna", e))
        }
    }

    /**
     * Get jumlah total pengguna
     */
    suspend fun getPenggunaCount(): Result<Int> {
        return try {
            val count = penggunaDao.getPenggunaCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPenggunaCount", e))
        }
    }

    private fun validatePenggunaData(pengguna: Pengguna) {
        if (pengguna.username.isBlank()) {
            throw ChibyChibyException.ValidationError("username", "Username tidak boleh kosong")
        }
        if (pengguna.username.length < 3) {
            throw ChibyChibyException.ValidationError("username", "Username minimal 3 karakter")
        }
        if (pengguna.passwordHash.isBlank()) {
            throw ChibyChibyException.ValidationError("password", "Password tidak boleh kosong")
        }
    }
}