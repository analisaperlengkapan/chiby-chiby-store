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
    fun getAllUsers(): Flow<List<Pengguna>> = penggunaDao.getAllPengguna()

    /**
     * Get pengguna by ID
     */
    suspend fun getUserById(id: Long): Result<Pengguna> {
        return try {
            val user = penggunaDao.getPenggunaById(id)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Pengguna dengan ID $id tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getUserById", e))
        }
    }

    /**
     * Get pengguna by username
     */
    suspend fun getUserByUsername(username: String): Result<Pengguna> {
        return try {
            val user = penggunaDao.getPenggunaByUsername(username)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Pengguna dengan username $username tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getUserByUsername", e))
        }
    }

    /**
     * Get pengguna by role
     */
    fun getUsersByRole(role: Role): Flow<List<Pengguna>> = penggunaDao.getPenggunaByRole(role)

    /**
     * Search pengguna
     */
    fun searchUsers(query: String): Flow<List<Pengguna>> = penggunaDao.searchPengguna(query)

    /**
     * Create pengguna baru
     */
    suspend fun createPengguna(pengguna: Pengguna): Result<Long> {
        return try {
            validatePenggunaData(pengguna)

            val existingUser = penggunaDao.getPenggunaByUsername(pengguna.username)
            if (existingUser != null) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username sudah digunakan"))
            }

            val id = penggunaDao.insertPengguna(pengguna)
            Result.success(id)

        } catch (e: ChibyChibyException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createUser", e))
        }
    }

    /**
     * Update pengguna
     */
    suspend fun updateUser(pengguna: Pengguna): Result<Unit> {
        return try {
            validatePenggunaData(pengguna)

            penggunaDao.getPenggunaById(pengguna.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Pengguna tidak ditemukan"))

            val userWithSameUsername = penggunaDao.getPenggunaByUsername(pengguna.username)
            if (userWithSameUsername != null && userWithSameUsername.id != pengguna.id) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username sudah digunakan"))
            }

            penggunaDao.updatePengguna(pengguna)
            Result.success(Unit)

        } catch (e: ChibyChibyException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateUser", e))
        }
    }

    /**
     * Delete pengguna
     */
    suspend fun deleteUser(id: Long): Result<Unit> {
        return try {
            val user = penggunaDao.getPenggunaById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Pengguna tidak ditemukan"))

            if (user.role == Role.OWNER) {
                val ownerCount = penggunaDao.countByRole(Role.OWNER)
                if (ownerCount <= 1) {
                    return Result.failure(ChibyChibyException.BusinessLogicError("Tidak dapat menghapus owner terakhir"))
                }
            }

            penggunaDao.deletePenggunaById(id)
            Result.success(Unit)

        } catch (e: ChibyChibyException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteUser", e))
        }
    }

    /**
     * Get jumlah total pengguna
     */
    suspend fun getUserCount(): Result<Int> {
        return try {
            val count = penggunaDao.getPenggunaCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getUserCount", e))
        }
    }

    /**
     * Count active users
     */
    suspend fun countActiveUsers(): Result<Int> {
        return try {
            val count = penggunaDao.countActiveUsers()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("countActiveUsers", e))
        }
    }

    /**
     * Count users by role
     */
    suspend fun countByRole(role: Role): Result<Int> {
        return try {
            val count = penggunaDao.countByRole(role)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("countByRole", e))
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