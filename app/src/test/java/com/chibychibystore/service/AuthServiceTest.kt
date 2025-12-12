package com.chibychibystore.service

import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.MessageDigest

class AuthServiceTest {

    private lateinit var authService: AuthServiceImpl
    private lateinit var mockPenggunaDao: PenggunaDao

    private val testUser = Pengguna(
        id = 1,
        username = "testuser",
        passwordHash = hashPassword("password123"),
        role = Role.CASHIER
    )

    @Before
    fun setup() {
        mockPenggunaDao = mock()
        authService = AuthServiceImpl(mockPenggunaDao)
    }

    @Test
    fun `login with valid credentials should succeed`() = runTest {
        // Given
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser"))
            .thenReturn(testUser)

        // When
        val result = authService.login("testuser", "password123")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
        assertEquals(testUser, authService.getCurrentUser())
    }

    @Test
    fun `login with invalid username should fail`() = runTest {
        // Given
        whenever(mockPenggunaDao.getPenggunaByUsername("invaliduser"))
            .thenReturn(null)

        // When
        val result = authService.login("invaliduser", "password123")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.AuthenticationError)
        assertNull(authService.getCurrentUser())
    }

    @Test
    fun `login with invalid password should fail`() = runTest {
        // Given
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser"))
            .thenReturn(testUser)

        // When
        val result = authService.login("testuser", "wrongpassword")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.AuthenticationError)
        assertNull(authService.getCurrentUser())
    }

    @Test
    fun `login with empty username should fail`() = runTest {
        // When
        val result = authService.login("", "password123")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.ValidationError)
    }

    @Test
    fun `login with empty password should fail`() = runTest {
        // When
        val result = authService.login("testuser", "")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.ValidationError)
    }

    @Test
    fun `logout should clear current user`() = runTest {
        // Given - user is logged in
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser"))
            .thenReturn(testUser)
        authService.login("testuser", "password123")

        // When
        val result = authService.logout()

        // Then
        assertTrue(result.isSuccess)
        assertNull(authService.getCurrentUser())
    }

    @Test
    fun `hasPermission for owner should return true for all permissions`() = runTest {
        // Given - owner is logged in
        val ownerUser = testUser.copy(role = Role.OWNER)
        whenever(mockPenggunaDao.getPenggunaByUsername("owner"))
            .thenReturn(ownerUser)
        authService.login("owner", "password123")

        // When & Then
        assertTrue(authService.hasPermission("ANY_PERMISSION"))
        assertTrue(authService.hasPermission("DELETE_DATA"))
        assertTrue(authService.hasPermission("MANAGE_USERS"))
    }

    @Test
    fun `hasPermission for cashier should return true for cashier permissions`() = runTest {
        // Given - cashier is logged in
        whenever(mockPenggunaDao.getPenggunaByUsername("cashier"))
            .thenReturn(testUser)
        authService.login("cashier", "password123")

        // When & Then
        assertTrue(authService.hasPermission("CREATE_SALES"))
        assertTrue(authService.hasPermission("VIEW_INVENTORY"))
        assertTrue(authService.hasPermission("VIEW_DAILY_SALES_REPORT"))
        assertFalse(authService.hasPermission("MANAGE_USERS"))
        assertFalse(authService.hasPermission("DELETE_DATA"))
    }

    @Test
    fun `hasPermission for manager should return true for manager permissions`() = runTest {
        // Given - manager is logged in
        val managerUser = testUser.copy(role = Role.MANAGER)
        whenever(mockPenggunaDao.getPenggunaByUsername("manager"))
            .thenReturn(managerUser)
        authService.login("manager", "password123")

        // When & Then
        assertTrue(authService.hasPermission("VIEW_SALES_REPORTS"))
        assertTrue(authService.hasPermission("VIEW_FINANCIAL_REPORTS"))
        assertTrue(authService.hasPermission("APPROVE_LARGE_TRANSACTIONS"))
        assertTrue(authService.hasPermission("VIEW_INVENTORY"))
        assertTrue(authService.hasPermission("EDIT_INVENTORY"))
        assertFalse(authService.hasPermission("MANAGE_USERS"))
    }

    @Test
    fun `hasPermission for warehouse staff should return true for warehouse permissions`() = runTest {
        // Given - warehouse staff is logged in
        val warehouseUser = testUser.copy(role = Role.WAREHOUSE)
        whenever(mockPenggunaDao.getPenggunaByUsername("warehouse"))
            .thenReturn(warehouseUser)
        authService.login("warehouse", "password123")

        // When & Then
        assertTrue(authService.hasPermission("VIEW_INVENTORY"))
        assertTrue(authService.hasPermission("EDIT_INVENTORY"))
        assertTrue(authService.hasPermission("MANAGE_WAREHOUSES"))
        assertTrue(authService.hasPermission("PRINT_BARCODE_LABELS"))
        assertTrue(authService.hasPermission("VIEW_INVENTORY_REPORTS"))
        assertFalse(authService.hasPermission("CREATE_SALES"))
    }

    @Test
    fun `hasPermission when not logged in should return false`() = runTest {
        // Given - no user logged in

        // When & Then
        assertFalse(authService.hasPermission("ANY_PERMISSION"))
    }

    @Test
    fun `changePassword with correct old password should succeed`() = runTest {
        // Given - user is logged in
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser"))
            .thenReturn(testUser)
        authService.login("testuser", "password123")

        // When
        val result = authService.changePassword("password123", "newpassword123")

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `changePassword with incorrect old password should fail`() = runTest {
        // Given - user is logged in
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser"))
            .thenReturn(testUser)
        authService.login("testuser", "password123")

        // When
        val result = authService.changePassword("wrongpassword", "newpassword123")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.AuthenticationError)
    }

    @Test
    fun `changePassword with empty new password should fail`() = runTest {
        // Given - user is logged in
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser"))
            .thenReturn(testUser)
        authService.login("testuser", "password123")

        // When
        val result = authService.changePassword("password123", "")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.ValidationError)
    }

    @Test
    fun `changePassword when not logged in should fail`() = runTest {
        // Given - no user logged in

        // When
        val result = authService.changePassword("oldpass", "newpass")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.AuthenticationError)
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}