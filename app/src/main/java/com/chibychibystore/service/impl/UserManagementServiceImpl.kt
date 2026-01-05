package com.chibychibystore.service.impl

import com.chibychibystore.data.model.Result
import com.chibychibystore.constant.Permissions
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.UserManagementService
import com.chibychibystore.service.UserStats
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PenggunaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManagementServiceImpl @Inject constructor(
    private val userRepository: PenggunaRepository,
    private val authService: AuthService
) : UserManagementService {

    override fun getAllUsers(): Flow<List<Pengguna>> =
        userRepository.getAllUsers()

    override suspend fun getUserById(userId: Long): Result<Pengguna> {
        return try {
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat detail pengguna"))
            }
            userRepository.getUserById(userId)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getUserById", e))
        }
    }

    override suspend fun getUserStats(): Result<UserStats> {
        return try {
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat statistik pengguna"))
            }

            val totalUsers = userRepository.getUserCount().getOrNull() ?: 0
            val activeUsers = userRepository.countActiveUsers().getOrNull() ?: 0
            val owners = userRepository.countByRole(Role.OWNER).getOrNull() ?: 0
            val managers = userRepository.countByRole(Role.MANAGER).getOrNull() ?: 0
            val cashiers = userRepository.countByRole(Role.CASHIER).getOrNull() ?: 0
            val warehouseStaff = userRepository.countByRole(Role.WAREHOUSE).getOrNull() ?: 0

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
        userRepository.getUsersByRole(role)

    override fun searchUsers(query: String): Flow<List<Pengguna>> =
        userRepository.searchUsers(query)

    override fun canDeleteLastOwner(): Flow<Boolean> {
        return userRepository.getAllUsers().map { users ->
            users.count { it.role == Role.OWNER } > 1
        }
    }

    override suspend fun createUser(
        username: String,
        password: String,
        role: Role,
        createdBy: Long
    ): Result<Long> {
        return try {
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk membuat pengguna"))
            }
            // Validate input
            validateUserData(username, password)

            // Check if username already exists
            val existingUserResult = userRepository.getUserByUsername(username)
            if (existingUserResult.isSuccess) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username sudah digunakan"))
            }

            // Hash password using SHA-256
            val passwordHash = MessageDigest.getInstance("SHA-256")
                .digest(password.toByteArray())
                .joinToString("") { "%02x".format(it) }

            val user = Pengguna(
                username = username,
                passwordHash = passwordHash,
                role = role,
                createdAt = java.util.Date(),
                updatedAt = java.util.Date()
            )

            userRepository.createPengguna(user)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal membuat pengguna", e))
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
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk memperbarui pengguna"))
            }

            if (username != null && username.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username tidak boleh kosong"))
            }

            val existingUserResult = userRepository.getUserById(userId)
            val existingUser = existingUserResult.getOrNull()
                ?: return Result.failure(ChibyChibyException.DatabaseError("Pengguna tidak ditemukan"))

            if (username != null && username != existingUser.username) {
                val userWithSameUsernameResult = userRepository.getUserByUsername(username)
                if (userWithSameUsernameResult.isSuccess) {
                    return Result.failure(ChibyChibyException.ValidationError("username", "Username sudah digunakan"))
                }
            }

            val updatedUser = existingUser.copy(
                username = username ?: existingUser.username,
                role = role ?: existingUser.role,
                isActive = isActive ?: existingUser.isActive,
                updatedAt = java.util.Date()
            )

            userRepository.updateUser(updatedUser)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal memperbarui pengguna", e))
        }
    }

    override suspend fun deleteUser(userId: Long, deletedBy: Long): Result<Unit> {
        return try {
            val userResult = userRepository.getUserById(userId)
            if (userResult.isFailure) {
                return Result.failure(Exception("Pengguna tidak ditemukan"))
            }

            val currentUser = authService.getCurrentUser()
            if (currentUser?.id == userId) {
                return Result.failure(Exception("Tidak dapat menghapus pengguna sendiri"))
            }

            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk menghapus pengguna"))
            }

            userRepository.deleteUser(userId)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghapus pengguna", e))
        }
    }

    override suspend fun resetUserPassword(
        userId: Long,
        newPassword: String,
        resetBy: Long
    ): Result<Unit> {
        return try {
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk mereset kata sandi"))
            }
            if (newPassword.length < 6) {
                return Result.failure(Exception("Kata sandi minimal 6 karakter"))
            }

            val userResult = userRepository.getUserById(userId)
            val user = userResult.getOrNull() ?: return Result.failure(Exception("Pengguna tidak ditemukan"))

            val passwordHash = MessageDigest.getInstance("SHA-256")
                .digest(newPassword.toByteArray())
                .joinToString("") { "%02x".format(it) }

            val updatedUser = user.copy(
                passwordHash = passwordHash,
                updatedAt = java.util.Date()
            )

            userRepository.updateUser(updatedUser)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mereset kata sandi: ${e.message}"))
        }
    }

    override suspend fun deactivateUser(userId: Long, deactivatedBy: Long): Result<Unit> {
        return try {
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk menonaktifkan pengguna"))
            }
            val currentUser = authService.getCurrentUser()

            if (currentUser?.id == userId) {
                return Result.failure(Exception("Tidak dapat menonaktifkan pengguna sendiri"))
            }

            val userResult = userRepository.getUserById(userId)
            val user = userResult.getOrNull() ?: return Result.failure(Exception("Pengguna tidak ditemukan"))

            val updatedUser = user.copy(isActive = false, updatedAt = java.util.Date())
            userRepository.updateUser(updatedUser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menonaktifkan pengguna: ${e.message}"))
        }
    }

    override suspend fun activateUser(userId: Long, activatedBy: Long): Result<Unit> {
        return try {
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengaktifkan pengguna"))
            }

            val userResult = userRepository.getUserById(userId)
            val user = userResult.getOrNull() ?: return Result.failure(Exception("Pengguna tidak ditemukan"))

            val updatedUser = user.copy(isActive = true, updatedAt = java.util.Date())
            userRepository.updateUser(updatedUser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mengaktifkan pengguna: ${e.message}"))
        }
    }

    private fun validateUserData(username: String, password: String?) {
        if (username.isBlank()) {
            throw ChibyChibyException.ValidationError("username", "Username tidak boleh kosong")
        }
        if (username.length < 3) {
            throw ChibyChibyException.ValidationError("username", "Username minimal 3 karakter")
        }
        if (password != null && password.length < 6) {
            throw ChibyChibyException.ValidationError("password", "Kata sandi minimal 6 karakter")
        }
    }
}