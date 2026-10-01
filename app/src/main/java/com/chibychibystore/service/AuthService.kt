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
    /**
     * True when there is no authenticated user, or the current user's session has
     * been idle past the timeout. Callers that hold protected state (the
     * navigation graph, [AuthGuard]) poll this while the app is open so an
     * unattended session is revoked instead of being trusted forever.
     */
    suspend fun isSessionExpired(): Boolean
}
