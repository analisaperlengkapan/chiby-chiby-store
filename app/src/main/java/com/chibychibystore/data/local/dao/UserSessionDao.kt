package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.UserSession
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_sessions WHERE isActive = 1 ORDER BY loginTime DESC LIMIT 1")
    suspend fun getActiveSession(): UserSession?

    @Query("SELECT * FROM user_sessions WHERE userId = :userId AND isActive = 1 ORDER BY loginTime DESC LIMIT 1")
    suspend fun getActiveSessionForUser(userId: Long): UserSession?

    @Query("SELECT * FROM user_sessions WHERE isActive = 1")
    fun getAllActiveSessions(): Flow<List<UserSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSession): Long

    @Update
    suspend fun updateSession(session: UserSession)

    @Query("UPDATE user_sessions SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateUserSessions(userId: Long)

    @Query("UPDATE user_sessions SET isActive = 0")
    suspend fun deactivateAllSessions()

    @Query("UPDATE user_sessions SET lastActivityTime = :time WHERE id = :sessionId")
    suspend fun updateLastActivityTime(sessionId: Long, time: java.util.Date)

    @Query("DELETE FROM user_sessions WHERE loginTime < :cutoffDate")
    suspend fun deleteOldSessions(cutoffDate: java.util.Date)
}