package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.UserSessionDao
import com.chibychibystore.data.local.entity.UserSession
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data UserSession (PenggunaSession)
 */
@Singleton
class PenggunaSessionRepository @Inject constructor(
    private val userSessionDao: UserSessionDao
) {

    /**
     * Get active session
     */
    suspend fun getActiveSession(): Result<UserSession?> {
        return try {
            val session = userSessionDao.getActiveSession()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getActiveSession", e))
        }
    }

    /**
     * Get active session for specific user
     */
    suspend fun getActiveSessionForUser(userId: Long): Result<UserSession?> {
        return try {
            val session = userSessionDao.getActiveSessionForUser(userId)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getActiveSessionForUser", e))
        }
    }

    /**
     * Get all active sessions
     */
    fun getAllActiveSessions(): Flow<List<UserSession>> = userSessionDao.getAllActiveSessions()

    /**
     * Create new session
     */
    suspend fun createSession(session: UserSession): Result<Long> {
        return try {
            val id = userSessionDao.insertSession(session)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createSession", e))
        }
    }

    /**
     * Update session
     */
    suspend fun updateSession(session: UserSession): Result<Unit> {
        return try {
            userSessionDao.updateSession(session)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateSession", e))
        }
    }

    /**
     * Deactivate all sessions for a user
     */
    suspend fun deactivateUserSessions(userId: Long): Result<Unit> {
        return try {
            userSessionDao.deactivateUserSessions(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deactivateUserSessions", e))
        }
    }

    /**
     * Deactivate all sessions
     */
    suspend fun deactivateAllSessions(): Result<Unit> {
        return try {
            userSessionDao.deactivateAllSessions()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deactivateAllSessions", e))
        }
    }

    /**
     * Update last activity time
     */
    suspend fun updateLastActivityTime(sessionId: Long): Result<Unit> {
        return try {
            userSessionDao.updateLastActivityTime(sessionId, Date())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateLastActivityTime", e))
        }
    }

    /**
     * Clean up old sessions (older than specified days)
     */
    suspend fun cleanupOldSessions(daysOld: Int = 30): Result<Unit> {
        return try {
            val cutoffDate = Date(System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L))
            userSessionDao.deleteOldSessions(cutoffDate)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("cleanupOldSessions", e))
        }
    }
}