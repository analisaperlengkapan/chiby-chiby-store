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
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import java.security.MessageDigest

class AuthServiceTest {

    private lateinit var authService: AuthServiceImpl
    private lateinit var mockPenggunaDao: PenggunaDao
    private lateinit var mockUserSessionRepository: com.chibychibystore.repository.UserSessionRepository

    private val testUser = Pengguna(
        id = 1,
        username = "testuser",
        passwordHash = hashPassword("password123"),
        role = Role.CASHIER
    )

    @Before
    fun setup() {
        mockPenggunaDao = mock()
        mockUserSessionRepository = mock()
        authService = AuthServiceImpl(mockPenggunaDao, mockUserSessionRepository)
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

    @Test
    fun `initializeSession should restore active session when valid`() = runTest {
        // Given
        val session = com.chibychibystore.data.local.entity.UserSession(
            id = 1,
            userId = testUser.id,
            loginTime = java.util.Date(System.currentTimeMillis() - 1000),
            lastActivityTime = java.util.Date(),
            isActive = true
        )

        whenever(mockUserSessionRepository.getActiveSession()).thenReturn(Result.success(session))
        whenever(mockPenggunaDao.getPenggunaById(testUser.id)).thenReturn(testUser)

        // When
        val result = authService.initializeSession()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUser, authService.getCurrentUser())
        verify(mockUserSessionRepository).updateLastActivityTime(session.id)
    }

    @Test
    fun `extendSession should update last activity time when session exists`() = runTest {
        // Given - logged in user
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser")).thenReturn(testUser)
        whenever(mockUserSessionRepository.createSession(any())).thenReturn(Result.success(1L))
        authService.login("testuser", "password123")

        val session = com.chibychibystore.data.local.entity.UserSession(
            id = 2,
            userId = testUser.id,
            loginTime = java.util.Date(System.currentTimeMillis() - 1000),
            lastActivityTime = java.util.Date(),
            isActive = true
        )

        whenever(mockUserSessionRepository.getActiveSessionForUser(testUser.id)).thenReturn(Result.success(session))

        // When
        val result = authService.extendSession()

        // Then
        assertTrue(result.isSuccess)
        verify(mockUserSessionRepository).updateLastActivityTime(session.id)
    }

    @Test
    fun `forceLogoutAll should deactivate sessions and clear current user`() = runTest {
        // Given - user is logged in
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser")).thenReturn(testUser)
        whenever(mockUserSessionRepository.createSession(any())).thenReturn(Result.success(1L))
        authService.login("testuser", "password123")

        whenever(mockUserSessionRepository.deactivateAllSessions()).thenReturn(Result.success(Unit))

        // When
        val result = authService.forceLogoutAll()

        // Then
        assertTrue(result.isSuccess)
        assertNull(authService.getCurrentUser())
        verify(mockUserSessionRepository).deactivateAllSessions()
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}