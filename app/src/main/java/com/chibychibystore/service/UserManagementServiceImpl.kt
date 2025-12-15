package com.chibychibystore.service

import com.chibychibystore.data.model.Result
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PenggunaRepository
import com.chibychibystore.repository.UserSessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManagementServiceImpl @Inject constructor(
    private val penggunaRepository: PenggunaRepository,
    private val userSessionRepository: UserSessionRepository,
    private val authService: AuthService
) : UserManagementService {

    override fun getAllUsers(): Flow<List<Pengguna>> =
        penggunaRepository.getAllPengguna()

    override suspend fun getUserStats(): Result<UserStats> {
        return try {
            val allUsers = penggunaRepository.getAllPengguna().first()
            val totalUsers = allUsers.size
            val activeUsers = allUsers.count { it.isActive }
            val owners = allUsers.count { it.role == "OWNER" }
            val managers = allUsers.count { it.role == "MANAGER" }
            val cashiers = allUsers.count { it.role == "CASHIER" }
            val warehouseStaff = allUsers.count { it.role == "WAREHOUSE_STAFF" }
            
            Result.success(UserStats(
                totalUsers = totalUsers,
                activeUsers = activeUsers,
                owners = owners,
                managers = managers,
                cashiers = cashiers,
                warehouseStaff = warehouseStaff
            ))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getUserStats", e))
        }
    }

    override fun getUsersByRole(role: Role): Flow<List<Pengguna>> =
        penggunaRepository.getPenggunaByRole(role)

    override fun canDeleteLastOwner(): Flow<Boolean> {
        return penggunaRepository.getAllPengguna().map { users ->
            users.count { it.role == "OWNER" } > 1
        }
    }

    override suspend fun createUser(
        username: String,
        password: String,
        role: Role,
        createdBy: Long
    ): Result<Long> {
        return try {
            // Validate input
            if (username.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username tidak boleh kosong"))
            }
            if (password.length < 6) {
                return Result.failure(ChibyChibyException.ValidationError("password", "Password minimal 6 karakter"))
            }

            // Check if username already exists
            val existingUserResult = penggunaRepository.getPenggunaByUsername(username)
            if (existingUserResult.isSuccess) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username sudah digunakan"))
            }

            // Hash password using SHA-256
            val passwordHash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(password.toByteArray())
                .joinToString("") { "%02x".format(it) }

            val user = Pengguna(
                username = username,
                passwordHash = passwordHash,
                role = role,
                permissions = "[]", // Default empty permissions
                createdAt = java.util.Date(),
                updatedAt = java.util.Date()
            )

            val result = penggunaRepository.createPengguna(user)
            result as com.chibychibystore.data.model.Result<Long>
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal membuat user", e))
        }
    }

    override suspend fun updateUser(
        userId: Long,
        username: String?,
        role: Role?,
        isActive: Boolean?,
        updatedBy: Long
    ): Result<Unit> {
        return try {
            // Get existing user
            val existingUser = penggunaRepository.getPenggunaById(userId)
                ?: return Result.failure(Exception("User tidak ditemukan"))

            // Validate username uniqueness if changed
            if (username != null && username != existingUser.username) {
                val userWithSameUsername = penggunaRepository.getPenggunaByUsername(username)
                if (userWithSameUsername != null) {
                    return Result.failure(Exception("Username sudah digunakan"))
                }
            }

            val updatedUser = existingUser.copy(
                username = username ?: existingUser.username,
                role = role?.name ?: existingUser.role,
                updatedAt = java.util.Date()
            )

            penggunaRepository.updatePengguna(updatedUser) as com.chibychibystore.data.model.Result<Unit>
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal update user", e))
        }
    }

    override suspend fun deleteUser(userId: Long, deletedBy: Long): Result<Unit> {
        return try {
            // Check if user exists
            val user = penggunaRepository.getPenggunaById(userId)
                ?: return Result.failure(Exception("User tidak ditemukan"))

            // Prevent deleting self
            val currentUser = authService.getCurrentUser()
            if (currentUser?.id == userId) {
                return Result.failure(Exception("Tidak dapat menghapus user sendiri"))
            }

            // Check permissions (only OWNER can delete users)
            if (currentUser?.role != "OWNER") {
                return Result.failure(Exception("Hanya Owner yang dapat menghapus user"))
            }

            penggunaRepository.deletePengguna(userId) as com.chibychibystore.data.model.Result<Unit>
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghapus user", e))
        }
    }

    override suspend fun resetUserPassword(
        userId: Long,
        newPassword: String,
        resetBy: Long
    ): Result<Unit> {
        return try {
            if (newPassword.length < 6) {
                return Result.failure(Exception("Password minimal 6 karakter"))
            }

            val passwordHash = authService.hashPassword(newPassword)
            penggunaRepository.updatePassword(userId, passwordHash)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal reset password: ${e.message}"))
        }
    }

    override suspend fun deactivateUser(userId: Long, deactivatedBy: Long): Result<Unit> {
        return try {
            // Check permissions
            val currentUser = authService.getCurrentUser()
            if (currentUser?.role != "OWNER") {
                return Result.failure(Exception("Hanya Owner yang dapat menonaktifkan user"))
            }

            // Prevent deactivating self
            if (currentUser.id == userId) {
                return Result.failure(Exception("Tidak dapat menonaktifkan user sendiri"))
            }

            // For now, just mark as inactive (future: add isActive field)
            // Since we don't have isActive field yet, this is a placeholder
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menonaktifkan user: ${e.message}"))
        }
    }

    override suspend fun activateUser(userId: Long, activatedBy: Long): Result<Unit> {
        return try {
            // Check permissions
            val currentUser = authService.getCurrentUser()
            if (currentUser?.role != "OWNER") {
                return Result.failure(Exception("Hanya Owner yang dapat mengaktifkan user"))
            }

            // For now, just mark as active (future: add isActive field)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mengaktifkan user: ${e.message}"))
        }
    }
}