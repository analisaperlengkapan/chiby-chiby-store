package com.chibychibystore.service

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PenggunaRepository
import com.chibychibystore.service.impl.UserManagementServiceImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * User administration: create/update/delete, password reset, activation, and
 * the authorization ordering. The legacy `UserManagementServiceTest` was removed
 * with the excluded suite; this restores its cases against the current service.
 */
class UserManagementServiceTest {

    @Mock lateinit var userRepository: PenggunaRepository
    @Mock lateinit var authService: AuthService

    private lateinit var service: UserManagementServiceImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = UserManagementServiceImpl(userRepository, authService)
    }

    private fun user(
        id: Long = 2,
        username: String = "kasir",
        role: Role = Role.CASHIER,
        isActive: Boolean = true
    ) = Pengguna(id = id, username = username, passwordHash = "hash", role = role, isActive = isActive)

    @Test
    fun `createUser inserts a user when the caller may manage users`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)
        whenever(userRepository.getUserByUsername("baru")).thenReturn(Result.failure(Exception("not found")))
        whenever(userRepository.createPengguna(any())).thenReturn(Result.success(7L))

        val result = service.createUser("baru", "secret123", Role.CASHIER, createdBy = 1L)

        assertEquals(7L, result.getOrNull())
        verify(userRepository).createPengguna(any())
    }

    @Test
    fun `createUser rejects a duplicate username`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)
        whenever(userRepository.getUserByUsername("kasir")).thenReturn(Result.success(user()))

        assertTrue(service.createUser("kasir", "secret123", Role.CASHIER, 1L).isFailure)
        verify(userRepository, never()).createPengguna(any())
    }

    @Test
    fun `createUser rejects a weak password`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)

        assertTrue(service.createUser("baru", "123", Role.CASHIER, 1L).isFailure)
        verify(userRepository, never()).createPengguna(any())
    }

    @Test
    fun `createUser is refused without permission`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(false)

        assertTrue(service.createUser("baru", "secret123", Role.CASHIER, 1L).isFailure)
        verify(userRepository, never()).createPengguna(any())
    }

    @Test
    fun `updateUser persists the changed fields`() = runTest {
        val existing = user()
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)
        whenever(userRepository.getUserById(existing.id)).thenReturn(Result.success(existing))
        // A changed username is looked up to rule out a collision; the only match
        // is the user being edited, so it is not a duplicate.
        whenever(userRepository.getUserByUsername("kasir2")).thenReturn(Result.failure(Exception("not found")))
        whenever(userRepository.updateUser(any())).thenReturn(Result.success(Unit))

        val result = service.updateUser(existing.id, username = "kasir2", role = Role.MANAGER, isActive = false, updatedBy = 1L)

        assertTrue(result.isSuccess)
        verify(userRepository).updateUser(any())
    }

    @Test
    fun `updateUser rejects a username already taken by someone else`() = runTest {
        val existing = user(id = 2, username = "kasir")
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)
        whenever(userRepository.getUserById(2L)).thenReturn(Result.success(existing))
        whenever(userRepository.getUserByUsername("owner")).thenReturn(Result.success(user(id = 1, username = "owner", role = Role.OWNER)))

        assertTrue(service.updateUser(2L, "owner", null, null, 1L).isFailure)
        verify(userRepository, never()).updateUser(any())
    }

    @Test
    fun `deleteUser checks authorization before revealing whether the user exists`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(false)

        val result = service.deleteUser(42L, deletedBy = 1L)

        assertTrue(result.isFailure)
        // The existence lookup must not run for an unauthorised caller, otherwise
        // the error message leaks whether an account id exists.
        verify(userRepository, never()).getUserById(any())
        verify(userRepository, never()).deleteUser(any())
    }

    @Test
    fun `deleteUser refuses to delete the signed-in user`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)
        whenever(userRepository.getUserById(2L)).thenReturn(Result.success(user()))
        whenever(authService.getCurrentUser()).thenReturn(user())

        assertTrue(service.deleteUser(2L, deletedBy = 2L).isFailure)
        verify(userRepository, never()).deleteUser(any())
    }

    @Test
    fun `deleteUser removes another user`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)
        whenever(userRepository.getUserById(2L)).thenReturn(Result.success(user()))
        whenever(authService.getCurrentUser()).thenReturn(user(id = 1, username = "owner", role = Role.OWNER))
        whenever(userRepository.deleteUser(2L)).thenReturn(Result.success(Unit))

        assertTrue(service.deleteUser(2L, deletedBy = 1L).isSuccess)
        verify(userRepository).deleteUser(2L)
    }

    @Test
    fun `resetUserPassword writes a new hash`() = runTest {
        val target = user()
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)
        whenever(userRepository.getUserById(2L)).thenReturn(Result.success(target))
        whenever(userRepository.updateUser(any())).thenReturn(Result.success(Unit))

        assertTrue(service.resetUserPassword(2L, "brandnew1", resetBy = 1L).isSuccess)
        verify(userRepository).updateUser(any())
    }

    @Test
    fun `resetUserPassword rejects a short password`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)

        assertTrue(service.resetUserPassword(2L, "abc", resetBy = 1L).isFailure)
        verify(userRepository, never()).updateUser(any())
    }

    @Test
    fun `deactivateUser refuses to deactivate the signed-in user`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)
        whenever(authService.getCurrentUser()).thenReturn(user(id = 2))

        assertTrue(service.deactivateUser(2L, deactivatedBy = 2L).isFailure)
        verify(userRepository, never()).updateUser(any())
    }

    @Test
    fun `getUserStats aggregates the repository counts`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(true)
        whenever(userRepository.getUserCount()).thenReturn(Result.success(10))
        whenever(userRepository.countActiveUsers()).thenReturn(Result.success(8))
        whenever(userRepository.countByRole(Role.OWNER)).thenReturn(Result.success(1))
        whenever(userRepository.countByRole(Role.MANAGER)).thenReturn(Result.success(2))
        whenever(userRepository.countByRole(Role.CASHIER)).thenReturn(Result.success(4))
        whenever(userRepository.countByRole(Role.WAREHOUSE)).thenReturn(Result.success(3))

        val stats = service.getUserStats().getOrNull()!!

        assertEquals(10, stats.totalUsers)
        assertEquals(8, stats.activeUsers)
        assertEquals(1, stats.owners)
        assertEquals(2, stats.managers)
        assertEquals(4, stats.cashiers)
        assertEquals(3, stats.warehouseStaff)
    }

    @Test
    fun `getUserById is refused without permission`() = runTest {
        whenever(authService.hasPermission(Permissions.MANAGE_USERS)).thenReturn(false)

        assertTrue(service.getUserById(2L).isFailure)
        verify(userRepository, never()).getUserById(any())
    }
}
