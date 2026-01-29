package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PenggunaSessionDao
import com.chibychibystore.data.local.entity.PenggunaSession
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data PenggunaSession
 */
@Singleton
class PenggunaSessionRepository @Inject constructor(
    private val penggunaSessionDao: PenggunaSessionDao
) {

    /**
     * Get active session
     */
    suspend fun getActiveSession(): Result<PenggunaSession?> {
        return try {
            val session = penggunaSessionDao.getActiveSession()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getActiveSession", e))
        }
    }

    /**
     * Get active session for specific user
     */
    suspend fun getActiveSessionForUser(userId: Long): Result<PenggunaSession?> {
        return try {
            val session = penggunaSessionDao.getActiveSessionForUser(userId)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getActiveSessionForUser", e))
        }
    }

    /**
     * Get all active sessions
     */
    fun getAllActiveSessions(): Flow<List<PenggunaSession>> = penggunaSessionDao.getAllActiveSessions()

    /**
     * Create new session
     */
    suspend fun createSession(session: PenggunaSession): Result<Long> {
        return try {
            val id = penggunaSessionDao.insertSession(session)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createSession", e))
        }
    }

    /**
     * Update session
     */
    suspend fun updateSession(session: PenggunaSession): Result<Unit> {
        return try {
            penggunaSessionDao.updateSession(session)
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
            penggunaSessionDao.deactivateUserSessions(userId)
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
            penggunaSessionDao.deactivateAllSessions()
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
            penggunaSessionDao.updateLastActivityTime(sessionId, Date())
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
            penggunaSessionDao.deleteOldSessions(cutoffDate)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("cleanupOldSessions", e))
        }
    }
}