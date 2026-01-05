package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.UserDao
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.local.entity.User
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun getUserById(id: Long): Result<User> {
        return try {
            val user = userDao.getUserById(id)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("User with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getUserById", e))
        }
    }

    suspend fun getUserByUsername(username: String): Result<User> {
        return try {
            val user = userDao.getUserByUsername(username)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("User with username $username not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getUserByUsername", e))
        }
    }

    fun getUsersByRole(role: Role): Flow<List<User>> = userDao.getUsersByRole(role)

    fun searchUsers(query: String): Flow<List<User>> = userDao.searchUsers(query)

    suspend fun createUser(user: User): Result<Long> {
        return try {
            validateUserData(user)
            val existingUser = userDao.getUserByUsername(user.username)
            if (existingUser != null) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username ${user.username} already exists"))
            }
            val id = userDao.insertUser(user)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createUser", e))
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            validateUserData(user)
            userDao.getUserById(user.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("User with ID ${user.id} not found"))

            val userWithSameUsername = userDao.getUserByUsername(user.username)
            if (userWithSameUsername != null && userWithSameUsername.id != user.id) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username ${user.username} already taken"))
            }

            userDao.updateUser(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateUser", e))
        }
    }

    suspend fun deleteUser(id: Long): Result<Unit> {
        return try {
            val user = userDao.getUserById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("User not found"))

            if (user.role == Role.OWNER) {
                val users = userDao.getAllUsers().first()
                val ownerCount = users.count { it.role == Role.OWNER }
                if (ownerCount <= 1) {
                    return Result.failure(ChibyChibyException.ValidationError("role", "Cannot delete the last Owner account"))
                }
            }

            userDao.deleteUserById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteUser", e))
        }
    }

    suspend fun getUserCount(): Result<Int> {
        return try {
            val count = userDao.getUserCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getUserCount", e))
        }
    }

    suspend fun countActiveUsers(): Result<Int> {
        return try {
            val count = userDao.countActiveUsers()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("countActiveUsers", e))
        }
    }

    suspend fun countByRole(role: Role): Result<Int> {
        return try {
            val count = userDao.countByRole(role)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("countByRole", e))
        }
    }

    private fun validateUserData(user: User) {
        if (user.username.isBlank()) {
            throw ChibyChibyException.ValidationError("username", "Username cannot be empty")
        }
        if (user.username.length < 3) {
            throw ChibyChibyException.ValidationError("username", "Username must be at least 3 characters")
        }
    }
}
