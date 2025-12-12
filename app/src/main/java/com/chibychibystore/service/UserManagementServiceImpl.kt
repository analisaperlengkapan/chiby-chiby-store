package com.chibychibystore.service

import com.chibychibystore.data.Result
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
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

    override fun getUsersByRole(role: Role): Flow<List<Pengguna>> =
        penggunaRepository.getPenggunaByRole(role)

    override suspend fun createUser(
        username: String,
        password: String,
        role: Role,
        createdBy: Long
    ): Result<Long> {
        return try {
            // Validate input
            if (username.isBlank()) {
                return Result.Error("Username tidak boleh kosong")
            }
            if (password.length < 6) {
                return Result.Error("Password minimal 6 karakter")
            }

            // Check if username already exists
            val existingUser = penggunaRepository.getPenggunaByUsername(username)
            if (existingUser != null) {
                return Result.Error("Username sudah digunakan")
            }

            // Hash password
            val passwordHash = authService.hashPassword(password)

            val user = Pengguna(
                username = username,
                passwordHash = passwordHash,
                role = role.name,
                permissions = "[]", // Default empty permissions
                createdAt = java.util.Date(),
                updatedAt = java.util.Date()
            )

            penggunaRepository.insertPengguna(user)
        } catch (e: Exception) {
            Result.Error("Gagal membuat user: ${e.message}")
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
                ?: return Result.Error("User tidak ditemukan")

            // Validate username uniqueness if changed
            if (username != null && username != existingUser.username) {
                val userWithSameUsername = penggunaRepository.getPenggunaByUsername(username)
                if (userWithSameUsername != null) {
                    return Result.Error("Username sudah digunakan")
                }
            }

            val updatedUser = existingUser.copy(
                username = username ?: existingUser.username,
                role = role?.name ?: existingUser.role,
                updatedAt = java.util.Date()
            )

            penggunaRepository.updatePengguna(updatedUser)
        } catch (e: Exception) {
            Result.Error("Gagal update user: ${e.message}")
        }
    }

    override suspend fun deleteUser(userId: Long, deletedBy: Long): Result<Unit> {
        return try {
            // Check if user exists
            val user = penggunaRepository.getPenggunaById(userId)
                ?: return Result.Error("User tidak ditemukan")

            // Prevent deleting self
            val currentUser = authService.getCurrentUser()
            if (currentUser?.id == userId) {
                return Result.Error("Tidak dapat menghapus user sendiri")
            }

            // Check permissions (only OWNER can delete users)
            if (currentUser?.role != "OWNER") {
                return Result.Error("Hanya Owner yang dapat menghapus user")
            }

            penggunaRepository.deletePengguna(userId)
        } catch (e: Exception) {
            Result.Error("Gagal menghapus user: ${e.message}")
        }
    }

    override suspend fun resetUserPassword(
        userId: Long,
        newPassword: String,
        resetBy: Long
    ): Result<Unit> {
        return try {
            if (newPassword.length < 6) {
                return Result.Error("Password minimal 6 karakter")
            }

            val passwordHash = authService.hashPassword(newPassword)
            penggunaRepository.updatePassword(userId, passwordHash)
        } catch (e: Exception) {
            Result.Error("Gagal reset password: ${e.message}")
        }
    }

    override suspend fun deactivateUser(userId: Long, deactivatedBy: Long): Result<Unit> {
        return try {
            // Check permissions
            val currentUser = authService.getCurrentUser()
            if (currentUser?.role != "OWNER") {
                return Result.Error("Hanya Owner yang dapat menonaktifkan user")
            }

            // Prevent deactivating self
            if (currentUser.id == userId) {
                return Result.Error("Tidak dapat menonaktifkan user sendiri")
            }

            // For now, just mark as inactive (future: add isActive field)
            // Since we don't have isActive field yet, this is a placeholder
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Gagal menonaktifkan user: ${e.message}")
        }
    }

    override suspend fun activateUser(userId: Long, activatedBy: Long): Result<Unit> {
        return try {
            // Check permissions
            val currentUser = authService.getCurrentUser()
            if (currentUser?.role != "OWNER") {
                return Result.Error("Hanya Owner yang dapat mengaktifkan user")
            }

            // For now, just mark as active (future: add isActive field)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Gagal mengaktifkan user: ${e.message}")
        }
    }
}