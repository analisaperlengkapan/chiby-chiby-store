package com.chibychibystore.service.impl

import com.chibychibystore.data.model.Result
import com.chibychibystore.constant.Permissions
import com.chibychibystore.service.UserManagementService
import com.chibychibystore.service.UserStats
import com.chibychibystore.data.local.entity.User
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.UserRepository
import com.chibychibystore.repository.UserSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManagementServiceImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val userSessionRepository: UserSessionRepository,
    private val authService: AuthService
) : UserManagementService {

    override fun getAllUsers(): Flow<List<User>> =
        userRepository.getAllUsers()

    override suspend fun getUserById(userId: Long): Result<User> {
        return try {
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat detail user"))
            }
            userRepository.getUserById(userId)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getUserById", e))
        }
    }

    override suspend fun getUserStats(): Result<UserStats> {
        return try {
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat statistik user"))
            }
            val allUsers = userRepository.getAllUsers().first()
            val totalUsers = allUsers.size
            val activeUsers = allUsers.count { it.isActive }
            val owners = allUsers.count { it.role == Role.OWNER }
            val managers = allUsers.count { it.role == Role.MANAGER }
            val cashiers = allUsers.count { it.role == Role.CASHIER }
            val warehouseStaff = allUsers.count { it.role == Role.WAREHOUSE }

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

    override fun getUsersByRole(role: Role): Flow<List<User>> =
        userRepository.getUsersByRole(role)

    override fun searchUsers(query: String): Flow<List<User>> =
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
                return Result.failure(Exception("Tidak memiliki izin untuk membuat user"))
            }
            // Validate input
            validateUserData(username, password)

            // Check if username already exists
            val existingUserResult = userRepository.getUserByUsername(username)
            if (existingUserResult.isSuccess) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username sudah digunakan"))
            }

            // Hash password using SHA-256
            val passwordHash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(password.toByteArray())
                .joinToString("") { "%02x".format(it) }

            val user = User(
                username = username,
                passwordHash = passwordHash,
                role = role,
                permissions = "[]", // Default empty permissions
                createdAt = java.util.Date(),
                updatedAt = java.util.Date()
            )

            return userRepository.createUser(user)
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
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengupdate user"))
            }

            // Validate partial updates
            if (username != null && username.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username tidak boleh kosong"))
            }

            // Get existing user
            val existingUserResult = userRepository.getUserById(userId)
            val existingUser = existingUserResult.getOrNull()
                ?: return Result.failure(ChibyChibyException.DatabaseError("User tidak ditemukan"))

            // Validate username uniqueness if changed
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

            val updateResult = userRepository.updateUser(updatedUser)
            if (updateResult.isSuccess) {
                Result.success(Unit)
            } else {
                val cause = updateResult.exceptionOrNull()
                Result.failure(
                    cause ?: Exception("Gagal update user")
                )
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal update user", e))
        }
    }

    override suspend fun deleteUser(userId: Long, deletedBy: Long): Result<Unit> {
        return try {
            // Check if user exists
            val userResult = userRepository.getUserById(userId)
            if (userResult.isFailure) {
                return Result.failure(Exception("User tidak ditemukan"))
            }

            // Prevent deleting self
            val currentUser = authService.getCurrentUser()
            if (currentUser?.id == userId) {
                return Result.failure(Exception("Tidak dapat menghapus user sendiri"))
            }

            // Check permissions (only OWNER can delete users)
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk menghapus user"))
            }
            // Optional: prevent deleting another owner/manager if not owner
            // but for now MANAGE_USERS is enough for simplicity as per implementation plan.

            val deleteResult = userRepository.deleteUser(userId)
            if (deleteResult.isSuccess) {
                Result.success(Unit)
            } else {
                val cause = deleteResult.exceptionOrNull()
                Result.failure(
                    cause ?: Exception("Gagal menghapus user")
                )
            }
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
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk mereset password"))
            }
            if (newPassword.length < 6) {
                return Result.failure(Exception("Password minimal 6 karakter"))
            }

            val userResult = userRepository.getUserById(userId)
            val user = userResult.getOrNull() ?: return Result.failure(Exception("User tidak ditemukan"))

            val passwordHash = MessageDigest.getInstance("SHA-256")
                .digest(newPassword.toByteArray())
                .joinToString("") { "%02x".format(it) }

            val updatedUser = user.copy(
                passwordHash = passwordHash,
                updatedAt = java.util.Date()
            )

            val updateResult = userRepository.updateUser(updatedUser)
            if (updateResult.isSuccess) {
                Result.success(Unit)
            } else {
                val cause = updateResult.exceptionOrNull()
                Result.failure(
                    cause ?: Exception("Gagal reset password")
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal reset password: ${e.message}"))
        }
    }

    override suspend fun deactivateUser(userId: Long, deactivatedBy: Long): Result<Unit> {
        return try {
            // Check permissions
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk menonaktifkan user"))
            }
            val currentUser = authService.getCurrentUser()

            // Prevent deactivating self
            if (currentUser?.id == userId) {
                return Result.failure(Exception("Tidak dapat menonaktifkan user sendiri"))
            }

            val userResult = userRepository.getUserById(userId)
            val user = userResult.getOrNull() ?: return Result.failure(Exception("User tidak ditemukan"))

            val updatedUser = user.copy(isActive = false, updatedAt = java.util.Date())
            userRepository.updateUser(updatedUser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menonaktifkan user: ${e.message}"))
        }
    }

    override suspend fun activateUser(userId: Long, activatedBy: Long): Result<Unit> {
        return try {
            // Check permissions
            if (!authService.hasPermission(Permissions.MANAGE_USERS)) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengaktifkan user"))
            }

            val userResult = userRepository.getUserById(userId)
            val user = userResult.getOrNull() ?: return Result.failure(Exception("User tidak ditemukan"))

            val updatedUser = user.copy(isActive = true, updatedAt = java.util.Date())
            userRepository.updateUser(updatedUser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mengaktifkan user: ${e.message}"))
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
            throw ChibyChibyException.ValidationError("password", "Password minimal 6 karakter")
        }
    }
}