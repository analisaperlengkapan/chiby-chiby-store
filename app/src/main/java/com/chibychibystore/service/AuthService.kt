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
     * Whether the current user's session is still valid, so callers that hold
     * protected state (the navigation graph, [AuthGuard]) can revoke an
     * unattended session instead of trusting it forever.
     *
     * [VALID] means the stored session is present and not idle past the timeout.
     * [EXPIRED] is a confirmed absence or timeout and the session has been
     * revoked — the caller may clear its state. [UNKNOWN] means the session could
     * not be read (a transient database error); the caller must keep the user
     * signed in and retry rather than sign them out.
     */
    suspend fun sessionStatus(): SessionStatus

    /** Outcome of [sessionStatus]. */
    enum class SessionStatus { VALID, EXPIRED, UNKNOWN }
}
