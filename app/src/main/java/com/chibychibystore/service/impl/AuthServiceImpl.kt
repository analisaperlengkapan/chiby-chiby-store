package com.chibychibystore.service.impl

import com.chibychibystore.service.AuthService
import com.chibychibystore.repository.PenggunaSessionRepository
import com.chibychibystore.data.local.entity.PenggunaSession
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.security.MessageDigest
import com.chibychibystore.data.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthServiceImpl @Inject constructor(
    private val penggunaDao: PenggunaDao,
    private val penggunaSessionRepository: PenggunaSessionRepository
) : AuthService {

    private var currentUser: Pengguna? = null

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

            currentUser = user

            val session = PenggunaSession(
                userId = user.id,
                loginTime = java.util.Date(),
                lastActivityTime = java.util.Date(),
                isActive = true
            )
            penggunaSessionRepository.createSession(session)

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("login", e))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val user = currentUser
            if (user != null) {
                penggunaSessionRepository.deactivateUserSessions(user.id)
            }
            currentUser = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("logout", e))
        }
    }

    override suspend fun getCurrentUser(): Pengguna? {
        return currentUser
    }

    override suspend fun hasPermission(permission: String): Boolean {
        val user = currentUser ?: return false
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
            val user = currentUser
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
            currentUser = updatedUser

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("changePassword", e))
        }
    }

    override fun observeCurrentUser(): Flow<Pengguna?> {
        return flowOf(currentUser)
    }

    override suspend fun initializeSession(): Result<Unit> {
        return try {
            val activeSessionRes = penggunaSessionRepository.getActiveSession()
            val activeSession = activeSessionRes.getOrNull()
            if (activeSession != null) {
                val sessionAge = System.currentTimeMillis() - activeSession.loginTime.time
                val maxSessionAge = 24 * 60 * 60 * 1000L // 24 hours

                if (sessionAge < maxSessionAge) {
                    val user = penggunaDao.getPenggunaById(activeSession.userId)
                    if (user != null) {
                        currentUser = user
                        penggunaSessionRepository.updateLastActivityTime(activeSession.id)
                    } else {
                        penggunaSessionRepository.deactivateUserSessions(activeSession.userId)
                    }
                } else {
                    penggunaSessionRepository.deactivateUserSessions(activeSession.userId)
                }
            }

            penggunaSessionRepository.cleanupOldSessions()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("initializeSession", e))
        }
    }

    override suspend fun isSessionExpired(): Boolean {
        val user = currentUser ?: return true
        val activeSession = penggunaSessionRepository.getActiveSessionForUser(user.id).getOrNull()
        if (activeSession == null) return true

        val sessionAge = System.currentTimeMillis() - activeSession.lastActivityTime.time
        val maxSessionAge = 24 * 60 * 60 * 1000L // 24 hours

        return sessionAge > maxSessionAge
    }

    override suspend fun extendSession(): Result<Unit> {
        return try {
            val user = currentUser
                ?: return Result.failure(ChibyChibyException.AuthenticationError("Tidak ada user yang login"))

            val activeSession = penggunaSessionRepository.getActiveSessionForUser(user.id).getOrNull()
                ?: return Result.failure(ChibyChibyException.AuthenticationError("Session tidak ditemukan"))

            penggunaSessionRepository.updateLastActivityTime(activeSession.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("extendSession", e))
        }
    }

    override suspend fun forceLogoutAll(): Result<Unit> {
        return try {
            penggunaSessionRepository.deactivateAllSessions()
            currentUser = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("forceLogoutAll", e))
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
}
