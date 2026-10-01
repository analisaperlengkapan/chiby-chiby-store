package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PenggunaSessionDao
import com.chibychibystore.data.local.entity.PenggunaSession
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
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
     * Snapshot of every active session, newest activity first.
     */
    suspend fun getActiveSessions(): Result<List<PenggunaSession>> {
        return try {
            Result.success(penggunaSessionDao.getActiveSessions())
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getActiveSessions", e))
        }
    }

    /**
     * The most recently used active session for a user, or null when they have
     * none. Used to re-check the idle timeout against the stored session.
     */
    suspend fun getActiveSessionForUser(userId: Long): Result<PenggunaSession?> {
        return try {
            Result.success(penggunaSessionDao.getActiveSessionForUser(userId))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getActiveSessionForUser", e))
        }
    }

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
     * Deactivate a single session row.
     */
    suspend fun deactivateSession(sessionId: Long): Result<Unit> {
        return try {
            penggunaSessionDao.deactivateSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deactivateSession", e))
        }
    }

    /**
     * Reactivate a session and stamp it with the current time. Used when a
     * restore picks an existing session so the chosen row is the most recently
     * used one from then on.
     */
    suspend fun activateSession(sessionId: Long): Result<Unit> {
        return try {
            penggunaSessionDao.activateSession(sessionId, Date())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("activateSession", e))
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
