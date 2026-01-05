package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.UserSessionDao
import com.chibychibystore.data.local.entity.UserSession
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

@Singleton
class UserSessionRepository @Inject constructor(
    private val userSessionDao: UserSessionDao
) {
    suspend fun createSession(session: UserSession) {
        userSessionDao.insertSession(session)
    }

    suspend fun getActiveSession(): Result<UserSession?> {
        return try {
            val session = userSessionDao.getActiveSession()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getActiveSession", e))
        }
    }

    suspend fun getActiveSessionForUser(userId: Long): Result<UserSession?> {
        return try {
            val session = userSessionDao.getActiveSessionForUser(userId)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getActiveSessionForUser", e))
        }
    }

    suspend fun deactivateUserSessions(userId: Long) {
        userSessionDao.deactivateUserSessions(userId)
    }

    suspend fun updateLastActivityTime(sessionId: Long) {
        userSessionDao.updateLastActivityTime(sessionId, Date())
    }

    suspend fun cleanupOldSessions() {
        val cutoffDate = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L) // 7 days
        userSessionDao.deleteOldSessions(cutoffDate)
    }

    suspend fun deactivateAllSessions() {
        userSessionDao.deactivateAllSessions()
    }
}
