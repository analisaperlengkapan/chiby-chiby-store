package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Authentication Service untuk Chiby Chiby Store
 */
interface AuthService {
    suspend fun login(username: String, password: String): Result<Pengguna>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Pengguna?
    suspend fun hasPermission(permission: String): Boolean
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
    fun observeCurrentUser(): Flow<Pengguna?>
    suspend fun initializeSession(): Result<Unit>
    suspend fun isSessionExpired(): Boolean
    suspend fun extendSession(): Result<Unit>
    suspend fun forceLogoutAll(): Result<Unit>
}
