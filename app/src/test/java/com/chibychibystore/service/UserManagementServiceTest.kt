package com.chibychibystore.service

import com.chibychibystore.data.Result
import com.chibychibystore.data.model.Pengguna
import com.chibychibystore.repository.PenggunaRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class UserManagementServiceTest {

    @Mock
    private lateinit var penggunaRepository: PenggunaRepository

    private lateinit var userManagementService: UserManagementService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        userManagementService = UserManagementServiceImpl(penggunaRepository)
    }

    @Test
    fun `getAllUsers should return list of users successfully`() = runTest {
        // Given
        val mockUsers = listOf(
            Pengguna(id = 1, username = "owner", passwordHash = "hash1", role = "OWNER", permissions = "[]"),
            Pengguna(id = 2, username = "manager", passwordHash = "hash2", role = "MANAGER", permissions = "[]")
        )
        `when`(penggunaRepository.getAllPengguna()).thenReturn(flowOf(mockUsers))

        // When
        val result = userManagementService.getAllUsers()

        // Then
        assertTrue(result is Result.Success)
        val users = (result as Result.Success).data
        assertEquals(2, users.size)
        assertEquals("owner", users[0].username)
        assertEquals("manager", users[1].username)
    }

    @Test
    fun `getAllUsers should return error when repository fails`() = runTest {
        // Given
        `when`(penggunaRepository.getAllPengguna())
            .thenThrow(RuntimeException("Database error"))

        // When
        val result = userManagementService.getAllUsers()

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals("Database error", error.exception.message)
    }

    @Test
    fun `createUser should create user successfully with valid data`() = runTest {
        // Given
        val newUser = Pengguna(
            username = "cashier",
            passwordHash = "hashed_password",
            role = "CASHIER",
            permissions = "[]"
        )
        `when`(penggunaRepository.insertPengguna(any())).thenReturn(1L)

        // When
        val result = userManagementService.createUser("cashier", "password123", "CASHIER")

        // Then
        assertTrue(result is Result.Success)
        val createdUser = (result as Result.Success).data
        assertEquals("cashier", createdUser.username)
        assertEquals("CASHIER", createdUser.role)
        assertNotNull(createdUser.passwordHash)
        assertNotEquals("password123", createdUser.passwordHash) // Should be hashed
    }

    @Test
    fun `createUser should return error for duplicate username`() = runTest {
        // Given
        `when`(penggunaRepository.insertPengguna(any()))
            .thenThrow(RuntimeException("UNIQUE constraint failed: pengguna.username"))

        // When
        val result = userManagementService.createUser("existing_user", "password123", "CASHIER")

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("sudah ada") == true)
    }

    @Test
    fun `createUser should return error for invalid role`() = runTest {
        // When
        val result = userManagementService.createUser("user", "password123", "INVALID_ROLE")

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("role") == true)
    }

    @Test
    fun `createUser should return error for weak password`() = runTest {
        // When
        val result = userManagementService.createUser("user", "123", "CASHIER")

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("password") == true)
    }

    @Test
    fun `updateUser should update user successfully with valid data`() = runTest {
        // Given
        val existingUser = Pengguna(
            id = 1,
            username = "old_username",
            passwordHash = "old_hash",
            role = "CASHIER",
            permissions = "[]"
        )
        `when`(penggunaRepository.getPenggunaById(1)).thenReturn(existingUser)
        `when`(penggunaRepository.updatePengguna(any())).thenReturn(1)

        // When
        val result = userManagementService.updateUser(1, "new_username", "MANAGER")

        // Then
        assertTrue(result is Result.Success)
        val updatedUser = (result as Result.Success).data
        assertEquals("new_username", updatedUser.username)
        assertEquals("MANAGER", updatedUser.role)
    }

    @Test
    fun `updateUser should return error for non-existent user`() = runTest {
        // Given
        `when`(penggunaRepository.getPenggunaById(999)).thenReturn(null)

        // When
        val result = userManagementService.updateUser(999, "username", "CASHIER")

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tidak ditemukan") == true)
    }

    @Test
    fun `updateUser should return error for duplicate username`() = runTest {
        // Given
        val existingUser = Pengguna(
            id = 1,
            username = "user1",
            passwordHash = "hash",
            role = "CASHIER",
            permissions = "[]"
        )
        `when`(penggunaRepository.getPenggunaById(1)).thenReturn(existingUser)
        `when`(penggunaRepository.updatePengguna(any()))
            .thenThrow(RuntimeException("UNIQUE constraint failed: pengguna.username"))

        // When
        val result = userManagementService.updateUser(1, "existing_username", "CASHIER")

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("sudah ada") == true)
    }

    @Test
    fun `deleteUser should delete user successfully`() = runTest {
        // Given
        val existingUser = Pengguna(
            id = 1,
            username = "user",
            passwordHash = "hash",
            role = "CASHIER",
            permissions = "[]"
        )
        `when`(penggunaRepository.getPenggunaById(1)).thenReturn(existingUser)
        `when`(penggunaRepository.deletePengguna(1)).thenReturn(1)

        // When
        val result = userManagementService.deleteUser(1)

        // Then
        assertTrue(result is Result.Success)
        val success = (result as Result.Success).data
        assertTrue(success)
    }

    @Test
    fun `deleteUser should return error for non-existent user`() = runTest {
        // Given
        `when`(penggunaRepository.getPenggunaById(999)).thenReturn(null)

        // When
        val result = userManagementService.deleteUser(999)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tidak ditemukan") == true)
    }

    @Test
    fun `deleteUser should return error for owner user deletion attempt`() = runTest {
        // Given
        val ownerUser = Pengguna(
            id = 1,
            username = "owner",
            passwordHash = "hash",
            role = "OWNER",
            permissions = "[]"
        )
        `when`(penggunaRepository.getPenggunaById(1)).thenReturn(ownerUser)

        // When
        val result = userManagementService.deleteUser(1)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("OWNER") == true)
    }

    @Test
    fun `changePassword should change password successfully with valid data`() = runTest {
        // Given
        val existingUser = Pengguna(
            id = 1,
            username = "user",
            passwordHash = "old_hash",
            role = "CASHIER",
            permissions = "[]"
        )
        `when`(penggunaRepository.getPenggunaById(1)).thenReturn(existingUser)
        `when`(penggunaRepository.updatePengguna(any())).thenReturn(1)

        // When
        val result = userManagementService.changePassword(1, "new_password123")

        // Then
        assertTrue(result is Result.Success)
        val success = (result as Result.Success).data
        assertTrue(success)
    }

    @Test
    fun `changePassword should return error for non-existent user`() = runTest {
        // Given
        `when`(penggunaRepository.getPenggunaById(999)).thenReturn(null)

        // When
        val result = userManagementService.changePassword(999, "new_password")

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tidak ditemukan") == true)
    }

    @Test
    fun `changePassword should return error for weak password`() = runTest {
        // Given
        val existingUser = Pengguna(
            id = 1,
            username = "user",
            passwordHash = "hash",
            role = "CASHIER",
            permissions = "[]"
        )
        `when`(penggunaRepository.getPenggunaById(1)).thenReturn(existingUser)

        // When
        val result = userManagementService.changePassword(1, "123")

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("password") == true)
    }

    @Test
    fun `getUserById should return user successfully`() = runTest {
        // Given
        val mockUser = Pengguna(
            id = 1,
            username = "user",
            passwordHash = "hash",
            role = "CASHIER",
            permissions = "[]"
        )
        `when`(penggunaRepository.getPenggunaById(1)).thenReturn(mockUser)

        // When
        val result = userManagementService.getUserById(1)

        // Then
        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("user", user.username)
        assertEquals("CASHIER", user.role)
    }

    @Test
    fun `getUserById should return error for non-existent user`() = runTest {
        // Given
        `when`(penggunaRepository.getPenggunaById(999)).thenReturn(null)

        // When
        val result = userManagementService.getUserById(999)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tidak ditemukan") == true)
    }

    @Test
    fun `getUsersByRole should return users filtered by role successfully`() = runTest {
        // Given
        val mockUsers = listOf(
            Pengguna(id = 1, username = "cashier1", passwordHash = "hash1", role = "CASHIER", permissions = "[]"),
            Pengguna(id = 2, username = "cashier2", passwordHash = "hash2", role = "CASHIER", permissions = "[]"),
            Pengguna(id = 3, username = "manager", passwordHash = "hash3", role = "MANAGER", permissions = "[]")
        )
        `when`(penggunaRepository.getAllPengguna()).thenReturn(flowOf(mockUsers))

        // When
        val result = userManagementService.getUsersByRole("CASHIER")

        // Then
        assertTrue(result is Result.Success)
        val users = (result as Result.Success).data
        assertEquals(2, users.size)
        users.forEach { assertEquals("CASHIER", it.role) }
    }

    @Test
    fun `getUsersByRole should return empty list for role with no users`() = runTest {
        // Given
        val mockUsers = listOf(
            Pengguna(id = 1, username = "cashier", passwordHash = "hash", role = "CASHIER", permissions = "[]")
        )
        `when`(penggunaRepository.getAllPengguna()).thenReturn(flowOf(mockUsers))

        // When
        val result = userManagementService.getUsersByRole("WAREHOUSE")

        // Then
        assertTrue(result is Result.Success)
        val users = (result as Result.Success).data
        assertTrue(users.isEmpty())
    }

    @Test
    fun `validatePasswordStrength should accept strong passwords`() {
        // Given
        val strongPasswords = listOf(
            "password123",
            "MySecurePass123",
            "Complex!Password#456",
            "VeryLongPasswordWithNumbers123"
        )

        // When & Then
        strongPasswords.forEach { password ->
            assertTrue(userManagementService.validatePasswordStrength(password))
        }
    }

    @Test
    fun `validatePasswordStrength should reject weak passwords`() {
        // Given
        val weakPasswords = listOf(
            "",
            "1",
            "12",
            "123",
            "1234",
            "12345",
            "123456",
            "1234567"
        )

        // When & Then
        weakPasswords.forEach { password ->
            assertFalse(userManagementService.validatePasswordStrength(password))
        }
    }

    @Test
    fun `validateRole should accept valid roles`() {
        // Given
        val validRoles = listOf("OWNER", "MANAGER", "CASHIER", "WAREHOUSE")

        // When & Then
        validRoles.forEach { role ->
            assertTrue(userManagementService.validateRole(role))
        }
    }

    @Test
    fun `validateRole should reject invalid roles`() {
        // Given
        val invalidRoles = listOf("", "ADMIN", "USER", "invalid", "null")

        // When & Then
        invalidRoles.forEach { role ->
            assertFalse(userManagementService.validateRole(role))
        }
    }
}