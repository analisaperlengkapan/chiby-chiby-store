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
     * Re-checks the current user's stored session against the idle timeout and
     * enforces it: an absent or idle session is closed and the user signed out,
     * so callers that hold protected state (the navigation graph, [AuthGuard])
     * can revoke an unattended session instead of trusting it forever.
     *
     * The check is bound to the user it starts for. If a newer login replaces
     * the current user while the (suspending) session read is in flight, the
     * older result is discarded and the new user's session is left alone — a
     * stale check must not sign out the user who just logged in. It is also
     * non-throwing: a transient read failure leaves the user signed in and
     * simply retries on the next call — until the session has gone unverified
     * for longer than the idle timeout, at which point it is revoked rather than
     * defended indefinitely (a sustained database failure must not keep
     * protected screens reachable forever).
     *
     * @return true when the current user's session is (still) usable, false when
     *   it is absent or idle past the timeout and access has been revoked.
     */
    suspend fun enforceIdleTimeout(): Boolean
}
