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
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import com.chibychibystore.data.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthServiceImpl @Inject constructor(
    private val penggunaDao: PenggunaDao,
    private val penggunaSessionRepository: PenggunaSessionRepository
) : AuthService {

    private val currentUser = MutableStateFlow<Pengguna?>(null)

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
                loginTime = java.util.Date(),
                lastActivityTime = java.util.Date(),
                isActive = true
            )
            val sessionResult = penggunaSessionRepository.createSession(session)
            if (sessionResult.isFailure) {
                return Result.failure(
                    sessionResult.exceptionOrNull()
                        ?: ChibyChibyException.DatabaseError("login", IllegalStateException("Sesi gagal dibuat"))
                )
            }

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
        // Re-check the session on every emission. The stored session is the
        // authority: when the idle timeout has passed (or the row was closed),
        // the user is revoked here as well as by the caller's periodic poll, so
        // no consumer of this flow can keep trusting an expired session.
        return currentUser.asStateFlow().map { user ->
            if (user == null) {
                null
            } else if (isSessionExpired()) {
                revokeExpiredSession()
                null
            } else {
                user
            }
        }
    }

    override suspend fun isSessionExpired(): Boolean {
        val user = currentUser.value ?: return true
        val session = penggunaSessionRepository.getActiveSessionForUser(user.id).getOrNull()
            ?: return true
        return System.currentTimeMillis() - session.lastActivityTime.time > SESSION_TIMEOUT_MS
    }

    /** Clears the in-memory user and closes the persisted session for it. */
    private suspend fun revokeExpiredSession() {
        currentUser.value?.let { penggunaSessionRepository.deactivateUserSessions(it.id) }
        currentUser.value = null
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
            val now = System.currentTimeMillis()

            var restored = false
            for (session in sessions) {
                val idleAge = now - session.lastActivityTime.time
                val user = penggunaDao.getPenggunaById(session.userId)
                if (user != null && user.isActive && idleAge in 0 until SESSION_TIMEOUT_MS) {
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
