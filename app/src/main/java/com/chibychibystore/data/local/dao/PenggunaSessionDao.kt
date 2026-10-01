package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chibychibystore.data.local.entity.PenggunaSession

@Dao
interface PenggunaSessionDao {
    @Query("SELECT * FROM user_sessions WHERE isActive = 1 ORDER BY lastActivityTime DESC")
    suspend fun getActiveSessions(): List<PenggunaSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PenggunaSession): Long

    @Query("UPDATE user_sessions SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateUserSessions(userId: Long)

    @Query("UPDATE user_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun deactivateSession(sessionId: Long)

    @Query("UPDATE user_sessions SET isActive = 1, lastActivityTime = :time WHERE id = :sessionId")
    suspend fun activateSession(sessionId: Long, time: java.util.Date)

    @Query("DELETE FROM user_sessions WHERE loginTime < :cutoffDate")
    suspend fun deleteOldSessions(cutoffDate: java.util.Date)
}
