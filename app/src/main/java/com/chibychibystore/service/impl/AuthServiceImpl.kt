package com.chibychibystore.service.impl

import com.chibychibystore.service.AuthService
import com.chibychibystore.repository.PenggunaSessionRepository
import com.chibychibystore.data.local.entity.PenggunaSession
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import com.chibychibystore.data.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthServiceImpl @Inject constructor(
    private val penggunaDao: PenggunaDao,
    private val penggunaSessionRepository: PenggunaSessionRepository
) : AuthService {

    private val currentUser = MutableStateFlow<Pengguna?>(null)

    /**
     * Bumped on every login and logout. A session check that suspends (Room read)
     * captures the revision it started under and discards its result if the
     * revision moved on — the user changed identity meanwhile.
     */
    private val sessionRevision = AtomicLong(0)

    /**
     * Wall-clock millis of the last time a session was confirmed usable, or null
     * while no identity is established. A transient read failure does not clear
     * the user, but if the session stays unverifiable for longer than the idle
     * timeout we can no longer claim it is valid and revoke it (fail closed)
     * rather than keep protected screens reachable indefinitely.
     */
    private var lastVerifiedMillis: Long? = null

    /**
     * Wall clock, read through a field so tests can pin "now" and exercise the
     * idle timeout without sleeping. Production always uses [System.currentTimeMillis].
     */
    internal var now: () -> Long = System::currentTimeMillis

    override suspend fun login(username: String, password: String): Result<Pengguna> {
        return try {
            if (username.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username tidak boleh kosong"))
            }
            if (password.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("password", "Password tidak boleh kosong"))
            }

            val user = penggunaDao.getPenggunaByUsername(username)
                ?: return Result.failure(ChibyChibyException.AuthenticationError("Username atau password salah"))

            val passwordHash = hashPassword(password)
            if (user.passwordHash != passwordHash) {
                return Result.failure(ChibyChibyException.AuthenticationError("Username atau password salah"))
            }

            if (!user.isActive) {
                return Result.failure(ChibyChibyException.AuthenticationError("Akun Anda telah dinonaktifkan. Silakan hubungi admin."))
            }

            // Persist the session before publishing the user. observeCurrentUser
            // re-checks the session against the database, so emitting first would
            // briefly expose a user whose session row does not exist yet — and a
            // failed insert would leave the caller authenticated with nothing to
            // expire. The insert is authoritative: no session row, no login.
            val session = PenggunaSession(
                userId = user.id,
                loginTime = java.util.Date(now()),
                lastActivityTime = java.util.Date(now()),
                isActive = true
            )
            val sessionResult = penggunaSessionRepository.createSession(session)
            if (sessionResult.isFailure) {
                return Result.failure(
                    sessionResult.exceptionOrNull()
                        ?: ChibyChibyException.DatabaseError("login", IllegalStateException("Sesi gagal dibuat"))
                )
            }

            // Establish the in-flight window *before* publishing the user, so a
            // session check for the previous user cannot land in between and
            // mistake this login for its own. The newly created session row is
            // authoritative, so this identity starts verified.
            sessionRevision.incrementAndGet()
            lastVerifiedMillis = now()
            currentUser.value = user
            Result.success(user)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("login", e))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val user = currentUser.value
            if (user != null) {
                penggunaSessionRepository.deactivateUserSessions(user.id)
            }
            // Stop any in-flight check from acting on the now-signed-out identity.
            sessionRevision.incrementAndGet()
            lastVerifiedMillis = null
            currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("logout", e))
        }
    }

    override suspend fun getCurrentUser(): Pengguna? {
        return currentUser.value
    }

    override suspend fun hasPermission(permission: String): Boolean {
        val user = currentUser.value ?: return false
        if (user.role == Role.OWNER) return true

        return when (user.role) {
            Role.OWNER -> true
            Role.MANAGER -> hasManagerPermission(permission)
            Role.CASHIER -> hasCashierPermission(permission)
            Role.WAREHOUSE -> hasGudangPermission(permission)
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = currentUser.value
                ?: return Result.failure(ChibyChibyException.AuthenticationError("Tidak ada user yang login"))

            if (oldPassword.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("oldPassword", "Password lama tidak boleh kosong"))
            }
            if (newPassword.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("newPassword", "Password baru tidak boleh kosong"))
            }
            if (newPassword.length < 6) {
                return Result.failure(ChibyChibyException.ValidationError("newPassword", "Password minimal 6 karakter"))
            }

            val oldPasswordHash = hashPassword(oldPassword)
            if (user.passwordHash != oldPasswordHash) {
                return Result.failure(ChibyChibyException.AuthenticationError("Password lama salah"))
            }

            val newPasswordHash = hashPassword(newPassword)
            val updatedUser = user.copy(
                passwordHash = newPasswordHash,
                updatedAt = java.util.Date()
            )

            penggunaDao.updatePengguna(updatedUser)
            currentUser.value = updatedUser

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("changePassword", e))
        }
    }

    override fun observeCurrentUser(): Flow<Pengguna?> {
        // Every emitted value is re-checked against the stored session, but the
        // check is keyed by *identity*: a profile edit (changePassword replaces
        // the user object but keeps the id) is passed straight through without a
        // second session round-trip, while a real identity change — login as a
        // different user, or the logout that clears the user — re-runs it. The
        // stored session is the authority, so what a check leaves in [currentUser]
        // is what is emitted; when it revokes, the null is emitted too.
        return flow {
            val seen = mutableMapOf<Long, Pengguna>()
            currentUser.asStateFlow().collect { user ->
                emit(
                    when {
                        user == null -> null
                        seen[user.id] == user -> user
                        seen.containsKey(user.id) -> user.also { seen[user.id] = it }
                        else -> {
                            enforceIdleTimeout()
                            currentUser.value?.also { if (it.id == user.id) seen[user.id] = it }
                        }
                    }
                )
            }
        }.distinctUntilChanged()
    }

    override suspend fun enforceIdleTimeout(): Boolean {
        val user = currentUser.value ?: return false
        val startedUser = user.id

        // Capture the identity this pass is for. A login/logout bumps the
        // revision; if that happened while a read below is suspended, the result
        // belongs to a user who is no longer current and must be ignored — a
        // slow check for A must not sign out B or close a session B just created.
        val startedRevision = sessionRevision.get()

        // Confirm the stored session is still present, still this identity, and
        // still fresh. A failed read is not an expiry: keep the user signed in
        // and retry — but only while the session was verified recently. A
        // *successful* read that returns no row is not a failure to decide: the
        // session is confirmed gone, so revoke even if the last check failed.
        val sessionResult = penggunaSessionRepository.getActiveSessionForUser(startedUser)
        val session = sessionResult.getOrNull()
        if (session == null) {
            // Tolerate a transient read failure while the session was verified
            // recently; otherwise the absence is confirmed (or the failure has
            // outlived the idle window) and the session is revoked. There is no
            // row to close in that case.
            if (sessionResult.isFailure && verifiedRecently()) return true
            return revokeSession(startedUser, startedRevision, sessionId = null)
        }

        if (!stillCurrent(startedUser, startedRevision)) return true

        val expired = now() - session.lastActivityTime.time > SESSION_TIMEOUT_MS
        if (expired) return revokeSession(startedUser, startedRevision, sessionId = session.id)

        lastVerifiedMillis = now()
        return true
    }

    /**
     * True while the session was verified within the idle window. The window is
     * reset on every login, logout and revocation, so it can never outlive the
     * identity it was recorded for.
     */
    private fun verifiedRecently(): Boolean {
        val verifiedAt = lastVerifiedMillis ?: return false
        return now() - verifiedAt <= SESSION_TIMEOUT_MS
    }

    /** True while [userId] is still the current identity at [revision]. */
    private fun stillCurrent(userId: Long, revision: Long): Boolean =
        currentUser.value?.id == userId && sessionRevision.get() == revision

    /**
     * Closes the session row that was found idle and signs the user out — but
     * only if nothing changed since the check started.
     *
     * The caller passes the exact [sessionId] it decided on, so there is no
     * second read here: a same-account login that lands after that decision
     * creates a fresh row with a different id, and closing the decided row cannot
     * touch it. Re-reading "the newest active row" instead would race that login —
     * the re-read could return the row the login just created and deactivate it.
     *
     * A row with [sessionId] `null` means the session was already confirmed
     * absent (or the read failed past the idle window); there is nothing to
     * deactivate, only the identity to clear.
     *
     * Re-checking the identity and revision *after* the (suspending) deactivate is
     * what stops a login that completed meanwhile from being signed out: its
     * identity is left alone and its session row is not cleared.
     *
     * @return true when a newer login (or logout) has superseded this check, so
     *   the caller must not treat the session as revoked; false when the user was
     *   actually signed out.
     */
    private suspend fun revokeSession(userId: Long, startedRevision: Long, sessionId: Long?): Boolean {
        if (!stillCurrent(userId, startedRevision)) return true

        if (sessionId != null) {
            penggunaSessionRepository.deactivateSession(sessionId)
        }

        // Re-check after the suspension: if a login/logout happened meanwhile,
        // leave the new identity (and its freshly created session) alone.
        if (!stillCurrent(userId, startedRevision)) return true

        sessionRevision.incrementAndGet()
        lastVerifiedMillis = null
        currentUser.value = null
        return false
    }

    override suspend fun initializeSession(): Result<Unit> {
        return try {
            // Consider every active session, most recently used first. Several
            // rows can be active at once (login never closes an earlier user's
            // session), and the newest loginTime can belong to a session that has
            // gone idle while an older row is still fresh. Restoring only by
            // loginTime discarded the fresh row and forced a needless re-login.
            val sessionsResult = penggunaSessionRepository.getActiveSessions()
            val sessions = sessionsResult.getOrNull()
                ?: throw sessionsResult.exceptionOrNull() ?: IllegalStateException("Gagal membaca sesi")
            val currentTime = now()

            var restored = false
            for (session in sessions) {
                val idleAge = currentTime - session.lastActivityTime.time
                val user = penggunaDao.getPenggunaById(session.userId)
                if (user != null && user.isActive && idleAge in 0 until SESSION_TIMEOUT_MS) {
                    // Bump before publishing: this is a new identity for any
                    // in-flight check that started while no user was current.
                    sessionRevision.incrementAndGet()
                    lastVerifiedMillis = currentTime
                    currentUser.value = user
                    penggunaSessionRepository.activateSession(session.id)
                    restored = true
                    break
                }
                // Rejected: idle past the timeout, or the user was deleted or
                // deactivated while the session lingered. Close that row so it is
                // not reconsidered on the next launch. Deactivating per row (not
                // per user) matters when the same user also has a still-fresh
                // session further down the list.
                penggunaSessionRepository.deactivateSession(session.id)
            }

            if (!restored) {
                currentUser.value = null
            }

            penggunaSessionRepository.cleanupOldSessions()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("initializeSession", e))
        }
    }

    private fun hasManagerPermission(permission: String): Boolean {
        return when (permission) {
            "VIEW_SALES_REPORTS",
            "VIEW_FINANCIAL_REPORTS",
            "APPROVE_LARGE_TRANSACTIONS",
            "VIEW_INVENTORY",
            "EDIT_INVENTORY",
            "MANAGE_USERS" -> true
            else -> false
        }
    }

    private fun hasCashierPermission(permission: String): Boolean {
        return when (permission) {
            "CREATE_SALES",
            "VIEW_INVENTORY",
            "VIEW_DAILY_SALES_REPORT",
            "VIEW_SALES_REPORTS" -> true
            else -> false
        }
    }

    private fun hasGudangPermission(permission: String): Boolean {
        return when (permission) {
            "VIEW_INVENTORY",
            "EDIT_INVENTORY",
            "MANAGE_WAREHOUSES",
            "PRINT_BARCODE_LABELS",
            "VIEW_INVENTORY_REPORTS" -> true
            else -> false
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    companion object {
        /** A session that has been idle longer than this is no longer valid. */
        internal const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
}
