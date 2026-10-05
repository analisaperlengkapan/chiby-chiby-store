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
     * can revoke an unattended session instead of trusting it forever. An absent
     * session is a *confirmed* revocation — the stored row is the authority, not
     * the in-memory user.
     *
     * The check is bound to the user it starts for. If a newer login replaces
     * the current user while a (suspending) read is in flight, the older result
     * is discarded and the new user's identity and freshly created session are
     * left alone — a stale check must not sign out the user who just logged in.
     * It is also non-throwing: a transient read failure leaves the user signed in
     * and simply retries — but only while the session was verified within the
     * idle window, so a sustained database failure cannot keep protected screens
     * reachable forever. A *successful* read that returns no row revokes
     * regardless of earlier read failures.
     *
     * @return true when the current user's session is (still) usable, false when
     *   it is absent or idle past the timeout and access has been revoked.
     */
    suspend fun enforceIdleTimeout(): Boolean
}
