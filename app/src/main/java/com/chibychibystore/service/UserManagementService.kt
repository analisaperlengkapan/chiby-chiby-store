package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk User Management Service
 */
interface UserManagementService {
    /**
     * Get all users
     */
    fun getAllUsers(): Flow<List<Pengguna>>

    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: Long): Result<Pengguna>

    /**
     * Get users by role
     */
    fun getUsersByRole(role: Role): Flow<List<Pengguna>>

    /**
     * Search users
     */
    fun searchUsers(query: String): Flow<List<Pengguna>>

    /**
     * Create new user
     */
    suspend fun createUser(
        username: String,
        password: String,
        role: Role,
        createdBy: Long
    ): Result<Long>

    /**
     * Update user
     */
    suspend fun updateUser(
        userId: Long,
        username: String?,
        role: Role?,
        isActive: Boolean?,
        updatedBy: Long
    ): Result<Unit>

    /**
     * Delete user
     */
    suspend fun deleteUser(userId: Long, deletedBy: Long): Result<Unit>

    /**
     * Reset user password
     */
    suspend fun resetUserPassword(
        userId: Long,
        newPassword: String,
        resetBy: Long
    ): Result<Unit>

    /**
     * Deactivate user
     */
    suspend fun deactivateUser(userId: Long, deactivatedBy: Long): Result<Unit>

    /**
     * Activate user
     */
    suspend fun activateUser(userId: Long, activatedBy: Long): Result<Unit>

    /**
     * Get user statistics
     */
    suspend fun getUserStats(): Result<UserStats>

    /**
     * Check if can delete last owner
     */
    fun canDeleteLastOwner(): Flow<Boolean>
}

data class UserStats(
    val totalUsers: Int,
    val activeUsers: Int,
    val owners: Int,
    val managers: Int,
    val cashiers: Int,
    val warehouseStaff: Int
)