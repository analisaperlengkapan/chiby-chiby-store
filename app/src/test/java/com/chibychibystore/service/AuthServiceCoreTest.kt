package com.chibychibystore.service

import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PenggunaSessionRepository
import com.chibychibystore.service.impl.AuthServiceImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.security.MessageDigest

/**
 * Core authentication behaviour: credential checks, the session row created on
 * login, logout, password changes, and the role-based permission matrix.
 *
 * The legacy `AuthServiceTest` was deleted with the rest of the excluded suite;
 * this restores its cases against the current service (session restore and the
 * idle timeout live in [AuthSessionRestoreTest]).
 */
class AuthServiceCoreTest {

    private val penggunaDao = mock<PenggunaDao>()
    private val sessionRepository = mock<PenggunaSessionRepository>()
    private val service = AuthServiceImpl(penggunaDao, sessionRepository)

    private fun hash(password: String): String =
        MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun user(
        id: Long = 1,
        username: String = "owner",
        password: String = "secret123",
        role: Role = Role.OWNER,
        isActive: Boolean = true
    ) = Pengguna(id = id, username = username, passwordHash = hash(password), role = role, isActive = isActive)

    @Test
    fun `login with valid credentials succeeds and opens a session`() = runTest {
        val owner = user()
        whenever(penggunaDao.getPenggunaByUsername("owner")).thenReturn(owner)
        whenever(sessionRepository.createSession(any())).thenReturn(Result.success(9L))

        val result = service.login("owner", "secret123")

        assertTrue(result.isSuccess)
        assertSame(owner, service.getCurrentUser())
        val sessionCaptor = argumentCaptor<com.chibychibystore.data.local.entity.PenggunaSession>()
        verify(sessionRepository).createSession(sessionCaptor.capture())
        val session = sessionCaptor.firstValue
        assertEquals(owner.id, session.userId)
        assertTrue(session.isActive)
    }

    @Test
    fun `login with a wrong password fails and does not open a session`() = runTest {
        whenever(penggunaDao.getPenggunaByUsername("owner")).thenReturn(user())

        val result = service.login("owner", "wrong")

        assertTrue(result.isFailure)
        assertNull(service.getCurrentUser())
        verify(sessionRepository, never()).createSession(any())
    }

    @Test
    fun `login with an unknown username fails`() = runTest {
        whenever(penggunaDao.getPenggunaByUsername("ghost")).thenReturn(null)

        assertTrue(service.login("ghost", "secret123").isFailure)
        assertNull(service.getCurrentUser())
    }

    @Test
    fun `login with blank input fails`() = runTest {
        assertTrue(service.login("", "secret123").isFailure)
        assertTrue(service.login("owner", "").isFailure)
    }

    @Test
    fun `login refuses an inactive account`() = runTest {
        whenever(penggunaDao.getPenggunaByUsername("owner")).thenReturn(user(isActive = false))

        assertTrue(service.login("owner", "secret123").isFailure)
        assertNull(service.getCurrentUser())
        verify(sessionRepository, never()).createSession(any())
    }

    @Test
    fun `login does not authenticate when the session cannot be persisted`() = runTest {
        whenever(penggunaDao.getPenggunaByUsername("owner")).thenReturn(user())
        whenever(sessionRepository.createSession(any())).thenReturn(Result.failure(Exception("disk full")))

        assertTrue(service.login("owner", "secret123").isFailure)
        assertNull(service.getCurrentUser())
    }

    @Test
    fun `logout closes the user sessions and clears the current user`() = runTest {
        whenever(penggunaDao.getPenggunaByUsername("owner")).thenReturn(user())
        whenever(sessionRepository.createSession(any())).thenReturn(Result.success(1L))
        service.login("owner", "secret123")

        val result = service.logout()

        assertTrue(result.isSuccess)
        assertNull(service.getCurrentUser())
        verify(sessionRepository).deactivateUserSessions(1L)
    }

    @Test
    fun `owner has every permission`() = runTest {
        whenever(penggunaDao.getPenggunaByUsername("owner")).thenReturn(user())
        whenever(sessionRepository.createSession(any())).thenReturn(Result.success(1L))
        service.login("owner", "secret123")

        assertTrue(service.hasPermission("MANAGE_USERS"))
        assertTrue(service.hasPermission("SOMETHING_UNLISTED"))
    }

    @Test
    fun `cashier permissions are narrower than a manager's`() = runTest {
        val cashier = user(id = 2, username = "kasir", role = Role.CASHIER)
        whenever(penggunaDao.getPenggunaByUsername("kasir")).thenReturn(cashier)
        whenever(sessionRepository.createSession(any())).thenReturn(Result.success(2L))
        service.login("kasir", "secret123")

        assertTrue(service.hasPermission("CREATE_SALES"))
        assertFalse(service.hasPermission("MANAGE_USERS"))
    }

    @Test
    fun `hasPermission is false when nobody is logged in`() = runTest {
        assertFalse(service.hasPermission("VIEW_INVENTORY"))
    }

    @Test
    fun `changePassword replaces the hash when the old password matches`() = runTest {
        val owner = user()
        whenever(penggunaDao.getPenggunaByUsername("owner")).thenReturn(owner)
        whenever(sessionRepository.createSession(any())).thenReturn(Result.success(1L))
        service.login("owner", "secret123")

        val result = service.changePassword("secret123", "newpass456")

        assertTrue(result.isSuccess)
        val updatedCaptor = argumentCaptor<Pengguna>()
        verify(penggunaDao).updatePengguna(updatedCaptor.capture())
        val updated = updatedCaptor.firstValue
        assertEquals(hash("newpass456"), updated.passwordHash)
        assertEquals(hash("newpass456"), service.getCurrentUser()?.passwordHash)
    }

    @Test
    fun `changePassword rejects a wrong old password`() = runTest {
        whenever(penggunaDao.getPenggunaByUsername("owner")).thenReturn(user())
        whenever(sessionRepository.createSession(any())).thenReturn(Result.success(1L))
        service.login("owner", "secret123")

        assertTrue(service.changePassword("nope", "newpass456").isFailure)
        verify(penggunaDao, never()).updatePengguna(any())
    }

    @Test
    fun `changePassword rejects a short or blank new password`() = runTest {
        whenever(penggunaDao.getPenggunaByUsername("owner")).thenReturn(user())
        whenever(sessionRepository.createSession(any())).thenReturn(Result.success(1L))
        service.login("owner", "secret123")

        assertTrue(service.changePassword("secret123", "short").isFailure)
        assertTrue(service.changePassword("secret123", "").isFailure)
    }

    @Test
    fun `changePassword fails when nobody is logged in`() = runTest {
        assertTrue(service.changePassword("old", "newpass456").isFailure)
    }
}
