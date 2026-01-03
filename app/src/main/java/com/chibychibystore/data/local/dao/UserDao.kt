package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM pengguna ORDER BY username ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM pengguna WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    @Query("SELECT * FROM pengguna WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM pengguna WHERE role = :role ORDER BY username ASC")
    fun getUsersByRole(role: Role): Flow<List<User>>

    @Query("SELECT * FROM pengguna WHERE username LIKE '%' || :query || '%' ORDER BY username ASC")
    fun searchUsers(query: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUserList(userList: List<User>): List<Long>

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM pengguna WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("SELECT COUNT(*) FROM pengguna")
    suspend fun getUserCount(): Int

    @Query("SELECT COUNT(*) FROM pengguna WHERE isActive = 1")
    suspend fun countActiveUsers(): Int

    @Query("SELECT COUNT(*) FROM pengguna WHERE role = :role")
    suspend fun countByRole(role: Role): Int
}
